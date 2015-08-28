/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2015, FrostWire(TM). All rights reserved.
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

package com.frostwire.android.gui.affiliates;

import android.app.Activity;

import java.lang.ref.WeakReference;

public interface Affiliate {
    void initialize(final Activity activity);
    boolean enabled();
    boolean started();
    boolean showInterstitial(final WeakReference<Activity> activityRef,
                             final boolean shutdownActivityAfterwards,
                             final boolean dismissActivityAfterward);
    void loadNewInterstitial(Activity activity);
}