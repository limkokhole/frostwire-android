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

package com.frostwire.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * @author gubatron
 * @author aldenml
 */
public final class Storages {

    private static final int OPEN_DOCUMENT_TREE_REQUEST_CODE = 10000;

    public static void openDocumentTree(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        activity.startActivityForResult(intent, OPEN_DOCUMENT_TREE_REQUEST_CODE);
    }

    public static void handleDocumentTreeResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode != OPEN_DOCUMENT_TREE_REQUEST_CODE) {
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            Uri uri = resultData.getData();

            System.out.println(uri);
        }
    }
}
