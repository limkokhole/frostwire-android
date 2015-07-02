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

package com.frostwire.android.gui.helpers;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.widget.RadioButton;
import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.logging.Logger;


/**
 * This class uses browse_peer_button_selector.xml, browse_peer_button_selector_on.xlm and
 * browse_peer_button_selector_off.xml and dynamically sets and scales the proper bitmap
 * for the radio buttons depending on the given fileType, to avoid having 3 different XML files
 * per radio button.
 */
public final class FileTypeRadioButtonSelectorFactory {
    private static Logger LOG = Logger.getLogger(FileTypeRadioButtonSelectorFactory.class);

    public enum RadioButtonContainerType {
        SEARCH,
        BROWSE
    }
    private byte fileType;
    private Resources r;
    private LayerDrawable selectorOn;
    private LayerDrawable selectorOff;
    private RadioButtonContainerType containerType;

    public FileTypeRadioButtonSelectorFactory(byte fileType, Resources r, RadioButtonContainerType containerType) {
        this.fileType = fileType;
        this.r = r;
        this.containerType = containerType;
        this.init();
    }

    public LayerDrawable getSelectorOn() {
        return selectorOn;
    }

    public LayerDrawable getSelectorOff() {
        return selectorOff;
    }

    public RadioButtonContainerType getContainerType() {
        return containerType;
    }

    public void updateButtonBackground(RadioButton button) {
        LayerDrawable drawable = button.isChecked() ? getSelectorOn() : getSelectorOff();
        if (getContainerType() == RadioButtonContainerType.SEARCH) {
            // things are a bit different for the radio buttons on the search screen.
            if (button.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // android:drawableTop
                button.setBackgroundDrawable(null);
                button.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
            } else {
                button.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                button.setBackgroundDrawable(drawable);
            }
        } else {
            button.setBackgroundDrawable(drawable);
        }
    }

    private void init() {
        // Get layer-lists to modify.
        if (containerType == RadioButtonContainerType.BROWSE) {
            selectorOn = (LayerDrawable) r.getDrawable(R.drawable.browse_peer_button_selector_on);
            selectorOff = (LayerDrawable) r.getDrawable(R.drawable.browse_peer_button_selector_off);
        } else if (containerType == RadioButtonContainerType.SEARCH) {
            selectorOn = (LayerDrawable) r.getDrawable(R.drawable.search_peer_button_selector_on);
            selectorOff = (LayerDrawable) r.getDrawable(R.drawable.search_peer_button_selector_off);
        }

        // Load the image we want for this file type.
        int selectorOnDrawableId = R.drawable.browse_peer_audio_icon_selector_on;
        int selectorOffDrawableId = R.drawable.browse_peer_audio_icon_selector_off;

        LOG.info("Got fileType: " + fileType);
        switch (fileType) {
            case Constants.FILE_TYPE_AUDIO:
                selectorOnDrawableId = R.drawable.browse_peer_audio_icon_selector_on;
                selectorOffDrawableId = R.drawable.browse_peer_audio_icon_selector_off;
                break;
            case Constants.FILE_TYPE_APPLICATIONS:
                selectorOnDrawableId = R.drawable.browse_peer_application_icon_selector_on;
                selectorOffDrawableId = R.drawable.browse_peer_application_icon_selector_off;
                break;
            case Constants.FILE_TYPE_DOCUMENTS:
                selectorOnDrawableId = R.drawable.browse_peer_document_icon_selector_on;
                selectorOffDrawableId = R.drawable.browse_peer_document_icon_selector_off;
                break;
            case Constants.FILE_TYPE_PICTURES:
                selectorOnDrawableId = R.drawable.browse_peer_picture_icon_selector_on;
                selectorOffDrawableId = R.drawable.browse_peer_picture_icon_selector_off;
                break;
            case Constants.FILE_TYPE_RINGTONES:
                selectorOnDrawableId = R.drawable.browse_peer_ringtone_icon_selector_on;
                selectorOffDrawableId = R.drawable.browse_peer_ringtone_icon_selector_off;
                break;
            case Constants.FILE_TYPE_TORRENTS:
                selectorOnDrawableId = R.drawable.browse_peer_torrent_icon_selector_on;
                selectorOffDrawableId = R.drawable.browse_peer_torrent_icon_selector_off;
                break;
            case Constants.FILE_TYPE_VIDEOS:
                selectorOnDrawableId = R.drawable.browse_peer_video_icon_selector_on;
                selectorOffDrawableId = R.drawable.browse_peer_video_icon_selector_off;
                break;
        }

        final BitmapDrawable iconOn = (BitmapDrawable) r.getDrawable(selectorOnDrawableId);
        final BitmapDrawable iconOff = (BitmapDrawable) r.getDrawable(selectorOffDrawableId);

        // Fix scaling.
        iconOn.setGravity(Gravity.CENTER);
        iconOff.setGravity(Gravity.CENTER);

        int onBitmapId = (containerType == RadioButtonContainerType.SEARCH) ?
                R.id.search_peer_button_selector_on_bitmap :
                R.id.browse_peer_button_selector_on_bitmap;

        int offBitmapId = (containerType == RadioButtonContainerType.SEARCH) ?
                R.id.search_peer_button_selector_off_bitmap :
                R.id.browse_peer_button_selector_off_bitmap;

        selectorOn.setDrawableByLayerId(onBitmapId, iconOn);
        selectorOff.setDrawableByLayerId(offBitmapId, iconOff);
    }
}