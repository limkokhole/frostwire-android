package com.frostwire.android.gui.adapters.menu;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import com.frostwire.android.R;
import com.frostwire.android.gui.transfers.BittorrentDownload;
import com.frostwire.android.gui.views.MenuAction;


public class CopyInfoHashMenuAction extends MenuAction {
    private final BittorrentDownload download;

    public CopyInfoHashMenuAction(Context context, BittorrentDownload download, int stringId){
            super(context, R.drawable.contextmenu_icon_play_transfer, stringId);
            this.download = download;
    }

    @Override
    protected void onClick(Context context) {
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clip = ClipData.newPlainText("infohash", this.download.getHash());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(context.getApplicationContext(), "InfoHash copied to clipboard",
                Toast.LENGTH_LONG).show();
    }
}