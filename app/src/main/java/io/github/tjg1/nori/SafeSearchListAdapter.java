/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.List;

/** Adapter for the SafeSearch setting List (in the fdroid version) */
public class SafeSearchListAdapter extends BaseAdapter implements
        CompoundButton.OnCheckedChangeListener {
    private final SafeSearchSettingsActivity activity;

    public SafeSearchListAdapter(SafeSearchSettingsActivity activity){
        this.activity = activity;
    }
    public int getCount() {
        return activity.getSafeSearchEntries().length;
    }

    @Override
    public Object getItem(int position) {
        return activity.getSafeSearchEntries()[position];
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
            final LayoutInflater inflater = activity.getLayoutInflater();
            view = inflater.inflate(R.layout.listitem_safesearch_rating, container, false);
        }

        // Populate views.
        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
        checkBox.setChecked(activity.getSafeSearchCurrentSetting().contains(activity.getSafeSearchValues()[position]));
        checkBox.setOnCheckedChangeListener(this);
        checkBox.setTag(activity.getSafeSearchValues()[position]);
        final TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(activity.getSafeSearchEntries()[position]);
        final TextView summary = (TextView) view.findViewById(R.id.summary);
        summary.setText(activity.getSafeSearchSummaries()[position]);

        return view;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        List<String> safeSearchCurrentSetting = activity.getSafeSearchCurrentSetting();

        if (checked && !safeSearchCurrentSetting.contains(compoundButton.getTag())) {
            safeSearchCurrentSetting.add((String) compoundButton.getTag());
        } else if (!checked && safeSearchCurrentSetting.contains(compoundButton.getTag())) {
            safeSearchCurrentSetting.remove(compoundButton.getTag());
        }

        // Update SharedPreferences.
        activity.updateSafeSearchSettings(safeSearchCurrentSetting
                .toArray(new String[safeSearchCurrentSetting.size()]));
    }
}