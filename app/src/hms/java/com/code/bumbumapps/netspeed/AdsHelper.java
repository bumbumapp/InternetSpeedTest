package com.code.bumbumapps.netspeed;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.InterstitialAd;
import com.huawei.hms.ads.NonPersonalizedAd;
import com.huawei.hms.ads.RequestOptions;
import com.huawei.hms.ads.banner.BannerView;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Huawei AppGallery build: same API and interstitial cap as the Play build, but served by Huawei Ads
 * (Petal) instead of AdMob. Only non-personalized ads are requested, so no consent form is needed.
 * The AdMob unit IDs passed by callers are ignored; the Huawei slot IDs come from resources.
 */
public final class AdsHelper {

    private static final String TAG = "AdsHelper";

    /** Minimum time between two interstitials (and between app start and the first one). */
    public static final long INTERSTITIAL_MIN_INTERVAL_MS = 10 * 60_000L; // after a finished test; also the delay after app start

    private static final AtomicBoolean sdkStarted = new AtomicBoolean(false);
    private static volatile boolean sdkReady;
    private static InterstitialAd interstitialAd;
    private static long lastInterstitialAt = -1L;

    public interface OnAdsReady {
        void onAdsReady();
    }

    private AdsHelper() {
    }

    /** Initialises Huawei Ads (non-personalized) and calls {@code onReady} once. */
    public static void gatherConsent(@NonNull Activity activity, @NonNull OnAdsReady onReady) {
        if (lastInterstitialAt < 0) {
            lastInterstitialAt = SystemClock.elapsedRealtime();
        }
        if (activity.isFinishing()) {
            return;
        }
        if (sdkStarted.compareAndSet(false, true)) {
            try {
                HwAds.init(activity.getApplicationContext());
                RequestOptions options = HwAds.getRequestOptions().toBuilder()
                        .setNonPersonalizedAd(NonPersonalizedAd.ALLOW_NON_PERSONALIZED)
                        .build();
                HwAds.setRequestOptions(options);
                sdkReady = true;
            } catch (RuntimeException e) {
                Log.w(TAG, "Huawei Ads init failed", e);
            }
        }
        if (sdkReady) {
            onReady.onAdsReady();
        }
    }

    public static boolean canRequestAds(@NonNull Context context) {
        return sdkReady;
    }

    public static boolean isPrivacyOptionsRequired(@NonNull Context context) {
        return false;
    }

    public static void showPrivacyOptions(@NonNull Activity activity) {
    }

    private static String slot(Context context, int real, int test) {
        return context.getString(BuildConfig.DEBUG ? test : real);
    }

    /** Adds a banner to {@code container}; returns null if ads are not allowed. */
    @Nullable
    public static BannerView loadBanner(@NonNull Activity activity, @NonNull ViewGroup container, @NonNull String adUnitId) {
        if (!canRequestAds(activity)) {
            return null;
        }
        BannerView banner = new BannerView(activity);
        banner.setAdId(slot(activity, R.string.hms_banner_id, R.string.hms_test_banner_id));
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int widthDp = (int) (metrics.widthPixels / metrics.density);
        banner.setBannerAdSize(BannerAdSize.getCurrentDirectionBannerSize(activity, widthDp));
        container.removeAllViews();
        container.addView(banner);
        banner.loadAd(new AdParam.Builder().build());
        return banner;
    }

    /** Loads one interstitial in the background if none is cached or loading. */
    public static void preloadInterstitial(@NonNull Context context, @NonNull String adUnitId) {
        if (interstitialAd != null || !canRequestAds(context)) {
            return;
        }
        final InterstitialAd ad = new InterstitialAd(context.getApplicationContext());
        ad.setAdId(slot(context, R.string.hms_interstitial_id, R.string.hms_test_interstitial_id));
        ad.setAdListener(new AdListener() {
            @Override
            public void onAdFailed(int code) {
                if (interstitialAd == ad) {
                    interstitialAd = null;
                }
            }
        });
        interstitialAd = ad;
        ad.loadAd(new AdParam.Builder().build());
    }

    public static boolean showInterstitialIfReady(@NonNull Activity activity, @NonNull String adUnitId) {
        return showInterstitialIfReady(activity, adUnitId, null);
    }

    public static boolean showInterstitialIfReady(@NonNull Activity activity, @NonNull String adUnitId,
                                                  @Nullable Runnable afterAd) {
        boolean shown = showInterstitialInternal(activity, adUnitId, afterAd);
        if (!shown && afterAd != null) {
            afterAd.run();
        }
        return shown;
    }

    private static boolean showInterstitialInternal(Activity activity, String adUnitId, @Nullable Runnable afterAd) {
        long now = SystemClock.elapsedRealtime();
        InterstitialAd ad = interstitialAd;
        if (ad == null) {
            preloadInterstitial(activity, adUnitId);
            return false;
        }
        if (!ad.isLoaded() || !isIntervalElapsed(lastInterstitialAt, now, INTERSTITIAL_MIN_INTERVAL_MS)
                || activity.isFinishing()) {
            return false;
        }
        final Context app = activity.getApplicationContext();
        interstitialAd = null;
        // Huawei only reports load failures via onAdFailed: if show() never opens the ad, do not wait forever.
        final boolean[] opened = {false};
        final boolean[] done = {false};
        final Runnable finish = () -> {
            if (done[0]) {
                return;
            }
            done[0] = true;
            preloadInterstitial(app, adUnitId);
            if (afterAd != null) {
                afterAd.run();
            }
        };
        ad.setAdListener(new AdListener() {
            @Override
            public void onAdOpened() {
                opened[0] = true;
            }

            @Override
            public void onAdClosed() {
                finish.run();
            }

            @Override
            public void onAdFailed(int code) {
                finish.run();
            }
        });
        lastInterstitialAt = now;
        ad.show(activity);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!opened[0]) {
                finish.run();
            }
        }, 3000L);
        return true;
    }

    /** Frequency cap: true when at least {@code minIntervalMs} passed since {@code lastAt} (negative = never). */
    static boolean isIntervalElapsed(long lastAt, long now, long minIntervalMs) {
        return lastAt < 0 || now - lastAt >= minIntervalMs;
    }
}
