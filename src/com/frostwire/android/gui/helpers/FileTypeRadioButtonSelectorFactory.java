package com.frostwire.android.gui.helpers;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import com.frostwire.android.R;
import com.frostwire.android.core.Constants;

/**
 * This class uses browse_peer_button_selector.xml, browse_peer_button_selector_on.xlm and
 * browse_peer_button_selector_off.xml and dynamically sets and scales the proper bitmap
 * for the radio buttons depending on the given fileType, to avoid having 3 different XML files
 * per radio button.
 */
public final class FileTypeRadioButtonSelectorFactory {
    private byte fileType;
    private Resources r;
    private LayerDrawable selectorOn;
    private LayerDrawable selectorOff;

    public FileTypeRadioButtonSelectorFactory(byte fileType, Resources r) {
        this.fileType = fileType;
        this.r = r;
        this.init();
    }

    public LayerDrawable getSelectorOn() {
        return selectorOn;
    }

    public LayerDrawable getSelectorOff() {
        return selectorOff;
    }

    private void init() {
        // Get layer-lists to modify.
        selectorOn = (LayerDrawable) r.getDrawable(R.drawable.browse_peer_button_selector_on);
        selectorOff = (LayerDrawable) r.getDrawable(R.drawable.browse_peer_button_selector_off);

        // Load the image we want for this file type.
        int selectorOnDrawableId = R.drawable.browse_peer_audio_icon_selector_on;
        int selectorOffDrawableId = R.drawable.browse_peer_audio_icon_selector_off;

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
        iconOn.setGravity(Gravity.CENTER); // Fixes scaling.
        iconOff.setGravity(Gravity.CENTER);
        selectorOn.setDrawableByLayerId(R.id.browse_peer_button_selector_on_bitmap, iconOn);
        selectorOff.setDrawableByLayerId(R.id.browse_peer_button_selector_off_bitmap, iconOff);
    }
}
