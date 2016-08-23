package io.github.tjg1.nori;

import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import java.util.Locale;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.nori.fragment.ImageFragment;
import io.github.tjg1.nori.fragment.RemoteImageFragment;
import io.github.tjg1.nori.fragment.VideoPlayerFragment;

/** Adapter used to populate {@link android.support.v4.view.ViewPager} with {@link io.github.tjg1.nori.fragment.ImageFragment}s. */
public class ImagePagerAdapter extends FragmentStatePagerAdapter {
    private final ImageViewerActivity activity;
    private ImageFragment activeFragment;

    public ImagePagerAdapter(FragmentManager fm,ImageViewerActivity activity) {
        super(fm);
        this.activity = activity;
    }

    @Override
    public Fragment getItem(int position) {
        // Create a new instance of ImageFragment for the given image.
        Image image = activity.getSearchResult().getImages()[position];

        if (shouldUseVideoPlayerFragment(image)) {
            return VideoPlayerFragment.newInstance(image);
        } else {
            return RemoteImageFragment.newInstance(image);
        }
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        if (activeFragment != object) {
            if (activeFragment != null) {
                activeFragment.onHidden();
            }
            activeFragment = (ImageFragment) object;
            activeFragment.onShown();
        }
    }

    @Override
    public int getCount() {
        // Return the search result count.
        if (activity.getSearchResult() == null) {
            return 0;
        }
        return activity.getSearchResult().getImages().length;
    }

    /** Returns true if the {@link Image} object is a WebM/MP4 animation. */
    private boolean shouldUseVideoPlayerFragment(Image image) {
        String path = Uri.parse(image.fileUrl).getPath();
        String fileExt = path.contains(".") ? path.toLowerCase(Locale.US)
                .substring(path.lastIndexOf(".") + 1) : null;
        return "mp4".equals(fileExt) || "webm".equals(fileExt);
    }
}
