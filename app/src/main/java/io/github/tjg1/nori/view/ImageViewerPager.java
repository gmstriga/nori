/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * View pager used in {@link io.github.tjg1.nori.ImageViewerActivity}. Gives touch event precedence to
 * multi-touch events sent to the {@link uk.co.senab.photoview.PhotoView} in the contained fragment.
 */
public class ImageViewerPager extends ViewPager {

  public ImageViewerPager(Context context) {
    super(context);
  }

  public ImageViewerPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    try {
      return super.onInterceptTouchEvent(ev);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
