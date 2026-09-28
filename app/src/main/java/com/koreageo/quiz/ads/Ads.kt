package com.koreageo.quiz.ads

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.koreageo.quiz.BuildConfig
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object Ads {
    private var appContext: Context? = null
    private var interstitial: InterstitialAd? = null
    private var loading = false

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        MobileAds.initialize(context.applicationContext) { loadInterstitial() }
    }

    private fun loadInterstitial() {
        val context = appContext ?: return
        if (interstitial != null || loading) return
        loading = true
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    loading = false
                }
            },
        )
    }

    /** 전면 광고를 띄우고 닫힐 때까지 기다린다. 준비된 광고가 없으면 기다리지 않고 바로 돌아온다. */
    suspend fun showInterstitial(activity: Activity) {
        val ad = interstitial
        if (ad == null) {
            loadInterstitial()
            return
        }
        interstitial = null
        suspendCancellableCoroutine { cont ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadInterstitial()
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    loadInterstitial()
                    if (cont.isActive) cont.resume(Unit)
                }
            }
            ad.show(activity)
        }
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt()
        val adSize = remember(widthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        val adView = remember(adSize) {
            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }
        AndroidView(
            factory = { adView },
            modifier = Modifier
                .fillMaxWidth()
                .height(adSize.height.dp),
        )
    }
}
