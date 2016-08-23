/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori;


import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import io.github.tjg1.library.norilib.clients.SearchClient;
import io.github.tjg1.nori.database.APISettingsDatabase;
import io.github.tjg1.nori.fragment.EditAPISettingDialogFragment;

/** Populates the {@link android.widget.ListView} with data from {@link io.github.tjg1.nori.database.APISettingsDatabase}. */
public class ListAdapter extends BaseAdapter implements LoaderManager.LoaderCallbacks<List<Pair<Integer, SearchClient.Settings>>>, AdapterView.OnItemClickListener {
    /** {@link io.github.tjg1.nori.database.APISettingsDatabase} loader ID. */
    private static final int LOADER_ID_DATABASE_LOADER = 0x00;
    private final APISettingsActivity activity;
    /** List of {@link android.util.Pair}s mapping database row IDs to {@link io.github.tjg1.library.norilib.clients.SearchClient.Settings} objects. */
    private List<Pair<Integer, SearchClient.Settings>> settingsList;

    public ListAdapter(APISettingsActivity activity) {
        this.activity=activity;
        // Initialize the asynchronous database loader.
        activity.getSupportLoaderManager().initLoader(LOADER_ID_DATABASE_LOADER, null, this);
    }

    @Override
    public int getCount() {
        if (settingsList != null) {
            return settingsList.size();
        }
        return 0;
    }

    @Override
    public SearchClient.Settings getItem(int position) {
        return settingsList.get(position).second;
    }

    @Override
    public long getItemId(int position) {
        return settingsList.get(position).first;
    }

    @Override
    public View getView(int position, View recycledView, ViewGroup container) {
        // Recycle the view, if possible.
        View view = recycledView;

        if (view == null) {
            // Create a new instance of the view.
            view = LayoutInflater.from(activity)
                    .inflate(R.layout.listitem_service_setting, container, false);
        }

        // Get data from the List for current position.
        SearchClient.Settings settings = getItem(position);
        final long id = getItemId(position);
        // Populate views with content.
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(settings.getName());
        TextView summary = (TextView) view.findViewById(R.id.summary);
        summary.setText(settings.getEndpoint());
        // Attach onClickListener to the remove button and hook it up to the #removeSetting method.
        ImageButton actionRemove = (ImageButton) view.findViewById(R.id.action_remove);
        actionRemove.setFocusable(false);
        actionRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.removeSetting(id);
            }
        });

        return view;
    }

    @Override
    public Loader<List<Pair<Integer, SearchClient.Settings>>> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_ID_DATABASE_LOADER) {
            // Initialize the database loader.
            return new APISettingsDatabase.Loader(activity);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<Pair<Integer, SearchClient.Settings>>> loader, List<Pair<Integer, SearchClient.Settings>> data) {
        settingsList = data;
        notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<Pair<Integer, SearchClient.Settings>>> loader) {
        settingsList = null;
        notifyDataSetInvalidated();
    }

    @Override
    public void onItemClick(AdapterView<?> listView, View view, int position, long itemId) {
        // Show dialog to edit the service settings object.
        EditAPISettingDialogFragment.newInstance(itemId, getItem(position))
                .show(activity.getSupportFragmentManager(), "EditAPISettingDialogFragment");
    }
}