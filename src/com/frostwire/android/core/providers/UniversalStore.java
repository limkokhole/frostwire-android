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

package com.frostwire.android.core.providers;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import com.frostwire.android.BuildConfig;

/**
 * The Media provider contains meta data for all available media on both
 * internal and external storage devices.
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public final class UniversalStore {

    public static final String UNIVERSAL_APPLICATIONS_AUTHORITY = BuildConfig.APPLICATION_ID + ".core.providers.Applications";
    public static final String UNIVERSAL_DOCUMENTS_AUTHORITY = BuildConfig.APPLICATION_ID + ".core.providers.Documents";

    public static final String CONTENT_UNIVERSAL_DOCUMENTS_AUTHORITY_SLASH = "content://" + UNIVERSAL_DOCUMENTS_AUTHORITY + "/";
    public static final String CONTENT_UNIVERSAL_APPLICATIONS_AUTHORITY_SLASH = "content://" + UNIVERSAL_APPLICATIONS_AUTHORITY + "/";

    public static final class Documents {

        public static final String DEFAULT_SORT_ORDER = DocumentsColumns.DATE_ADDED + " DESC";

        public interface DocumentsColumns extends MediaColumns {
        }

        public static final class Media implements DocumentsColumns {

            /**
             * Get the content:// style URI for the video media table on the
             * given volume.
             * 
             * @param volumeName
             *            the name of the volume to get the URI for
             * @return the URI to the video media table on the given volume
             */
            public static Uri getContentUri(String level) {
                return Uri.parse(CONTENT_UNIVERSAL_DOCUMENTS_AUTHORITY_SLASH + level);
            }

            /**
             * The content:// style URI for the storage.
             */
            public static final Uri CONTENT_URI = getContentUri("documents");

            public static final Uri CONTENT_URI_ITEM = getContentUri("documents/#");

            /**
             * The MIME type for this table.
             */
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/documents";

            /**
             * The MIME type for this table item.
             */
            public static final String CONTENT_TYPE_ITEM = "vnd.android.cursor.item/documents";
        }
    }

    public static final class Applications {

        public static final String DEFAULT_SORT_ORDER = ApplicationsColumns.TITLE + " ASC";

        public interface ApplicationsColumns extends MediaColumns {

            public static final String VERSION = "version";

            public static final String PACKAGE_NAME = "package_name";
        }

        public static final class Media implements ApplicationsColumns {

            /**
             * Get the content:// style URI for the video media table on the
             * given volume.
             * 
             * @param volumeName
             *            the name of the volume to get the URI for
             * @return the URI to the video media table on the given volume
             */
            public static Uri getContentUri(String level) {
                return Uri.parse(CONTENT_UNIVERSAL_APPLICATIONS_AUTHORITY_SLASH + level);
            }

            /**
             * The content:// style URI for the storage.
             */
            public static final Uri CONTENT_URI = getContentUri("applications");

            public static final Uri CONTENT_URI_ITEM = getContentUri("applications/#");

            /**
             * The MIME type for this table.
             */
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/applications";

            /**
             * The MIME type for this table item.
             */
            public static final String CONTENT_TYPE_ITEM = "vnd.android.cursor.item/applications";
        }
    }
}
