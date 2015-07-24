/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2015, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui.activities;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import com.frostwire.android.R;
import com.frostwire.android.gui.dialogs.NewTransferDialog;
import com.frostwire.android.gui.fragments.SearchFragment;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.AbstractDialog;
import com.frostwire.android.util.ImageLoader;
import com.frostwire.logging.Logger;
import com.frostwire.search.FileSearchResult;
import com.frostwire.util.Ref;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author gubatron
 * @author aldenml
 */
public final class PreviewPlayerActivity extends AbstractActivity implements AbstractDialog.OnDialogClickListener {

    private static final Logger LOG = Logger.getLogger(PreviewPlayerActivity.class);

    public PreviewPlayerActivity() {
        super(R.layout.activity_preview_player_land);
    }

    public static WeakReference<FileSearchResult> srRef;

    private boolean isFullScreen = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void initComponents(Bundle savedInstanceState) {
        Intent i = getIntent();
        if (i == null) {
            finish();
        }

        String displayName = i.getStringExtra("displayName");
        String source = i.getStringExtra("source");
        String thumbnailUrl = i.getStringExtra("thumbnailUrl");
        final String streamUrl = i.getStringExtra("streamUrl");
        final boolean hasVideo = i.getBooleanExtra("hasVideo", false);
        final boolean audio = i.getBooleanExtra("audio", false);

        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
            int mediaTypeStrId = audio ? R.string.audio : R.string.video;
            ab.setTitle(getString(R.string.media_preview, getString(mediaTypeStrId)));
            int icon = audio ? R.drawable.browse_peer_audio_icon_selector_off :
                                   R.drawable.browse_peer_video_icon_selector_off;
            ab.setIcon(icon);
        } else {
            setTitle(displayName);
        }

        final VideoView v = findView(R.id.activity_preview_player_videoview);
        final ImageView img = findView(R.id.activity_preview_player_thumbnail);

        final TextView trackName = findView(R.id.activity_preview_player_track_name);
        final TextView artistName = findView(R.id.activity_preview_player_artist_name);
        trackName.setText(displayName);
        artistName.setText(source);

        v.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                finish();
            }
        });

        v.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                onVideoViewPrepared(audio, hasVideo, v, img);
            }
        });

        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                toggleFullScreen(v);
                return false;
            }
        });

        if (thumbnailUrl != null) {
            ImageLoader.getInstance(this).load(Uri.parse(thumbnailUrl), img);
        }

        final ImageButton downloadButton = findView(R.id.activity_preview_player_download_button);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDownloadButtonClick();
            }
        });

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final String url = getFinalUrl(streamUrl);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        v.setVideoURI(Uri.parse(url));
                        v.start();
                    }
                });
            }
        });

        t.start();
    }

    private void onVideoViewPrepared(boolean audio, boolean hasVideo, final VideoView v, final ImageView img) {
        final ImageButton downloadButton = findView(R.id.activity_preview_player_download_button);
        downloadButton.setVisibility(View.VISIBLE);

        if (!audio || hasVideo) {
            img.setVisibility(View.GONE);
        }
    }

    private void onDownloadButtonClick() {
        if (Ref.alive(srRef)) {
            NewTransferDialog dlg = NewTransferDialog.newInstance((FileSearchResult) srRef.get(), false);
            dlg.show(getFragmentManager());

            //TODO: ux log new action, download from preview.
        } else {
            finish();
        }
    }

    private String getFinalUrl(String url) {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) (new URL(url).openConnection());
            con.setInstanceFollowRedirects(false);
            con.connect();
            String location = con.getHeaderField("Location");

            if (location != null) {
                return location;
            }

        } catch (Throwable e) {
            LOG.error("Unable to detect final url", e);
        } finally {
            if (con != null) {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                    // ignore
                }
            }
        }
        return url;
    }

    @Override
    public void onDialogClick(String tag, int which) {
        if (tag.equals(NewTransferDialog.TAG) && which == AbstractDialog.BUTTON_POSITIVE) {
            if (Ref.alive(NewTransferDialog.srRef)) {
                SearchFragment.startDownload(this, NewTransferDialog.srRef.get(), getString(R.string.download_added_to_queue));
            }
            finish();
        }
    }

    private void toggleFullScreen(VideoView v) {
        DisplayMetrics metrics = new DisplayMetrics();
        final Display defaultDisplay = getWindowManager().getDefaultDisplay();
        defaultDisplay.getMetrics(metrics);
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        android.widget.FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) v.getLayoutParams();

        LinearLayout header = findView(R.id.activity_preview_player_header);
        ImageButton downloadButton =findView(R.id.activity_preview_player_download_button);
        ActionBar bar = getActionBar();

        // this one only on landscape
        LinearLayout rightSide = findView(R.id.activity_preview_player_right_side);

        // Go full screen.
        if (!isFullScreen) {
            header.setVisibility(View.GONE);
            downloadButton.setVisibility(View.GONE);
            bar.hide();

            if (rightSide != null) {
                rightSide.setVisibility(View.GONE);
            }

            System.out.println("width: " + metrics.widthPixels);
            System.out.println("height: " + metrics.heightPixels);

            if (isPortrait) {
                params.width = metrics.heightPixels;
                params.height = metrics.widthPixels;
                v.setRotation(90);
            } else {
                params.width = metrics.widthPixels;
                params.height = metrics.heightPixels;
                v.setRotation(0);
            }
            params.leftMargin = 0;
            params.rightMargin = 0;
            params.topMargin = 0;
            params.bottomMargin = 0;
            isFullScreen = true;
        } else {
            header.setVisibility(View.VISIBLE);
            downloadButton.setVisibility(View.VISIBLE);
            bar.show();
            if (rightSide != null) {
                rightSide.setVisibility(View.GONE);
            }

            v.setRotation(0);
            params.width = LinearLayout.LayoutParams.MATCH_PARENT;
            params.height = LinearLayout.LayoutParams.MATCH_PARENT;

            if (isPortrait) {
                params.bottomMargin = 20;
            } else {
                params.leftMargin = 0;
                params.rightMargin = 0;
                params.topMargin = 0;
                params.bottomMargin = 0;
            }
            isFullScreen = false;
        }
        v.setLayoutParams(params);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_preview_player_land);
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_preview_player_land);
        }
    }
}