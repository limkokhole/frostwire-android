package com.frostwire.android.gui.adapters.menu;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.MenuAction;

/**
 * @author gubatron
 * @author aldenml
 * @author votaguz
 */
public class CopyToClipboardMenuAction extends MenuAction {
    private final String message;
    private final String data;

    public CopyToClipboardMenuAction(Context context, int drawable, int actionNameId,
                                     int messageId, String data){

        super(context, drawable, actionNameId);
        this.message = context.getResources().getString(messageId);
        this.data = data;
    }

    @Override
    protected void onClick(Context context){
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("data", this.data);
            clipboard.setPrimaryClip(clip);
            UIUtils.showLongMessage(context, this.message);
        }
    }
}
