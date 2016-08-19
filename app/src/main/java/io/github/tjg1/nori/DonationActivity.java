/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

// Contains parts of android.donations.lib by SufficientlySecure.

package io.github.tjg1.nori;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;

import io.github.tjg1.nori.util.iab.IabHelper;
import io.github.tjg1.nori.util.iab.IabResult;
import io.github.tjg1.nori.util.iab.Inventory;
import io.github.tjg1.nori.util.iab.Purchase;

/**
 * Activity used to support the continued development of Nori using PayPal, Patreon or Google IAP.
 */
public class DonationActivity extends AppCompatActivity {

  /** List of Play Store IAP items. */
  private static final String[] GOOGLE_IAP_ITEMS =
      new String[]{"donate_xs", "donate_s", "donate_m", "donate_l", "donate_xl"};
  /** Currency used for donations. */
  private String mDonationCurrency;
  /** PayPal donations enabled. */
  private boolean mPayPalEnabled = false;
  /** PayPal donations email. */
  private String mPayPalDonationEmail;
  /** PayPal item name. */
  private String mPayPalItemName;
  /** Patreon donations enabled. */
  private boolean mPatreonEnabled = false;
  /** Patreon account name. */
  private String mPatreonAccountName;
  /** Bitcoin donations enabled. */
  private boolean mBitcoinEnabled = false;
  /** Bitcoin donations address. */
  private String mBitcoinAddress;
  /** Google IAP donations enabled. */
  private boolean mGoogleIAPEnabled = false;
  /** Google IAP public key. */
  private String mGoogleIAPPublicKey;
  /** Google IAP donation helper. */
  private IabHelper iabHelper;
  /** Google IAP handler acting as the event listener and ListView adapter. */
  private GoogleIAPHandler iapHandler;
  /** Handle donation button method clicks. */
  private View.OnClickListener donationButtonListener = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      // Handle donation method buttons.
      switch (view.getId()) {
        case R.id.button_donate_paypal:
          donatePayPal();
          break;
        case R.id.button_donate_patreon:
          donatePatreon();
          break;
        case R.id.button_donate_bitcoin:
          donateBitcoin();
          break;
        default:
          break;
      }
    }
  };
  /** Show the Bitcoin donation {@link AlertDialog}, even if a Bitcoin wallet app is installed. */
  private View.OnLongClickListener bitcoinButtonLongClickListener = new View.OnLongClickListener() {
    @Override
    public boolean onLongClick(View view) {
      // Inflate the Bitcoin donation dialog View.
      View dialogView = getLayoutInflater().inflate(R.layout.dialog_bitcoin_donation, null);
      TextView bitcoinAddress = (TextView) dialogView.findViewById(R.id.bitcoin_address);
      if (bitcoinAddress != null) {
        bitcoinAddress.setText(mBitcoinAddress);
      }

      // Show the dialog.
      new AlertDialog.Builder(DonationActivity.this)
          .setTitle(R.string.donation_method_bitcoin)
          .setView(dialogView)
          .setCancelable(true)
          .setNegativeButton(R.string.dialog_tags_closeButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
              dialog.cancel();
            }
          }).create().show();

      return true;
    }
  };

  /** Get the Google IAP donation helper. If the helper doesn't exist, create it. */
  private IabHelper getIabHelper() {
    if (iabHelper == null) {
      iabHelper = new IabHelper(this, BuildConfig.DONATIONS_GOOGLE_PUB_KEY);
      iabHelper.enableDebugLogging(BuildConfig.DEBUG);
    }
    return iabHelper;
  }

  /** Get the Google IAP listener/adapter. */
  private GoogleIAPHandler getIapHandler() {
    if (iapHandler == null) {
      iapHandler = new GoogleIAPHandler(this, android.R.layout.simple_list_item_1);
    }
    return iapHandler;
  }

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setDonationConfiguration();
    setContentView(R.layout.activity_donation);
    setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Pass on the Activity result to the IAB helper.
    if (iabHelper == null || iabHelper.handleActivityResult(requestCode, resultCode, data)) {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void setContentView(@LayoutRes int layoutResID) {
    super.setContentView(layoutResID);

    // Show available donation methods.
    if (layoutResID == R.layout.activity_donation) {
      if (mPayPalEnabled && !TextUtils.isEmpty(mPayPalDonationEmail)) {
        Button paypalDonateButton = (Button) findViewById(R.id.button_donate_paypal);
        paypalDonateButton.setVisibility(View.VISIBLE);
        paypalDonateButton.setOnClickListener(donationButtonListener);
      }
      if (mPatreonEnabled && !TextUtils.isEmpty(mPatreonAccountName)) {
        Button patreonDonateButton = (Button) findViewById(R.id.button_donate_patreon);
        patreonDonateButton.setVisibility(View.VISIBLE);
        patreonDonateButton.setOnClickListener(donationButtonListener);
      }
      if (mBitcoinEnabled && !TextUtils.isEmpty(mBitcoinAddress)) {
        Button bitcoinDonateButton = (Button) findViewById(R.id.button_donate_bitcoin);
        bitcoinDonateButton.setVisibility(View.VISIBLE);
        bitcoinDonateButton.setOnClickListener(donationButtonListener);
        bitcoinDonateButton.setOnLongClickListener(bitcoinButtonLongClickListener);
      }
      if (mGoogleIAPEnabled && !TextUtils.isEmpty(mGoogleIAPPublicKey)) {
        ListView listView = (ListView) findViewById(R.id.list_donation_amount);
        listView.setAdapter(getIapHandler());
        listView.setOnItemClickListener(getIapHandler());
        getIabHelper().startSetup(getIapHandler());
      }
    }
  }

  @Override
  public void setSupportActionBar(@Nullable Toolbar toolbar) {
    super.setSupportActionBar(toolbar);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayShowHomeEnabled(false);
      actionBar.setDisplayShowTitleEnabled(true);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home: // Handle back button in the action bar.
        onBackPressed();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /** Set donation configuration based on locale and build configuration. */
  private void setDonationConfiguration() {
    Resources resources = getResources();

    // Currency settings based on locale.
    mDonationCurrency = resources.getString(R.string.donation_currency);

    // PayPal settings from build configuration.
    mPayPalEnabled = BuildConfig.DONATIONS_PAYPAL;
    mPayPalDonationEmail = BuildConfig.DONATIONS_PAYPAL_EMAIL;
    mPayPalItemName = resources.getString(R.string.donation_paypal_item);
    mPatreonEnabled = BuildConfig.DONATIONS_PATREON;
    mPatreonAccountName = BuildConfig.DONATIONS_PATREON_ACCOUNT;
    mBitcoinEnabled = BuildConfig.DONATIONS_BITCOIN;
    mBitcoinAddress = BuildConfig.DONATIONS_BITCOIN_ADDRESS;
    mGoogleIAPEnabled = BuildConfig.DONATIONS_GOOGLE;
    mGoogleIAPPublicKey = BuildConfig.DONATIONS_GOOGLE_PUB_KEY;
  }

  /** Launch the system web browser pointed to the PayPal donation page. */
  private void donatePayPal() {
    Uri.Builder uriBuilder = new Uri.Builder();
    uriBuilder.scheme("https").authority("www.paypal.com").path("cgi-bin/webscr");
    uriBuilder.appendQueryParameter("cmd", "_donations");

    uriBuilder.appendQueryParameter("business", mPayPalDonationEmail);
    uriBuilder.appendQueryParameter("lc", "US");
    uriBuilder.appendQueryParameter("item_name", mPayPalItemName);
    uriBuilder.appendQueryParameter("no_note", "1");
    uriBuilder.appendQueryParameter("no_shipping", "1");
    uriBuilder.appendQueryParameter("currency_code", mDonationCurrency);
    Uri paypalUrl = uriBuilder.build();

    try {
      Intent intent = new Intent(Intent.ACTION_VIEW, paypalUrl);
      startActivity(intent);
    } catch (ActivityNotFoundException e) {
      copyToClipboard(paypalUrl.toString());
      Snackbar.make(findViewById(R.id.root), R.string.donation_url_copiedToClipboard,
          Snackbar.LENGTH_LONG).show();
    }
  }

  /** Launch the system web browser pointed to the Patreon donation page. */
  private void donatePatreon() {
    Uri.Builder uriBuilder = new Uri.Builder();
    uriBuilder.scheme("https").authority("www.patreon.com").path("bePatron");
    uriBuilder.appendQueryParameter("patAmt", "1");
    uriBuilder.appendQueryParameter("u", mPatreonAccountName);
    Uri patreonUrl = uriBuilder.build();

    try {
      Intent intent = new Intent(Intent.ACTION_VIEW, patreonUrl);
      startActivity(intent);
    } catch (ActivityNotFoundException e) {
      copyToClipboard(patreonUrl.toString());
      Snackbar.make(findViewById(R.id.root), R.string.donation_url_copiedToClipboard,
          Snackbar.LENGTH_LONG).show();
    }
  }

  /**
   * First copies the Bitcoin donation address to the clipboard, then either:
   * opens a Bitcoin wallet app installed on the user's device, or
   * displays a {@link android.app.Dialog} with the Bitcoin address and QR code.
   */
  private void donateBitcoin() {
    // Copy Bitcoin address to clipboard.
    copyToClipboard(mBitcoinAddress);
    Snackbar.make(findViewById(R.id.root), R.string.donation_method_bitcoin_copiedToClipboard,
        Snackbar.LENGTH_LONG).show();

    try {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(Uri.parse("bitcoin:" + mBitcoinAddress));
      startActivity(intent);
    } catch (ActivityNotFoundException e) {
      Button button = (Button) findViewById(R.id.button_donate_bitcoin);
      button.performLongClick();
    }
  }

  /** Copy a string to clipboard. */
  private void copyToClipboard(String string) {
    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    ClipData clip = ClipData.newPlainText(string, string);
    clipboard.setPrimaryClip(clip);
  }

  // This could be implemented as a fragment displaying the ListView and handling IAB functions.

  /** Display an error message {@link Snackbar} when connection to the Play Store service fails */
  private void onError() {
    Snackbar.make(findViewById(R.id.root), R.string.donation_error_connectingToPlayStoreService,
        Snackbar.LENGTH_LONG).show();
  }

  /** Listener handling interactions with the Google Play IAB helper. */
  private class GoogleIAPHandler extends ArrayAdapter<Pair<String, String>>
      implements IabHelper.OnIabSetupFinishedListener, IabHelper.QueryInventoryFinishedListener,
      IabHelper.OnIabPurchaseFinishedListener, IabHelper.OnConsumeFinishedListener,
      AdapterView.OnItemClickListener {

    /**
     * Create a new object acting as a listener for {@link IabHelper} events and as an Adapter
     * for the donation amount {@link android.widget.ListView}.
     *
     * @param context  Activity context.
     * @param resource Layout resource used to display donation amounts.
     */
    public GoogleIAPHandler(Context context, int resource) {
      super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // Create the list view item.
      View view = convertView;

      if (view == null) {
        view = LayoutInflater.from(DonationActivity.this)
            .inflate(android.R.layout.simple_list_item_1, parent, false);
      }

      // Get SKU details.
      Pair<String, String> skuPair = getItem(position);

      // Populate the views.
      TextView textView = (TextView) view.findViewById(android.R.id.text1);
      textView.setText(skuPair.second);

      return view;
    }

    /**
     * Called to notify that setup is complete.
     *
     * @param result The result of the setup process.
     */
    @Override
    public void onIabSetupFinished(IabResult result) {
      if (result.isSuccess()) {
        if (iabHelper == null) return;

        try {
          // Query the prices of IAP items and display them in the ListView.
          getIabHelper().queryInventoryAsync(true, Arrays.asList(GOOGLE_IAP_ITEMS), null, this);
          return;
        } catch (IabHelper.IabAsyncInProgressException ignored) {
        }
      }
      onError();
    }

    /**
     * Called to notify that an inventory query operation completed.
     *
     * @param result The result of the operation.
     * @param inv    The inventory.
     */
    @Override
    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
      if (result.isSuccess()) {
        if (iabHelper == null) return;
        // Populate adapter with SKU details.
        for (String sku : GOOGLE_IAP_ITEMS) {
          if (inv.hasDetails(sku)) {
            add(new Pair<>(sku, inv.getSkuDetails(sku).getPrice()));
          }
        }
        notifyDataSetChanged();
        return;
      }
      onError();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
      final String sku = getItem(position).first;

      try {
        // Launch Google Play purchase flow.
        getIabHelper().launchPurchaseFlow(DonationActivity.this, sku, 0, this);
      } catch (IabHelper.IabAsyncInProgressException e) {
        onError();
      }
    }

    /**
     * Called to notify that an in-app purchase finished. If the purchase was successful,
     * then the sku parameter specifies which item was purchased. If the purchase failed,
     * the sku and extraData parameters may or may not be null, depending on how far the purchase
     * process went.
     *
     * @param result The result of the purchase.
     * @param info   The purchase information (null if purchase failed)
     */
    @Override
    public void onIabPurchaseFinished(IabResult result, Purchase info) {
      if (iabHelper == null) return;

      if (result.isSuccess()) {
        // Consume the purchase, so that the user can donate multiple times.
        try {
          getIabHelper().consumeAsync(info, this);
        } catch (IabHelper.IabAsyncInProgressException e) {
          e.printStackTrace();
        }

        // Display thank you toast.
        Snackbar.make(findViewById(R.id.root),
            R.string.donation_toast_completed, Snackbar.LENGTH_LONG).show();
      }
    }

    /**
     * Called to notify that a consumption has finished.
     *
     * @param purchase The purchase that was (or was to be) consumed.
     * @param result   The result of the consumption operation.
     */
    @Override
    public void onConsumeFinished(Purchase purchase, IabResult result) {
      // Do nothing.
    }
  }
}
