/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.fragment;

import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.nori.R;
import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;

/**
 * Fragment using the {@link it.sephiroth.android.library.imagezoom.ImageViewTouch} widget
 * and the Picasso HTTP image loading library to display images.
 */
public class PicassoImageFragment extends ImageFragment {
  /** Widget used to display the image. */
  private ImageViewTouch imageView;

  /**
   * Factory method used to construct new fragments
   *
   * @param image Image object to display in the created fragment.
   * @return New PicassoImageFragment with the image object appended to its arguments bundle.
   */
  public static PicassoImageFragment newInstance(Image image) {
    // Create a new instance of the fragment.
    PicassoImageFragment fragment = new PicassoImageFragment();

    // Add the image object to the fragment's arguments Bundle.
    Bundle arguments = new Bundle();
    arguments.putParcelable(BUNDLE_ID_IMAGE, image);
    fragment.setArguments(arguments);

    return fragment;
  }

  /** Required public empty constructor. */
  public PicassoImageFragment() {
  }

  @Override
  public boolean canScroll(int direction) {
    if (imageView != null) {
      boolean canScroll = imageView.canScroll(direction);

      // Hack to fix a bug in ImageViewTouch when image.width < view.width.
      if (canScroll) {
        RectF bitmapRect = imageView.getBitmapRect();
        Rect imageViewRect = new Rect();
        imageView.getGlobalVisibleRect(imageViewRect);

        if (bitmapRect != null && (bitmapRect.width() < imageViewRect.width())) {
          return false;
        }
      }

      return canScroll;
    }
    return false;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_picasso_image, container, false);

    // Initialize the ImageView widget.
    imageView = (ImageViewTouch) view.findViewById(R.id.imageView);
    imageView.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);

    // Load image into the view.
    String imageUrl = shouldLoadImageSamples() ? image.sampleUrl : image.fileUrl;
    Picasso.with(getContext())
        .load(imageUrl)
        .into(imageView);

    return view;
  }
}
