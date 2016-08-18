/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewActivity extends AppCompatActivity {
  /** Intent extra used to pass the URL to display in the {@link WebView}. */
  private static final String INTENT_EXTRA_URL = "WebViewActivity.URL";
  /** Intent extra used to set the title of this {@link android.app.Activity}. */
  private static final String INTENT_EXTRA_TITLE = "WebViewActivity.TITLE";

  /** WebView client used to intercept webview events. */
  private final WebViewClient mWebViewClient = new WebViewClient() {
    @Override
    @SuppressWarnings("deprecation")
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
      Snackbar.make(findViewById(R.id.root), R.string.toast_error_noNetwork,
          Snackbar.LENGTH_SHORT).show();
      WebViewActivity.this.finish();
    }

    @TargetApi(android.os.Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
      onReceivedError(view, error.getErrorCode(), error.getDescription().toString(), request.getUrl().toString());
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      try {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); // Open links in relevant app.
      } catch (ActivityNotFoundException ignored) {
        Snackbar.make(findViewById(R.id.root), R.string.toast_error_noApplicationFound,
            Snackbar.LENGTH_LONG).show();
      }
      return true;
    }

    @TargetApi(android.os.Build.VERSION_CODES.M)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
      return shouldOverrideUrlLoading(view, request.getUrl().toString());
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_web_view);
    setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    Intent intent = getIntent();

    // Set up the web view.
    WebView webView = setUpWebView((WebView) findViewById(R.id.webView));

    // Set activity title from intent.
    if (intent.hasExtra(INTENT_EXTRA_TITLE)) {
      setTitle(intent.getStringExtra(INTENT_EXTRA_TITLE));
    }

    // Get URL from the intent used to start the activity.
    if (intent.hasExtra(INTENT_EXTRA_URL)) {
      webView.loadUrl(intent.getStringExtra(INTENT_EXTRA_URL));
    } else if ("io.github.tjg1.nori.ABOUT".equals(intent.getAction())) {
      webView.loadUrl("https://tjg1.github.io/nori/about.html?version=" + Uri.encode(BuildConfig.VERSION_NAME));
    } else {
      this.finish();
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
      case android.R.id.home:
        onBackPressed();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Sets the default preferences of the {@link WebView} used to display content of this activity.
   *
   * @param webView {@link WebView} to configure.
   * @return Configured WebView instance.
   */
  @SuppressLint("SetJavaScriptEnabled")
  private WebView setUpWebView(WebView webView) {
    webView.setWebViewClient(mWebViewClient);
    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);

    return webView;
  }
}
