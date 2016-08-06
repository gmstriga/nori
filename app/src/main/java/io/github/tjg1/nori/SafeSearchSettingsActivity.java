/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import io.github.tjg1.nori.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SafeSearchSettingsActivity extends AppCompatActivity implements ListView.OnItemClickListener, CompoundButton.OnCheckedChangeListener {
  /** Default {@link android.content.SharedPreferences} object. */
  private SharedPreferences sharedPreferences;
  /** Human-readable labels for each SafeSearch setting. */
  private String[] safeSearchEntries;
  /** Human-readable summaries for each SafeSearch setting. */
  private String[] safeSearchSummaries;
  /** Values for each SafeSearch setting stored in {@link android.content.SharedPreferences} */
  private String[] safeSearchValues;
  /** Current values of the preference_safeSearch preference. */
  private List<String> safeSearchCurrentSetting = new ArrayList<>(4);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_safe_search_settings);

    // Set Toolbar as the app bar.
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    // Hide the action bar icon and use the activity title as the home button.
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayShowHomeEnabled(false);
      actionBar.setDisplayShowTitleEnabled(true);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    // Get shared preference object.
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    // Get string array resources.
    safeSearchEntries = getResources().getStringArray(R.array.preference_safeSearch_entries);
    safeSearchSummaries = getResources().getStringArray(R.array.preference_safeSearch_summaries);
    safeSearchValues = getResources().getStringArray(R.array.preference_safeSearch_values);

    // Get current value of the preference_safeSearch preference, or fallback to the default value.
    if (sharedPreferences.contains(getString(R.string.preference_safeSearch_key))) {
      String safeSearchPreference = sharedPreferences.getString(getString(R.string.preference_safeSearch_key), null);
      if (!TextUtils.isEmpty(safeSearchPreference)) {
        safeSearchPreference = safeSearchPreference.trim();
        safeSearchCurrentSetting.addAll((Arrays.asList(safeSearchPreference.split(" "))));
      }
    } else {
      final String[] obscenityRatingDefaultValues = getResources().getStringArray(R.array.preference_safeSearch_defaultValues);
      safeSearchCurrentSetting.addAll(Arrays.asList(obscenityRatingDefaultValues));
    }

    // Set up ListView.
    final ListView listView = (ListView) findViewById(android.R.id.list);
    listView.setAdapter(new SafeSearchListAdapter());
    listView.setOnItemClickListener(this);
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

  @Override
  public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
    // Toggle checkbox when List item is clicked.
    final Checkable checkBox = (Checkable) view.findViewById(R.id.checkbox);
    checkBox.toggle();
  }

  @SuppressWarnings("RedundantCast")
  @Override
  public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
    if (checked && !safeSearchCurrentSetting.contains((String) compoundButton.getTag())) {
      safeSearchCurrentSetting.add((String) compoundButton.getTag());
    } else if (!checked && safeSearchCurrentSetting.contains((String) compoundButton.getTag())) {
      safeSearchCurrentSetting.remove((String) compoundButton.getTag());
    }

    // Update SharedPreferences.
    sharedPreferences.edit()
        .putString(getString(R.string.preference_safeSearch_key),
            StringUtils.mergeStringArray(safeSearchCurrentSetting.toArray(new String[safeSearchCurrentSetting.size()]), " ").trim())
        .apply();
  }

  private class SafeSearchListAdapter extends BaseAdapter {

    public int getCount() {
      return safeSearchEntries.length;
    }

    @Override
    public Object getItem(int position) {
      return safeSearchEntries[position];
    }

    @Override
    public long getItemId(int position) {
      // Position == item id.
      return position;
    }

    @Override
    public View getView(int position, View recycledView, ViewGroup container) {
      // Recycle view if possible.
      View view = recycledView;
      if (view == null) {
        final LayoutInflater inflater = getLayoutInflater();
        view = inflater.inflate(R.layout.listitem_obscenity_rating, container, false);
      }

      // Populate views.
      final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
      checkBox.setChecked(safeSearchCurrentSetting.contains(safeSearchValues[position]));
      checkBox.setOnCheckedChangeListener(SafeSearchSettingsActivity.this);
      checkBox.setTag(safeSearchValues[position]);
      final TextView title = (TextView) view.findViewById(R.id.title);
      title.setText(safeSearchEntries[position]);
      final TextView summary = (TextView) view.findViewById(R.id.summary);
      summary.setText(safeSearchSummaries[position]);

      return view;
    }
  }
}
