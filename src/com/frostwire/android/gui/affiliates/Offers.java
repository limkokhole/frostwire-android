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
import android.content.Context;
import android.content.Intent;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.activities.MainActivity;
import com.frostwire.logging.Logger;
import com.frostwire.util.Ref;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

public class Offers {

    private static final Logger LOG = Logger.getLogger(Offers.class);
    public static boolean MOBILE_CORE_NATIVE_ADS_READY = false;

    private final static AppLovinAffiliate APP_LOVIN = new AppLovinAffiliate();
    private final static MobileCoreAffiliate MOBILE_CORE = new MobileCoreAffiliate();
    private final static InMobiAffiliate IN_MOBI = new InMobiAffiliate();

    private static List<Affiliate> AFFILIATES;

    public static void initAffiliates(Activity activity) {
        AFFILIATES = Arrays.asList(new Affiliate[]{APP_LOVIN, IN_MOBI, MOBILE_CORE});
        for (Affiliate affiliate : AFFILIATES) {
            affiliate.initialize(activity);
        }
    }

    public static void stopAffiliateServices(Context context) {
        // Stop MobileCore if you have to.
        if (MOBILE_CORE.started()) {
            try {
                context.stopService(new Intent(context.getApplicationContext(),
                        com.ironsource.mobilcore.MobileCoreReport.class));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        // TODO: See if other SDKs leave any service running and stop them the same way if possible.
    }

    public static boolean isFreeAppsEnabled() {
        ConfigurationManager config;
        boolean isFreeAppsEnabled = false;
        try {
            config = ConfigurationManager.instance();
            isFreeAppsEnabled = (config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) && config.getBoolean(Constants.PREF_KEY_GUI_USE_MOBILE_CORE)) && Constants.IS_GOOGLE_PLAY_DISTRIBUTION;
        } catch (Throwable t) {
        }
        return isFreeAppsEnabled;
    }

    public static void onFreeAppsClick(Context context) {
        if (isFreeAppsEnabled() && MOBILE_CORE.started() && MOBILE_CORE.isDirectToMarketReady()) {
            try {
                LOG.debug("onFreeAppsClick");
                MOBILE_CORE.directToMarket((Activity) context);
            } catch (Throwable t) {
                LOG.error("can't show app wall", t);
                t.printStackTrace();
            }
        }
    }

    public static void showInterstitial(final Activity activity,
                                        final boolean shutdownAfterwards,
                                        final boolean dismissAfterwards) {
        boolean interstitialShown = false;
        final WeakReference<Activity> activityRef = Ref.weak(activity);
        for (Affiliate affiliate : AFFILIATES) {
            if (!interstitialShown && affiliate.started()) {
                interstitialShown = affiliate.showInterstitial(activityRef, shutdownAfterwards, dismissAfterwards);
            }
        }
        if (!interstitialShown) {
            if (dismissAfterwards) {
                activity.finish();
            }
            if (shutdownAfterwards && activity instanceof MainActivity) {
                ((MainActivity) activity).shutdown();
            }
        }
    }
}