package com.frostwire.android.gui.adapters.menu;

import android.content.ClipData;
import android.content.Context;
import android.content.ClipboardManager;
import android.widget.Toast;

import com.frostwire.android.R;
import com.frostwire.android.gui.transfers.BittorrentDownload;
import com.frostwire.android.gui.views.MenuAction;


public class CopyMagnetMenuAction extends MenuAction {
    private final BittorrentDownload download;

    public CopyMagnetMenuAction(Context context, BittorrentDownload download, int stringId){
            super(context, R.drawable.contextmenu_icon_play_transfer, stringId);
            this.download = download;
    }

    @Override
    protected void onClick(Context context) {
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clip = ClipData.newPlainText("magnet", this.download.makeMagnetUri());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(context.getApplicationContext(), "Magnet copied to clipboard",
                Toast.LENGTH_LONG).show();
    }
}