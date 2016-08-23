/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori;


import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Arrays;

import io.github.tjg1.nori.util.iab.IabHelper;
import io.github.tjg1.nori.util.iab.IabResult;
import io.github.tjg1.nori.util.iab.Inventory;
import io.github.tjg1.nori.util.iab.Purchase;

/** Listener handling interactions with the Google Play IAB helper. */
public class GoogleIAPHandler extends ArrayAdapter<Pair<String, String>>
        implements IabHelper.OnIabSetupFinishedListener, IabHelper.QueryInventoryFinishedListener,
        IabHelper.OnIabPurchaseFinishedListener, IabHelper.OnConsumeFinishedListener,
        AdapterView.OnItemClickListener {

    private final DonationActivity activity;

    /**
     * Create a new object acting as a listener for {@link IabHelper} events and as an Adapter
     * for the donation amount {@link android.widget.ListView}.
     *
     * @param context  Activity context.
     * @param resource Layout resource used to display donation amounts.
     */
    public GoogleIAPHandler(Context context, int resource,DonationActivity activity) {
        super(context, resource);
        this.activity = activity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Create the list view item.
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(activity)
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
            if (activity.getIabHelper() == null) return;

            try {
                // Query the prices of IAP items and display them in the ListView.
                activity.getIabHelper().queryInventoryAsync(true, Arrays.asList(activity.GOOGLE_IAP_ITEMS), null, this);
                return;
            } catch (IabHelper.IabAsyncInProgressException ignored) {
            }
        }
        activity.onError();
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
            if (activity.getIabHelper() == null) return;
            // Populate adapter with SKU details.
            for (String sku : activity.GOOGLE_IAP_ITEMS) {
                if (inv.hasDetails(sku)) {
                    add(new Pair<>(sku, inv.getSkuDetails(sku).getPrice()));
                }
            }
            notifyDataSetChanged();
            return;
        }
        activity.onError();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        final String sku = getItem(position).first;

        try {
            // Launch Google Play purchase flow.
            activity.getIabHelper().launchPurchaseFlow(activity, sku, 0, this);
        } catch (IabHelper.IabAsyncInProgressException e) {
            activity.onError();
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
        if (activity.getIabHelper() == null) return;

        if (result.isSuccess()) {
            // Consume the purchase, so that the user can donate multiple times.
            try {
                activity.getIabHelper().consumeAsync(info, this);
            } catch (IabHelper.IabAsyncInProgressException e) {
                e.printStackTrace();
            }

            // Display thank you toast.
            Snackbar.make(activity.findViewById(R.id.root),
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
