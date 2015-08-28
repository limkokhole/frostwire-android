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
import com.andrew.apollo.utils.MusicUtils;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.logging.Logger;

public class AppLovinAffiliate implements Affiliate {
    private static final Logger LOG = Logger.getLogger(AppLovinAffiliate.class);
    private AppLovinInterstitialAdapter interstitialAdapter = null;
    private boolean started = false;

    public AppLovinAffiliate() {}

    public void initialize(final Activity activity) {
        if (!enabled()) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    if (!started) {
                        AppLovinSdk.initializeSdk(activity.getApplicationContext());
                        loadNewInterstitial(activity);
                        started = true;
                    }
                } catch (Throwable e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }.start();
    }

    public void loadNewInterstitial(Activity activity) {
        interstitialAdapter = new AppLovinInterstitialAdapter(activity, this);
        AppLovinSdk.getInstance(activity).getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, interstitialAdapter);
    }

    public boolean started() {
        return started;
    }

    public boolean enabled() {
        ConfigurationManager config;
        boolean enabled = false;
        try {
            config = ConfigurationManager.instance();
            enabled = (config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) && config.getBoolean(Constants.PREF_KEY_GUI_USE_APPLOVIN));
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
        return enabled;
    }

    public boolean showInterstitial(final boolean shutdownAfterwards,
                                    final boolean dismissAfterward) {
        if (enabled() && started) {
            interstitialAdapter.shutdownAppAfter(shutdownAfterwards);
            interstitialAdapter.dismissActivityAfterwards(dismissAfterward);
            try {
                if (interstitialAdapter.isVideoAd() && MusicUtils.isPlaying()) {
                    return false;
                }
                return interstitialAdapter.isAdReadyToDisplay() && interstitialAdapter.show();
            } catch (Throwable e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }
}