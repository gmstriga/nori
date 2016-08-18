/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.library.norilib.Tag;
import io.github.tjg1.library.norilib.clients.SearchClient;
import io.github.tjg1.nori.database.APISettingsDatabase;
import io.github.tjg1.nori.database.SearchSuggestionDatabase;
import io.github.tjg1.nori.fragment.SearchResultGridFragment;

/** Searches for images and displays the results in a scrollable grid of thumbnails. */
public class SearchActivity extends AppCompatActivity implements SearchResultGridFragment.OnSearchResultGridFragmentInteractionListener {
  /** Identifier used to send the active {@link io.github.tjg1.library.norilib.SearchResult} to {@link io.github.tjg1.nori.ImageViewerActivity}. */
  public static final String BUNDLE_ID_SEARCH_RESULT = "io.github.tjg1.nori.SearchResult";
  /** Identifier used to send the position of the selected {@link io.github.tjg1.library.norilib.Image} to {@link io.github.tjg1.nori.ImageViewerActivity}. */
  public static final String BUNDLE_ID_IMAGE_INDEX = "io.github.tjg1.nori.ImageIndex";
  /** Identifier used to send {@link io.github.tjg1.library.norilib.clients.SearchClient} settings to {@link io.github.tjg1.nori.ImageViewerActivity}. */
  public static final String BUNDLE_ID_SEARCH_CLIENT_SETTINGS = "io.github.tjg1.nori.SearchClient.Settings";
  /** Identifier used for the query string to search when starting this activity with an {@link android.content.Intent} */
  public static final String INTENT_EXTRA_SEARCH_QUERY = "io.github.tjg1.nori.SearchQuery";
  /** Identifier used to include {@link SearchClient.Settings} objects in search intents. */
  public static final String INTENT_EXTRA_SEARCH_CLIENT_SETTINGS = "io.github.tjg1.nori.SearchClient.Settings";
  /** Identifier used to preserve current search query in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_SEARCH_QUERY = "io.github.tjg1.nori.SearchQuery";
  /** Identifier used to preserve iconified/expanded state of the SearchView in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_SEARCH_VIEW_IS_EXPANDED = "io.github.tjg1.nori.SearchView.isExpanded";
  /** Identifier used to preserve search view focused state. */
  private static final String BUNDLE_ID_SEARCH_VIEW_IS_FOCUSED = "io.github.tjg1.nori.SearchView.isFocused";
  /** Default {@link android.content.SharedPreferences} object. */
  private SharedPreferences sharedPreferences;
  /* {@link SearchClient.Settings} object selected from the service dropdown menu. */
  private SearchClient.Settings searchClientSettings;
  /** Search API client. */
  private SearchClient searchClient;
  /** Search API activity indicator. */
  private ProgressBar searchProgressBar;
  /** Search API service dropdown. */
  private Spinner serviceSpinner;
  /** Search view menu item. */
  private MenuItem searchMenuItem;
  /** Action bar search view. */
  private SearchView searchView;
  /** Search callback currently awaiting a response from the Search API. */
  private SearchResultCallback searchCallback;
  /** Search result grid fragment shown in this activity. */
  private SearchResultGridFragment searchResultGridFragment;
  /** Bundle used when restoring saved instance state (after screen rotation, app restored from background, etc.) */
  private Bundle savedInstanceState;

  /**
   * Set up the action bar SearchView and its event handlers.
   *
   * @param menu Menu after being inflated in {@link #onCreateOptionsMenu(android.view.Menu)}.
   */
  private void setUpSearchView(Menu menu) {
    // Extract SearchView from the MenuItem object.
    searchMenuItem = menu.findItem(R.id.action_search);
    searchMenuItem.setVisible(searchClientSettings != null);
    searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);

    // Set Searchable XML configuration.
    SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

    // Set SearchView attributes.
    searchView.setFocusable(false);
    searchView.setQueryRefinementEnabled(true);

    // Set submit action and allow empty queries.
    SearchView.SearchAutoComplete searchTextView = (SearchView.SearchAutoComplete) searchView.findViewById(R.id.search_src_text);
    searchTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        CharSequence query = textView.getText();
        if (query != null && searchClientSettings != null) {
          // Prepare a intent to send to a new instance of this activity.
          Intent intent = new Intent(SearchActivity.this, SearchActivity.class);
          intent.setAction(Intent.ACTION_SEARCH);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          intent.putExtra(BUNDLE_ID_SEARCH_CLIENT_SETTINGS, searchClientSettings);
          intent.putExtra(BUNDLE_ID_SEARCH_QUERY, query.toString());

          // Collapse the ActionView. This makes navigating through previous results using the back key less painful.
          MenuItemCompat.collapseActionView(searchMenuItem);

          // Start a new activity with the created Intent.
          startActivity(intent);
        }

        // Returns true to override the default behaviour and stop another search Intent from being sent.
        return true;
      }
    });

    searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
      @Override
      public boolean onSuggestionSelect(int position) {
        return onSuggestionClick(position);
      }

      @Override
      public boolean onSuggestionClick(int position) {
        if (searchClientSettings != null) {
          // Get the SearchView's suggestion adapter.
          CursorAdapter adapter = searchView.getSuggestionsAdapter();
          // Get the suggestion at given position.
          Cursor c = adapter.getCursor();
          c.moveToPosition(position);

          // Create and send a search intent.
          Intent intent = new Intent(SearchActivity.this, SearchActivity.class);
          intent.setAction(Intent.ACTION_SEARCH);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          intent.putExtra(BUNDLE_ID_SEARCH_CLIENT_SETTINGS, searchClientSettings);
          intent.putExtra(BUNDLE_ID_SEARCH_QUERY, c.getString(c.getColumnIndex(SearchSuggestionDatabase.COLUMN_NAME)));
          startActivity(intent);

          // Release native resources.
          c.close();
        }

        // Return true to override default behaviour and prevent another intent from being sent.
        return true;
      }
    });

    if (savedInstanceState != null) {
      // Restore state from saved instance state bundle (after screen rotation, app restored from background, etc.)
      if (savedInstanceState.containsKey(BUNDLE_ID_SEARCH_QUERY)) {
        // Restore search query from saved instance state.
        searchView.setQuery(savedInstanceState.getCharSequence(BUNDLE_ID_SEARCH_QUERY), false);
      }
      // Restore iconified/expanded search view state from saved instance state.
      if (savedInstanceState.getBoolean(BUNDLE_ID_SEARCH_VIEW_IS_EXPANDED, false)) {
        MenuItemCompat.expandActionView(searchMenuItem);
        // Restore focus state.
        if (!savedInstanceState.getBoolean(BUNDLE_ID_SEARCH_VIEW_IS_FOCUSED, false)) {
          searchView.clearFocus();
        }
      }
    } else if (getIntent() != null && getIntent().getAction().equals(Intent.ACTION_SEARCH)) {
      // Set SearchView query string to match the one sent in the SearchIntent.
      searchView.setQuery(getIntent().getStringExtra(BUNDLE_ID_SEARCH_QUERY), false);
    }
  }

  /** Set up the {@link android.support.v7.app.ActionBar}, including the API service picker dropdown. */
  private void setUpActionBar() {
    Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolBar);
    searchProgressBar = (ProgressBar) toolBar.findViewById(R.id.progressBar);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayShowHomeEnabled(false);
      actionBar.setDisplayShowTitleEnabled(false);
    }

    // Set up service list spinner.
    serviceSpinner = (Spinner) toolBar.findViewById(R.id.spinner_service);
    ServiceDropdownAdapter serviceDropdownAdapter = new ServiceDropdownAdapter();
    serviceSpinner.setAdapter(serviceDropdownAdapter);
    serviceSpinner.setOnItemSelectedListener(serviceDropdownAdapter);
  }

  /**
   * Request a {@link SearchResult} object to be fetched from the background.
   *
   * @param query Query string (a space-separated list of tags).
   */
  private void doSearch(String query) {
    // Show progress bar in ActionBar.
    if (searchProgressBar != null) {
      searchProgressBar.setVisibility(View.VISIBLE);
    }
    // Request a search result from the API client.
    searchCallback = new SearchResultCallback();
    searchClient.search(query, searchCallback);
  }

  /** Show the donation dialog at defined app launch count thresholds. */
  private void showDonationDialog() {
    // Get the number of times the dialog was already shown and current time.
    int donationDialogShownTimes = sharedPreferences
        .getInt(getString(R.string.preference_donation_dialog_count), 0);
    long currentTime = System.currentTimeMillis();

    // Get Nori installation date.
    long installationDate;
    try {
      installationDate = getPackageManager().getPackageInfo(getPackageName(), 0)
          .firstInstallTime;
    } catch (PackageManager.NameNotFoundException e) {
      return;
    }

    // Show the donation dialog 7 days after installation, 30 days thereafter and
    // then every 3 months thereafter.
    if (donationDialogShownTimes == 0 && (currentTime - installationDate) > 604800000L ||
        donationDialogShownTimes == 1 && (currentTime - installationDate) > 3196800000L ||
        (currentTime - installationDate) > (donationDialogShownTimes * 7776000000L + 3196800000L)) {
      new AlertDialog.Builder(this)
          .setTitle(R.string.activity_donations)
          .setMessage(R.string.donation_summary)
          .setCancelable(false)
          .setPositiveButton(R.string.donation_dialog_donate,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                  startActivity(new Intent(SearchActivity.this, DonationActivity.class));
                  dialogInterface.cancel();
                }
              })
          .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
              dialogInterface.cancel();
            }
          }).create().show();

      // Increment donation dialog count shared preference.
      sharedPreferences.edit().putInt(getString(R.string.preference_donation_dialog_count),
          ++donationDialogShownTimes).apply();
    }
  }

  /**
   * Called when a new Search API is selected by the user from the action bar dropdown.
   *
   * @param settings Selected {@link SearchClient.Settings} object.
   * @param expandActionView Should the SearchView action view be expanded?
   */
  protected void onSearchAPISelected(SearchClient.Settings settings, boolean expandActionView) {
    if (settings == null) {
      // The SearchClient setting database is empty.
      return;
    }

    // Show search action bar icon (if not already visible).
    if (searchMenuItem != null) {
      searchMenuItem.setVisible(true);
    }
    // Expand the SearchView when an API is selected manually by the user.
    // (and not automatically restored from previous state when the app is first launched)
    if (searchClientSettings != null && searchMenuItem != null && expandActionView) {
      MenuItemCompat.expandActionView(searchMenuItem);
    }

    searchClientSettings = settings;

    // If a SearchClient wasn't included in the Intent that started this activity, create one now and search for the default query.
    // Only do this if SearchSearch filter is enabled.
    if (searchClient == null && searchResultGridFragment.getSearchResult() == null) {
      searchClient = settings.createSearchClient(this);
      if (shouldLoadDefaultQuery()) {
        doSearch(searchClient.getDefaultQuery());
      } else if (searchMenuItem != null) {
        MenuItemCompat.expandActionView(searchMenuItem);
      }
    }
  }

  /**
   * Only load the default query on app launch if the SafeSearch filter is enabled.
   *
   * @return True if default query search results should be shown on first launch.
   */
  protected boolean shouldLoadDefaultQuery() {
    String filters = sharedPreferences.getString(getString(R.string.preference_safeSearch_key), "");

    return !(filters.contains("q") || filters.contains("x"));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Restore state from savedInstanceState.
    super.onCreate(savedInstanceState);

    // Get shared preferences.
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    // Inflate views.
    setContentView(R.layout.activity_search);

    // Get search result grid fragment from fragment manager.
    searchResultGridFragment = (SearchResultGridFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_searchResultGrid);

    SearchClient.Settings searchClientSettings;
    // Try restoring the SearchClient from savedInstanceState
    if (savedInstanceState != null) {
      if (this.searchClient == null && savedInstanceState.containsKey(BUNDLE_ID_SEARCH_CLIENT_SETTINGS)) {
        searchClientSettings = savedInstanceState.getParcelable(BUNDLE_ID_SEARCH_CLIENT_SETTINGS);
        if (searchClientSettings != null) {
          searchClient = searchClientSettings.createSearchClient(this);
        }
      }
    } else {
      Intent intent = getIntent();
      // If the activity was started from a Search intent, create the SearchClient object and submit search.
      if (intent != null && intent.getAction().equals(Intent.ACTION_SEARCH) && searchResultGridFragment.getSearchResult() == null) {
        searchClientSettings = intent.getParcelableExtra(BUNDLE_ID_SEARCH_CLIENT_SETTINGS);
        searchClient = searchClientSettings.createSearchClient(this);
        doSearch(intent.getStringExtra(BUNDLE_ID_SEARCH_QUERY));
      }
      showDonationDialog();
    }

    // Set up the dropdown API server picker.
    setUpActionBar();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    // Cancel pending API callbacks.
    if (searchCallback != null) {
      searchCallback.cancel();
    }
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    // Make saved instance state available to other methods.
    this.savedInstanceState = savedInstanceState;
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    // Preserve SearchView state.
    if (searchView != null) {
      outState.putCharSequence(BUNDLE_ID_SEARCH_QUERY, searchView.getQuery());
      outState.putBoolean(BUNDLE_ID_SEARCH_VIEW_IS_EXPANDED, MenuItemCompat.isActionViewExpanded(searchMenuItem));
      outState.putBoolean(BUNDLE_ID_SEARCH_VIEW_IS_FOCUSED, searchView.isFocused());
      if (searchClient != null) {
        outState.putParcelable(BUNDLE_ID_SEARCH_CLIENT_SETTINGS, searchClient.getSettings());
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.search, menu);
    // Set up action bar search view.
    setUpSearchView(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    switch (item.getItemId()) {
      case R.id.action_settings:
        startActivity(new Intent(SearchActivity.this, SettingsActivity.class));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onImageSelected(Image image, int position) {
    // Open ImageViewerActivity.
    final Intent intent = new Intent(SearchActivity.this, ImageViewerActivity.class);
    intent.putExtra(BUNDLE_ID_SEARCH_RESULT,
        searchResultGridFragment.getSearchResult().getSearchResultForPage(image.searchPage));
    intent.putExtra(BUNDLE_ID_IMAGE_INDEX, image.searchPagePosition);
    intent.putExtra(BUNDLE_ID_SEARCH_CLIENT_SETTINGS, searchClient.getSettings());
    startActivity(intent);
  }

  @Override
  public void fetchMoreImages(SearchResult searchResult) {
    // Ignore request if there is another API request pending.
    if (searchCallback != null) {
      return;
    }
    // Show progress bar in ActionBar.
    searchProgressBar.setVisibility(View.VISIBLE);
    // Request search result from API client.
    searchCallback = new SearchResultCallback(searchResult);
    searchClient.search(Tag.stringFromArray(searchResult.getQuery()), searchResult.getCurrentOffset() + 1, searchCallback);
  }

  /** Callback waiting for a SearchResult received on a background thread from the Search API. */
  private class SearchResultCallback implements SearchClient.SearchCallback {
    /** Search result to extend when fetching more images for endless scrolling. */
    private final SearchResult searchResult;
    /** Callback cancelled and should no longer respond to received SearchResult. */
    private boolean isCancelled = false;

    /** Default constructor. */
    public SearchResultCallback() {
      this.searchResult = null;
    }

    /** Constructor used to add more images to an existing SearchResult to implement endless scrolling. */
    public SearchResultCallback(SearchResult searchResult) {
      this.searchResult = searchResult;
    }

    @Override
    public void onFailure(IOException e) {
      if (!isCancelled) {
        // Show error message to user.
        Snackbar.make(findViewById(R.id.root), String.format(getString(R.string.toast_networkError),
            e.getLocalizedMessage()), Snackbar.LENGTH_INDEFINITE).show();
        // Clear callback and hide progress indicator in Action Bar.
        searchProgressBar.setVisibility(View.GONE);
        searchCallback = null;
      }
    }

    @Override
    public void onSuccess(SearchResult searchResult) {
      if (!isCancelled) {
        // Clear callback and hide progress indicator in Action Bar.
        searchProgressBar.setVisibility(View.GONE);
        searchCallback = null;

        // Filter the received SearchResult.
        final int resultCount = searchResult.getImages().length;
        if (sharedPreferences.contains(getString(R.string.preference_safeSearch_key)) &&
            !TextUtils.isEmpty(sharedPreferences.getString(getString(R.string.preference_safeSearch_key), "").trim())) {
          // Get filter from shared preferences.
          searchResult.filter(Image.SafeSearchRating.arrayFromStrings(
              sharedPreferences.getString(getString(R.string.preference_safeSearch_key), "").split(" ")));
        } else {
          // Get default filter from resources.
          searchResult.filter(Image.SafeSearchRating.arrayFromStrings(getResources().getStringArray(R.array.preference_safeSearch_defaultValues)));
        }
        if (sharedPreferences.contains(getString(R.string.preference_tagFilter_key))) {
          // Get tag filters from shared preferences and filter the result.
          searchResult.filter(Tag.arrayFromString(sharedPreferences.getString(getString(R.string.preference_tagFilter_key), "")));
        }

        if (this.searchResult != null) {
          // Set onLastPage if no more images were fetched.
          if (resultCount == 0) {
            this.searchResult.onLastPage();
          } else {
            // Extend existing search result for endless scrolling.
            this.searchResult.addImages(searchResult.getImages(), searchResult.getCurrentOffset());
            searchResultGridFragment.setSearchResult(this.searchResult);
          }
        } else {
          // Show search result.
          if (resultCount == 0) {
            searchResult.onLastPage();
          } else {
            addSearchHistoryEntry(Tag.stringFromArray(searchResult.getQuery()));
          }
          searchResultGridFragment.setSearchResult(searchResult);
        }
      }
    }

    /**
     * Adds a new entry to the {@link SearchSuggestionDatabase} on a background thread
     * (to prevent blocking the UI thread with database I/O).
     *
     * @param query Query string searched for by the user.
     */
    private void addSearchHistoryEntry(final String query) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          // Add query string to the database.
          SearchSuggestionDatabase searchSuggestionDatabase = new SearchSuggestionDatabase(SearchActivity.this);
          searchSuggestionDatabase.insert(query);
          searchSuggestionDatabase.close();
        }
      }).start();
    }

    /** Cancels this callback. */
    public void cancel() {
      this.isCancelled = true;
    }
  }

  /** Adapter populating the Search API picker in the ActionBar. */
  private class ServiceDropdownAdapter extends BaseAdapter implements LoaderManager.LoaderCallbacks<List<Pair<Integer, SearchClient.Settings>>>, AdapterView.OnItemSelectedListener {
    /** Search client settings loader ID. */
    private static final int LOADER_ID_API_SETTINGS = 0x00;
    /** Shared preference key used to store the last active {@link io.github.tjg1.library.norilib.clients.SearchClient}. */
    private static final String SHARED_PREFERENCE_LAST_SELECTED_INDEX = "io.github.tjg1.nori.SearchActivity.lastSelectedServiceIndex";
    /** List of service settings loaded from {@link io.github.tjg1.nori.database.APISettingsDatabase}. */
    private List<Pair<Integer, SearchClient.Settings>> settingsList;
    /** ID of the last selected item. */
    private long lastSelectedItem;

    public ServiceDropdownAdapter() {
      // Restore last active item from SharedPreferences.
      lastSelectedItem = sharedPreferences.getLong(SHARED_PREFERENCE_LAST_SELECTED_INDEX, 1L);
      // Initialize the search client settings database loader.
      getSupportLoaderManager().initLoader(LOADER_ID_API_SETTINGS, null, this);
    }

    @Override
    public int getCount() {
      if (settingsList == null) {
        return 1;
      } else {
        return settingsList.size() + 1;
      }
    }

    @Override
    public SearchClient.Settings getItem(int position) {
      if (settingsList == null || position == settingsList.size()) // APISettingActivity View.
        return null;
      return settingsList.get(position).second;
    }

    @Override
    public long getItemId(int position) {
      if (settingsList == null || position == settingsList.size()) // APISettingActivity View.
        return -1;
      // Return database row ID.
      return settingsList.get(position).first;
    }

    /**
     * Get position of the item with given database row ID.
     *
     * @param id Row ID.
     * @return Position of the item.
     */
    public int getPositionByItemId(long id) {
      for (int i = 0; i < getCount(); i++) {
        if (getItemId(i) == id) {
          return i;
        }
      }
      return 0;
    }

    @Override
    public View getView(int position, View recycledView, ViewGroup container) {
      // Reuse recycled view, if possible.
      @SuppressLint("ViewHolder") View view = LayoutInflater.from(SearchActivity.this)
          .inflate(R.layout.simple_dropdown_item, container, false);

      // Populate views with content.
      TextView text1 = (TextView) view.findViewById(android.R.id.text1);
      SearchClient.Settings settings = getItem(position);
      if (settings != null) {
        text1.setText(settings.getName());
      } else {
        text1.setText(R.string.service_dropdown_settings);
        view.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            startActivity(new Intent(SearchActivity.this, APISettingsActivity.class));
          }
        });
      }

      return view;
    }

    @Override
    public Loader<List<Pair<Integer, SearchClient.Settings>>> onCreateLoader(int id, Bundle args) {
      if (id == LOADER_ID_API_SETTINGS) {
        return new APISettingsDatabase.Loader(SearchActivity.this);
      }
      return null;
    }

    @Override
    public void onLoadFinished(Loader<List<Pair<Integer, SearchClient.Settings>>> loader, List<Pair<Integer, SearchClient.Settings>> data) {
      if (loader.getId() == LOADER_ID_API_SETTINGS) {
        // Update adapter data.
        settingsList = data;
        notifyDataSetChanged();
        // Reselect last active item.
        if (!data.isEmpty()) {
          serviceSpinner.setSelection(getPositionByItemId(lastSelectedItem));
        } else {
          // Start APISettingActivity.
          Intent intent = new Intent(SearchActivity.this, APISettingsActivity.class);
          intent.setAction(APISettingsActivity.ACTION_CREATE_SERVICE);
          startActivity(intent);
        }
      }
    }

    @Override
    public void onLoaderReset(Loader<List<Pair<Integer, SearchClient.Settings>>> loader) {
      // Invalidate adapter's data.
      settingsList = null;
      notifyDataSetInvalidated();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
      // Save last active item to SharedPreferences.
      if (id != -1) {
        // Notify parent activity.
        onSearchAPISelected(getItem(position), id != lastSelectedItem);
        // Update last selected item id.
        lastSelectedItem = id;
        sharedPreferences.edit().putLong(SHARED_PREFERENCE_LAST_SELECTED_INDEX, id).apply();
      }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
      // Do nothing.
    }
  }
}
