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
import android.content.Intent;
import com.andrew.apollo.utils.MusicUtils;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.*;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.activities.MainActivity;
import com.frostwire.logging.Logger;
import com.frostwire.util.Ref;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;
import com.inmobi.commons.InMobi;
import com.inmobi.monetization.IMErrorCode;
import com.inmobi.monetization.IMInterstitial;
import com.inmobi.monetization.IMInterstitialListener;
import com.ironsource.mobilcore.AdUnitEventListener;
import com.ironsource.mobilcore.CallbackResponse;
import com.ironsource.mobilcore.MobileCore;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OfferUtils {

    private static final Logger LOG = Logger.getLogger(OfferUtils.class);
    public static boolean MOBILE_CORE_NATIVE_ADS_READY = false;

    private static boolean mobileCoreStarted = false;
    private static boolean appLovinStarted = false;
    private static boolean inmobiStarted = false;

    private static IMInterstitial inmobiInterstitial = null;
    private static OfferUtils.InMobiListener inmobiListener = null;
    private static OfferUtils.FWAppLovinInterstitialAdapter appLovinInterstitialAdapter = null;

    public static void initAffiliatesAsync(Activity activity) {
        initializeMobileCore(activity); // this one needs UI thread initialization.
        initializeAppLovin(activity);
        initializeInMobi(activity);
    }

    public static void stopAffiliateServices(Context context) {
        // Stop MobileCore if you have to.
        if (mobileCoreStarted) {
            try {
                context.stopService(new Intent(context.getApplicationContext(),
                        com.ironsource.mobilcore.MobileCoreReport.class));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        // TODO: See if other SDKs leave any service running and stop them the same way if possible.
    }

    /**
     * True if user has enabled support for frostwire, Appia is enabled and it's not an Amazon distribution build.
     *
     * @return
     */
    public static boolean isfreeAppsEnabled() {
        ConfigurationManager config;
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
        ConfigurationManager config;
        boolean isInMobiEnabled = false;
        try {
            config = ConfigurationManager.instance();
            isInMobiEnabled = (config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) && config.getBoolean(Constants.PREF_KEY_GUI_USE_INMOBI));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return isInMobiEnabled;
    }

    public static void loadNewInmobiInterstitial(final Activity activity) {
        if (!inmobiStarted) {
            return; //not ready
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    inmobiInterstitial = new IMInterstitial(activity, Constants.INMOBI_INTERSTITIAL_PROPERTY_ID);
                    // in case it fails loading, it will try again every minute once.
                    inmobiListener = new OfferUtils.InMobiListener(activity, false, false);
                    inmobiInterstitial.setIMInterstitialListener(inmobiListener);
                    inmobiInterstitial.loadInterstitial();
                } catch (Throwable t) {
                    // don't crash, keep going.
                    // possible android.util.AndroidRuntimeException: android.content.pm.PackageManager$NameNotFoundException: com.google.android.webview
                }
            }
        });
    }

    public static boolean showInMobiInterstitial(boolean inmobiStarted, final IMInterstitial imInterstitial, final InMobiListener imListener) {
        if (!inmobiStarted || !isInMobiEnabled() || imInterstitial == null) {
            return false;
        }

        imInterstitial.setIMInterstitialListener(imListener);

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
                                loadNewInmobiInterstitial(activity);
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

                return adDialog.show();
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
        private AppLovinAd ad;

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
                isVideoAd = appLovinAd.isVideoAd();
            }
        }

        @Override
        public void failedToReceiveAd(int i) {
            LOG.warn("failed to receive ad ("+ i +")");
        }

        public boolean isAdReadyToDisplay() {
            return ad != null && Ref.alive(activityRef) && AppLovinInterstitialAd.isAdReadyToDisplay(activityRef.get());
        }

        public boolean show() {
            boolean result = false;
            if (ad!=null && Ref.alive(activityRef)) {
                try {
                    final AppLovinInterstitialAdDialog adDialog = AppLovinInterstitialAd.create(AppLovinSdk.getInstance(activityRef.get()), activityRef.get());
                    adDialog.showAndRender(ad);
                    result = true;
                } catch (Throwable t) {
                    result = false;
                }
            }
            return result;
        }
    }

    private static void initializeMobileCore(Activity activity) {
        if (!mobileCoreStarted && OfferUtils.isMobileCoreEnabled()) {
            try {
                MobileCore.init(activity, Constants.MOBILE_CORE_DEVHASH, MobileCore.LOG_TYPE.DEBUG, MobileCore.AD_UNITS.INTERSTITIAL, MobileCore.AD_UNITS.DIRECT_TO_MARKET);
                MobileCore.setNativeAdsBannerSupport(true);
                MobileCore.setAdUnitEventListener(new AdUnitEventListener() {
                    @Override
                    public void onAdUnitEvent(MobileCore.AD_UNITS ad_units, EVENT_TYPE event_type) {
                        if (event_type.equals(EVENT_TYPE.AD_UNIT_READY) && ad_units.equals(MobileCore.AD_UNITS.NATIVE_ADS)) {
                            OfferUtils.MOBILE_CORE_NATIVE_ADS_READY = true;

                        }
                    }
                });
                mobileCoreStarted = true;
            } catch (Throwable e) {
                e.printStackTrace();
                mobileCoreStarted = false;
            }
        } else if (mobileCoreStarted && OfferUtils.isMobileCoreEnabled()) {
            try {
                MobileCore.refreshOffers();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }


    private static void initializeAppLovin(final Activity activity) {
        if (!OfferUtils.isAppLovinEnabled()) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    if (!appLovinStarted) {
                        AppLovinSdk.initializeSdk(activity.getApplicationContext());
                        appLovinInterstitialAdapter = new OfferUtils.FWAppLovinInterstitialAdapter(activity);
                        AppLovinSdk.getInstance(activity).getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, appLovinInterstitialAdapter);
                        appLovinStarted = true;
                    }
                } catch (Throwable e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }.start();
    }

    private static void initializeInMobi(final Activity activity) {
        if (!OfferUtils.isInMobiEnabled()) {
            return;
        }

        if (!inmobiStarted) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        // this initialize call is very expensive, this is why we should be invoked in a thread.
                        //LOG.info("InMobi.initialize()...");
                        InMobi.initialize(activity, Constants.INMOBI_INTERSTITIAL_PROPERTY_ID);
                        //InMobi.setLogLevel(InMobi.LOG_LEVEL.DEBUG);
                        //LOG.info("InMobi.initialized.");
                        inmobiStarted = true;
                        //LOG.info("Load InmobiInterstitial.");
                        loadNewInmobiInterstitial(activity);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        inmobiStarted = false;
                    }
                }
            }.start();
        }
    }

    public static void showInterstitial(final Activity activity,
                                        final boolean shutdownAfterwards,
                                        final boolean dismissAfterwards) {
        boolean interstitialShown = false;

        if (mobileCoreStarted) {
            interstitialShown = showMobileCoreInsterstitial(activity, shutdownAfterwards, dismissAfterwards);
        }

        if (!interstitialShown && appLovinStarted && appLovinInterstitialAdapter != null) {
            appLovinInterstitialAdapter.shutdownAppAfter(shutdownAfterwards);
            appLovinInterstitialAdapter.dismissActivityAfterwards(dismissAfterwards);
            interstitialShown = OfferUtils.showAppLovinInterstitial(appLovinStarted, appLovinInterstitialAdapter);
        }

        if (!interstitialShown && inmobiStarted) {
            if (inmobiListener != null) {
                inmobiListener.finishAfterDismiss = dismissAfterwards;
                inmobiListener.shutdownAfterDismiss = shutdownAfterwards;
                interstitialShown = OfferUtils.showInMobiInterstitial(inmobiStarted,
                        inmobiInterstitial,
                        inmobiListener);
            }
        }

        // If interstitial's callbacks were not invoked because ads weren't displayed
        // then we're responsible for finish()'ing the activity or shutting down the app.
        if (!interstitialShown) {
            if (dismissAfterwards) {
                activity.finish();
            }
            if (shutdownAfterwards && activity instanceof MainActivity) {
                ((MainActivity) activity).shutdown();
            }
        }
    }

    private static boolean showMobileCoreInsterstitial(final Activity activity,
                                                final boolean shutdownAfterwards,
                                                final boolean dismissAfterwards) {
        return OfferUtils.showMobileCoreInterstitial(activity, true, new CallbackResponse() {
            @Override
            public void onConfirmation(TYPE type) {
                if (dismissAfterwards) {
                    activity.finish();
                }
                if (shutdownAfterwards && activity instanceof MainActivity) {
                    ((MainActivity) activity).shutdown();
                }
            }
        });
    }
}
