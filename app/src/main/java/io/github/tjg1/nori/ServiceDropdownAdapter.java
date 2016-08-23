/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import io.github.tjg1.library.norilib.clients.SearchClient;
import io.github.tjg1.nori.database.APISettingsDatabase;

/** Adapter populating the Search API picker in the ActionBar. */
public class ServiceDropdownAdapter extends BaseAdapter implements LoaderManager.LoaderCallbacks<List<Pair<Integer, SearchClient.Settings>>>, AdapterView.OnItemSelectedListener {
    /** Search client settings loader ID. */
    private static final int LOADER_ID_API_SETTINGS = 0x00;
    /** Shared preference key used to store the last active {@link io.github.tjg1.library.norilib.clients.SearchClient}. */
    private static final String SHARED_PREFERENCE_LAST_SELECTED_INDEX = "io.github.tjg1.nori.SearchActivity.lastSelectedServiceIndex";
    private final SearchActivity activity;
    /** List of service settings loaded from {@link io.github.tjg1.nori.database.APISettingsDatabase}. */
    private List<Pair<Integer, SearchClient.Settings>> settingsList;
    /** ID of the last selected item. */
    private long lastSelectedItem;

    public ServiceDropdownAdapter(SearchActivity activity) {
        this.activity = activity;
        // Restore last active item from SharedPreferences.
        lastSelectedItem = activity.getSharedPreferences().getLong(SHARED_PREFERENCE_LAST_SELECTED_INDEX, 1L);
        // Initialize the search client settings database loader.
        activity.getSupportLoaderManager().initLoader(LOADER_ID_API_SETTINGS, null, this);
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
        @SuppressLint("ViewHolder") View view = LayoutInflater.from(activity)
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
                    activity.startActivity(new Intent(activity, APISettingsActivity.class));
                }
            });
        }

        return view;
    }

    @Override
    public Loader<List<Pair<Integer, SearchClient.Settings>>> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_ID_API_SETTINGS) {
            return new APISettingsDatabase.Loader(activity);
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
                activity.getServiceSpinner().setSelection(getPositionByItemId(lastSelectedItem));
            } else {
                // Start APISettingActivity.
                Intent intent = new Intent(activity, APISettingsActivity.class);
                intent.setAction(APISettingsActivity.ACTION_CREATE_SERVICE);
                activity.startActivity(intent);
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
            activity.onSearchAPISelected(getItem(position), id != lastSelectedItem);
            // Update last selected item id.
            lastSelectedItem = id;
            activity.getSharedPreferences().edit().putLong(SHARED_PREFERENCE_LAST_SELECTED_INDEX, id).apply();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        // Do nothing.
    }
}
