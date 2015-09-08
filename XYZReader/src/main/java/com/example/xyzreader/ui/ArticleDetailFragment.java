package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<Cursor> {
  public static final String ARG_ITEM_ID = "item_id";
  private static final String TAG = "ArticleDetailFragment";
  private static final float PARALLAX_FACTOR = 1.25f;
  private static final String EXTRA_IMAGE = "extra_image";
  private static final String ARG_ITEM_TITLE = "item_title";

  private Cursor mCursor;
  private long mItemId;
  private View mRootView;
  private String title;
  private Toolbar toolbar;
  private CollapsingToolbarLayout collapsingToolbarLayout;
  private AppBarLayout appBarLayout;
  private FloatingActionButton floatingActionButton;
  private ImageView headerImage;
  private TextView bylineView;
  private TextView bodyView;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public ArticleDetailFragment() {
  }

  public static ArticleDetailFragment newInstance(long itemId, String title) {
    Bundle arguments = new Bundle();
    arguments.putLong(ARG_ITEM_ID, itemId);
    arguments.putString(ARG_ITEM_TITLE, title);
    ArticleDetailFragment fragment = new ArticleDetailFragment();
    fragment.setArguments(arguments);
    return fragment;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getArguments().containsKey(ARG_ITEM_ID)) {
      mItemId = getArguments().getLong(ARG_ITEM_ID);
    }
    setHasOptionsMenu(true);
  }

  public ArticleDetailActivity getActivityCast() {
    return (ArticleDetailActivity) getActivity();
  }

  @Override public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    getLoaderManager().initLoader(0, null, this);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
    toolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
    collapsingToolbarLayout =
        (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsing_toolbar);
    appBarLayout = (AppBarLayout) mRootView.findViewById(R.id.appbar);
    floatingActionButton = (FloatingActionButton) mRootView.findViewById(R.id.fab);
    headerImage = (ImageView) mRootView.findViewById(R.id.header_image);
    bylineView = (TextView) mRootView.findViewById(R.id.article_subtitle);
    bylineView.setMovementMethod(new LinkMovementMethod());
    getActivityCast().setSupportActionBar(toolbar);
    getActivityCast().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    ViewCompat.setTransitionName(appBarLayout, EXTRA_IMAGE);
    bodyView = (TextView) mRootView.findViewById(R.id.article_body);
    title = getArguments().getString(ARG_ITEM_TITLE);
    collapsingToolbarLayout.setTitle(title);
    //toolbar.setTitle(title);
    //getActivityCast().getSupportActionBar().setTitle(title);
    headerImage.setImageDrawable(getActivityCast().getResources().getDrawable(R.drawable.empty_detail));
    return mRootView;
  }

  private void bindViews() {
    if (mRootView == null) {
      return;
    }

    if (mCursor != null) {
      collapsingToolbarLayout.setTitle(title);
      bylineView.setText(Html.fromHtml(
          DateUtils.getRelativeTimeSpanString(mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
              System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL)
              .toString() + " by <font color='#ff333333'>" + mCursor.getString(
              ArticleLoader.Query.AUTHOR) + "</font>"));

      bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));
      ImageLoaderHelper.getInstance(getActivity())
          .getImageLoader()
          .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
            @Override public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
              Log.e("finally", "get a response hurray");
              final Bitmap bitmap = imageContainer.getBitmap();
              if (bitmap != null) {
                headerImage.setImageBitmap(bitmap);
                Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                  @Override public void onGenerated(Palette palette) {
                    collapsingToolbarLayout.setContentScrimColor(palette.getMutedColor(0xFF333333));
                    collapsingToolbarLayout.setStatusBarScrimColor(
                        palette.getDarkMutedColor(0xFF333333));
                    floatingActionButton.setRippleColor(palette.getVibrantColor(0xFF333333));
                    floatingActionButton.setBackgroundTintList(
                        ColorStateList.valueOf(palette.getVibrantColor(0xFF333333)));
                  }
                });
              }
            }

            @Override public void onErrorResponse(VolleyError volleyError) {
              Log.e("ERROR", "IMAGE ERROR");
              collapsingToolbarLayout.setTitle(title);
            }
          });

      floatingActionButton.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          Intent i = new Intent();
          i.setAction(Intent.ACTION_SEND);
          i.setType("text/plain");
          i.putExtra(Intent.EXTRA_TEXT, "simple text");
          startActivity(Intent.createChooser(i, "Share"));
        }
      });
    }
  }

  @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
  }

  @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
    if (!isAdded()) {
      if (cursor != null) {
        cursor.close();
      }
      return;
    }

    mCursor = cursor;
    if (mCursor != null && !mCursor.moveToFirst()) {
      Log.e(TAG, "Error reading item detail cursor");
      mCursor.close();
      mCursor = null;
    }
    if (mCursor != null) {
      bindViews();
    }
  }

  @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
    mCursor = null;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        return getActivity().onNavigateUp();
      default:
        return super.onOptionsItemSelected(item);
    }
  }

}
