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

package com.frostwire.android.gui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/**
 * @author gubatron
 * @author aldenml
 * @author marcelinkaaa
 */
public final class SearchThumbnailImageView extends ImageView {
    private final MediaThumbnailImage mediaThumbnail;

    public SearchThumbnailImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mediaThumbnail = new MediaThumbnailImage(new WeakReference<View>(this));
    }

    public void setOverlayState(MediaThumbnailImage.OverlayState state) {
        mediaThumbnail.setOverlayState(state);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mediaThumbnail.drawOverlayState(canvas);
    }
}
