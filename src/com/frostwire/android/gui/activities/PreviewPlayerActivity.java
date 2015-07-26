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
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.*;
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
public final class PreviewPlayerActivity extends AbstractActivity implements AbstractDialog.OnDialogClickListener, TextureView.SurfaceTextureListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnInfoListener {
    //TODO: Make sure Main Music player is paused before we do anything here.
    //      we don't want two audio streams being heard simultaneously.
    private static final Logger LOG = Logger.getLogger(PreviewPlayerActivity.class);
    public static WeakReference<FileSearchResult> srRef;

    private MediaPlayer mediaPlayer;
    private Surface surface;
    private String displayName;
    private String source;
    private String thumbnailUrl;
    private String streamUrl;
    private boolean hasVideo;
    private boolean audio;
    private boolean isFullScreen = false;
    private boolean videoSizeSetupDone = false;
    private boolean changedActionBarTitleToNonBuffering = false;

    public PreviewPlayerActivity() {
        super(R.layout.activity_preview_player);
    }

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

        displayName = i.getStringExtra("displayName");
        source = i.getStringExtra("source");
        thumbnailUrl = i.getStringExtra("thumbnailUrl");
        streamUrl = i.getStringExtra("streamUrl");
        hasVideo = i.getBooleanExtra("hasVideo", false);
        audio = i.getBooleanExtra("audio", false);
        isFullScreen = i.getBooleanExtra("isFullScreen", false);

        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
            int mediaTypeStrId = audio ? R.string.audio : R.string.video;
            ab.setTitle(getString(R.string.media_preview, getString(mediaTypeStrId)) + " (buffering...)");
            int icon = audio ? R.drawable.browse_peer_audio_icon_selector_off :
                                   R.drawable.browse_peer_video_icon_selector_off;
            ab.setIcon(icon);
        } else {
            setTitle(displayName);
        }

        final TextureView v = findView(R.id.activity_preview_player_videoview);
        v.setSurfaceTextureListener(this);

        // when previewing audio, we make the video view really tiny.
        // hiding it will cause the player not to play.
        if (audio) {
            final ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
            layoutParams.width = 1;
            layoutParams.height = 1;
            v.setLayoutParams(layoutParams);
        }

        final ImageView img = findView(R.id.activity_preview_player_thumbnail);

        final TextView trackName = findView(R.id.activity_preview_player_track_name);
        final TextView artistName = findView(R.id.activity_preview_player_artist_name);
        trackName.setText(displayName);
        artistName.setText(source);

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

        if (isFullScreen) {
            isFullScreen = false; //so it will make it full screen on what was an orientation change.
            toggleFullScreen(v);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("displayName", displayName);
        outState.putString("source", source);
        outState.putString("thumbnailUrl", thumbnailUrl);
        outState.putString("streamUrl", streamUrl);
        outState.putBoolean("hasVideo", hasVideo);
        outState.putBoolean("audio", audio);
        outState.putBoolean("isFullScreen", isFullScreen);
        if (mediaPlayer!=null && mediaPlayer.isPlaying()) {
            outState.putInt("currentPosition", mediaPlayer.getCurrentPosition());
        }
    }

    private void onVideoViewPrepared(final ImageView img) {
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

    private void toggleFullScreen(TextureView v) {
        videoSizeSetupDone = false;
        DisplayMetrics metrics = new DisplayMetrics();
        final Display defaultDisplay = getWindowManager().getDefaultDisplay();
        defaultDisplay.getMetrics(metrics);
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();

        final FrameLayout frameLayout = (FrameLayout) findView(R.id.activity_preview_player_framelayout);
        LinearLayout.LayoutParams frameLayoutParams = (LinearLayout.LayoutParams) frameLayout.getLayoutParams();

        LinearLayout header = findView(R.id.activity_preview_player_header);
        ImageView thumbnail = findView(R.id.activity_preview_player_thumbnail);
        ImageButton downloadButton =findView(R.id.activity_preview_player_download_button);
        ActionBar bar = getActionBar();

        // these ones only exist on landscape mode.
        LinearLayout rightSide = findView(R.id.activity_preview_player_right_side);
        View divider = findView(R.id.activity_preview_player_divider);
        View filler = findView(R.id.activity_preview_player_filler);

        System.out.println("========================================\n");
        System.out.println("audio: " + audio);
        System.out.println("hasVideo: " + hasVideo);
        System.out.println("width: " + metrics.widthPixels);
        System.out.println("height: " + metrics.heightPixels);
        System.out.println(isPortrait ? "portrait" : "landscape");
        System.out.println("was fullScreen? " + isFullScreen);
        System.out.println("========================================\n");

        // Let's Go into full screen mode.
        if (!isFullScreen) {
            bar.hide();
            setViewsVisibility(View.GONE, header, thumbnail, divider, downloadButton, rightSide, filler);

            if (isPortrait) {
                frameLayoutParams.width = metrics.heightPixels;
                frameLayoutParams.height = metrics.widthPixels;
            } else {
                frameLayoutParams.width = metrics.widthPixels;
                frameLayoutParams.height = metrics.heightPixels;
            }
            isFullScreen = true;
        } else {
            // restore components back from full screen mode.
            bar.show();
            setViewsVisibility(View.VISIBLE, header, divider, downloadButton, rightSide, filler);
            v.setRotation(0);

            // restore the thumbnail on the way back only if doing audio preview.
            thumbnail.setVisibility((!audio || hasVideo) ? View.GONE : View.VISIBLE);

            if (isPortrait) {
                frameLayoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;
                frameLayoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT;
            } else {
                frameLayoutParams.width = FrameLayout.LayoutParams.WRAP_CONTENT;
                frameLayoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT;
            }
            isFullScreen = false;
        }
        System.out.println("is fullScreen? " + isFullScreen);
        System.out.println("========================================\n");

        frameLayout.setLayoutParams(frameLayoutParams);
        v.setLayoutParams(params);
        changeVideoSize(frameLayoutParams.width, frameLayoutParams.height);
    }

    private void changeVideoSize(int width, int height) {
        System.out.println("\n\nchangeVideoSize -> " + width + "x" + height);
        if (width == -1 || height == -1) {
            return;
        }

        final TextureView v = findView(R.id.activity_preview_player_videoview);
        DisplayMetrics metrics = new DisplayMetrics();
        final Display defaultDisplay = getWindowManager().getDefaultDisplay();
        defaultDisplay.getMetrics(metrics);

        FrameLayout.LayoutParams params;
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        float scale = (width * 1.0f) / (height * 1.0f);
        float rotation = isFullScreen ? -90.0f : 0;

        if (isPortrait) {
            if (isFullScreen) {
                params = new FrameLayout.LayoutParams(metrics.heightPixels, metrics.widthPixels);
            } else {
                int scaledHeight = (int) (metrics.heightPixels / scale);
                System.out.println("scaledHeight: " + scaledHeight);
                params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, scaledHeight);
            }
        } else {
            if (isFullScreen) {
                params = new FrameLayout.LayoutParams(metrics.widthPixels, metrics.heightPixels);
            } else {
                int scaledWidth = (int) (metrics.widthPixels * scale);
                System.out.println("scaledWidth: " + scaledWidth);
                params = new FrameLayout.LayoutParams(scaledWidth, FrameLayout.LayoutParams.MATCH_PARENT);
            }
            rotation = 0f;
        }

        v.setRotation(rotation);
        v.setScaleX(scale);
        v.setLayoutParams(params);
        videoSizeSetupDone = true;
    }

    private void setViewsVisibility(int visibility, View ... views) {
        if (visibility != View.INVISIBLE && visibility != View.VISIBLE && visibility != View.GONE) {
            return; //invalid visibility constant.
        }
        if (views != null) {
            for (int i=0; i < views.length; i++) {
                View v = views[i];
                if (v != null) {
                    v.setVisibility(visibility);
                }
            }
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.setSurface(null);
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        System.out.println("onDestroy!");
        releaseMediaPlayer();
        if (mediaPlayer != null) {
            mediaPlayer.setSurface(null);
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        surface = new Surface(surfaceTexture);
        final String url = getFinalUrl(streamUrl);
        final Uri uri = Uri.parse(url);
        mediaPlayer= new MediaPlayer();
        try {
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.setSurface(surface);
            mediaPlayer.prepare();
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnVideoSizeChangedListener(this);
            mediaPlayer.setOnInfoListener(this);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.start();
        } catch (Throwable e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        System.out.println("onSurfaceTextureSizeChanged!");
        if (mediaPlayer != null) {
            //COULD DO.
            //mediaPlayer.setSurface(new Surface(surface));
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        System.out.println("onSurfaceTextureDestroyed");
        if (mediaPlayer != null) {
            mediaPlayer.setSurface(null);
            this.surface.release();
            this.surface = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //this happens all the time
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        finish();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
       final ImageView img = findView(R.id.activity_preview_player_thumbnail);
        onVideoViewPrepared(img);

        if (mp != null) {
            if (mp.getVideoHeight() > 0 && mp.getVideoWidth() > 0) {
                changeVideoSize(mp.getVideoWidth(), mp.getVideoHeight());
            }
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        if (width > 0 && height > 0 && !videoSizeSetupDone) {
            System.out.println("Video size has changed: " + width + "x" + height);
            changeVideoSize(width, height);
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        boolean startedPlayback = false;
        switch (what) {
            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                //LOG.warn("Media is too complex to decode it fast enough.");
                startedPlayback = true;
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                //LOG.warn("Start of media buffering.");
                startedPlayback = true;
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                //LOG.warn("End of media bufferring.");
                startedPlayback = true;
                break;
            case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                break;
            case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                break;
            case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                break;
            case MediaPlayer.MEDIA_INFO_UNKNOWN:
            default:
                break;
        }

        if (startedPlayback && !changedActionBarTitleToNonBuffering) {
            int mediaTypeStrId = audio ? R.string.audio : R.string.video;
            getActionBar().setTitle(getString(R.string.media_preview, getString(mediaTypeStrId)));
            changedActionBarTitleToNonBuffering = true;
        }

        return false;
    }
}