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

package com.frostwire.android.gui.util;

import android.app.Activity;
import android.content.Context;
import com.andrew.apollo.utils.MusicUtils;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.activities.MainActivity;
import com.frostwire.logging.Logger;
import com.frostwire.util.Ref;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;
import com.inmobi.monetization.IMErrorCode;
import com.inmobi.monetization.IMInterstitial;
import com.inmobi.monetization.IMInterstitialListener;
import com.ironsource.mobilcore.CallbackResponse;
import com.ironsource.mobilcore.MobileCore;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OfferUtils {

    private static final Logger LOG = Logger.getLogger(OfferUtils.class);
    public static boolean MOBILE_CORE_NATIVE_ADS_READY = false;

    /**
     * True if user has enabled support for frostwire, Appia is enabled and it's not an Amazon distribution build.
     *
     * @return
     */
    public static boolean isfreeAppsEnabled() {
        ConfigurationManager config = null;
        boolean isFreeAppsEnabled = false;
        try {
            config = ConfigurationManager.instance();
            isFreeAppsEnabled = (config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) && config.getBoolean(Constants.PREF_KEY_GUI_USE_MOBILE_CORE)) && Constants.IS_GOOGLE_PLAY_DISTRIBUTION;
        } catch (Throwable t) {
        }
        return isFreeAppsEnabled;
    }

    public static boolean isMobileCoreEnabled() {
        ConfigurationManager config = null;
        boolean isMobileCoreEnabled = false;
        try {
            config = ConfigurationManager.instance();
            isMobileCoreEnabled = (config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) && config.getBoolean(Constants.PREF_KEY_GUI_USE_MOBILE_CORE));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return isMobileCoreEnabled;
    }

    /**
     * If mobileCore is active, it will show the interstitial, then perform the callback.
     * Otherwise, it will perform the callback logic.
     *
     * @param callerActivity
     * @param mobileCoreStarted
     * @param callbackResponse
     */
    public static boolean showMobileCoreInterstitial(Activity callerActivity, boolean mobileCoreStarted, CallbackResponse callbackResponse) {
        if (isMobileCoreEnabled() && mobileCoreStarted && MobileCore.isInterstitialReady()) {
            try {
                MobileCore.showInterstitial(callerActivity, callbackResponse);
                UXStats.instance().log(UXAction.MISC_INTERSTITIAL_SHOW);
                return true;
            } catch (Throwable e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    public static void onFreeAppsClick(Context context) {
        if (isfreeAppsEnabled() && isMobileCoreEnabled() && MobileCore.isDirectToMarketReady()) {
            try {
                LOG.debug("onFreeAppsClick");
                MobileCore.directToMarket((Activity) context);
            } catch (Throwable t) {
                LOG.error("can't show app wall", t);
                t.printStackTrace();
            }
        }
    }

    public static boolean isInMobiEnabled() {
        ConfigurationManager config = null;
        boolean isInMobiEnabled = false;
        try {
            config = ConfigurationManager.instance();
            isInMobiEnabled = (config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) && config.getBoolean(Constants.PREF_KEY_GUI_USE_INMOBI));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return isInMobiEnabled;
    }

    public static boolean showInMobiInterstitial(boolean inmobiStarted, final IMInterstitial imInterstitial, final InMobiListener imListener) {
        if (!inmobiStarted || !isInMobiEnabled() || imInterstitial == null) {
            return false;
        }

        if (imInterstitial.getState().equals(IMInterstitial.State.READY)) {
            try {
                imInterstitial.show();
                return true;
            } catch (Throwable e) {
                LOG.error("InMobi Interstitial failed on .show()!", e);
                return false;
            }
        }
        return false;
    }

    public static class InMobiListener implements IMInterstitialListener {
        private Logger LOG = Logger.getLogger(IMInterstitialListener.class);
        private WeakReference<Activity> activityRef;
        public boolean shutdownAfterDismiss = false;
        public boolean finishAfterDismiss = false;

        public InMobiListener(Activity hostActivity, boolean shutdownAfterDismiss, boolean finishAfterDismiss) {
            activityRef = new WeakReference<Activity>(hostActivity);
            this.shutdownAfterDismiss = shutdownAfterDismiss;
            this.finishAfterDismiss = finishAfterDismiss;
        }

        @Override
        public void onDismissInterstitialScreen(IMInterstitial imInterstitial) {
            Activity callerActivity = Ref.alive(activityRef) ? activityRef.get() : null;

            if (shutdownAfterDismiss) {
                // Finish through MainActivity caller
                if (callerActivity != null && callerActivity instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) callerActivity;
                    mainActivity.shutdown();
                }
            }

            if (finishAfterDismiss) {
                try {
                    if (callerActivity != null) {
                        callerActivity.finish();
                    }
                } catch (Throwable e) {
                    // meh, activity was a goner already, shutdown was true most likely.
                }
            }

            imInterstitial.loadInterstitial();
        }

        @Override
        public void onInterstitialFailed(IMInterstitial imInterstitial, IMErrorCode imErrorCode) {
            LOG.error(imErrorCode.name());
            LOG.error(imErrorCode.toString());

            new Thread("OfferUtils.onInterstitialFailed") {
                @Override
                public void run() {
                    try {
                        TimeUnit.MINUTES.sleep(30);
                        if (Ref.alive(activityRef)) {
                            Activity activity = activityRef.get();
                            if (activity instanceof MainActivity) {
                                MainActivity mainActivity = (MainActivity) activity;
                                mainActivity.loadNewInmobiInterstitial();
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }

        @Override
        public void onInterstitialLoaded(IMInterstitial imInterstitial) {}
        @Override
        public void onShowInterstitialScreen(IMInterstitial imInterstitial) {}
        @Override
        public void onInterstitialInteraction(IMInterstitial imInterstitial, Map<String, String> map) {}
        @Override
        public void onLeaveApplication(IMInterstitial imInterstitial) {}
    }

    public static boolean isAppLovinEnabled() {
        ConfigurationManager config = null;
        boolean isAppLovinEnabled = false;
        try {
            config = ConfigurationManager.instance();
            isAppLovinEnabled = (config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) && config.getBoolean(Constants.PREF_KEY_GUI_USE_APPLOVIN));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return isAppLovinEnabled;
    }

    public static boolean showAppLovinInterstitial(boolean applovinStarted,
                                                   FWAppLovinInterstitialAdapter adDialog) {
        if (isAppLovinEnabled() && applovinStarted) {
            try {
                if (adDialog.isVideoAd && MusicUtils.isPlaying()) {
                    return false;
                }
                if (!adDialog.isAdReadyToDisplay()) {
                    return false;
                }
                adDialog.show();
                return true;
            } catch (Throwable e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    public static class FWAppLovinInterstitialAdapter implements AppLovinAdDisplayListener, AppLovinAdLoadListener {
        private static final Logger LOG = Logger.getLogger(FWAppLovinInterstitialAdapter.class);
        private final WeakReference<Activity> activityRef;
        private AppLovinInterstitialAdDialog ad;

        private boolean dismissAfter = false;
        private boolean shutdownAfter = false;
        private boolean isVideoAd = false;

        public FWAppLovinInterstitialAdapter(Activity parentActivity) {
            this.activityRef = Ref.weak(parentActivity);
        }

        public Activity getActivity() {
            if (!Ref.alive(activityRef)) {
                return null;
            }
            return activityRef.get();
        }

        public void dismissActivityAfterwards(boolean dismiss) {
            dismissAfter = dismiss;
        }

        public void shutdownAppAfter(boolean shutdown) {
            shutdownAfter = shutdown;
        }

        @Override
        public void adDisplayed(AppLovinAd appLovinAd) {
        }

        @Override
        public void adHidden(AppLovinAd appLovinAd) {
            if (Ref.alive(activityRef)) {
                Activity callerActivity = activityRef.get();

                if (dismissAfter) {
                    callerActivity.finish();
                }
                if (shutdownAfter) {
                    if (callerActivity instanceof MainActivity) {
                        ((MainActivity) callerActivity).shutdown();
                    }
                }
            }
        }

        @Override
        public void adReceived(AppLovinAd appLovinAd) {
            if (appLovinAd != null) {
                if (appLovinAd instanceof AppLovinInterstitialAdDialog) {
                    ad = (AppLovinInterstitialAdDialog) appLovinAd;
                    ad.setAdDisplayListener(this);
                    ad.setAdLoadListener(this);
                    isVideoAd = appLovinAd.isVideoAd();
                }
            }
        }

        @Override
        public void failedToReceiveAd(int i) {
            LOG.warn("failed to receive ad ("+ i +")");
        }

        public boolean isAdReadyToDisplay() {
            return ad != null && ad.isAdReadyToDisplay();
        }

        public void show() {
            if (ad!=null) {
                ad.show();
            }
        }
    }
}
