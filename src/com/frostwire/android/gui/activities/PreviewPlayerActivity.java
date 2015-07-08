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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;
import com.frostwire.android.R;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.util.ImageLoader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * @author gubatron
 * @author aldenml
 */
public final class PreviewPlayerActivity extends AbstractActivity {

    public PreviewPlayerActivity() {
        super(R.layout.activity_preview_player);
    }

    @Override
    protected void initComponents(Bundle savedInstanceState) {
        Intent i = getIntent();
        if (i == null) {
            finish();
        }

        String displayName = i.getStringExtra("displayName");
        String thumbnailUrl = i.getStringExtra("thumbnailUrl");
        String streamUrl = i.getStringExtra("streamUrl");

        setTitle(displayName);

        final VideoView v = findView(R.id.activity_preview_player_videoview);

        v.setVideoURI(Uri.parse(streamUrl));
        v.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                finish();
            }
        });

        v.start();
        ImageLoader.getInstance(this).load(Uri.parse(thumbnailUrl), new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                v.setBackgroundDrawable(new BitmapDrawable(getResources(), bitmap));
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        });
    }
}
