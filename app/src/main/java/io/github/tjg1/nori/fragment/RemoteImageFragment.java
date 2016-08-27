/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.builder.AnimateGifMode;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.nori.BuildConfig;
import io.github.tjg1.nori.R;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Fragment using the {@link PhotoView} widget
 * and the Picasso HTTP image loading library to display images.
 */
public class RemoteImageFragment extends ImageFragment {
  /** Progress bar used to display image fetch progress. */
  private ProgressBar progressBar;
  /** True, when the image is done loading. */

  /** Required public empty constructor. */
  public RemoteImageFragment() {
  }

  /**
   * Factory method used to construct new fragments
   *
   * @param image Image object to display in the created fragment.
   * @return New RemoteImageFragment with the image object appended to its arguments bundle.
   */
  public static RemoteImageFragment newInstance(Image image) {
    // Create a new instance of the fragment.
    RemoteImageFragment fragment = new RemoteImageFragment();

    // Add the image object to the fragment's arguments Bundle.
    Bundle arguments = new Bundle();
    arguments.putParcelable(BUNDLE_ID_IMAGE, image);
    fragment.setArguments(arguments);

    return fragment;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_remote_image, container, false);
    final TextView textView = (TextView) view.findViewById(R.id.errorText);

    // Initialize the ProgressBar.
    progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
    if (isActive) {
      progressBar.setVisibility(View.VISIBLE);
    }

    // Initialize the ImageView widget.
    PhotoView photoView = (PhotoView) view.findViewById(R.id.imageView);
    photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
    photoView.setMaximumScale(4);
    photoView.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
      @Override
      public void onViewTap(View view, float x, float y) {
        listener.onViewTap(view, x, y);
      }
    });



    // Load image into the view.
    String imageUrl = shouldLoadImageSamples() ? image.sampleUrl : image.fileUrl;
    Ion.with(this)
        .load(imageUrl)
        .progressBar(progressBar)
        .userAgent("nori/" + BuildConfig.VERSION_NAME)
        .withBitmap()
        .animateGif(AnimateGifMode.ANIMATE)
        .deepZoom()
        .intoImageView(photoView)
        .setCallback(new FutureCallback<ImageView>() {
          @Override
          public void onCompleted(Exception e, ImageView result) {
            if (e != null) {
              textView.setVisibility(View.VISIBLE);
              textView.setText(e.getLocalizedMessage());
            }

            progressBar.setProgress(100); // for cached images.
            progressBar.setVisibility(View.GONE);
          }
        });

    return view;
  }

  @Override
  public void onShown() {
    super.onShown();

    if (progressBar != null && progressBar.getProgress() < 100) {
      progressBar.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void onHidden() {
    super.onHidden();

    if (progressBar != null) {
      progressBar.setVisibility(View.GONE);
    }
  }
}
