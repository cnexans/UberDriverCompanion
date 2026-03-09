package com.carlos.uberanalyzer

import android.app.Application
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

class UberAnalyzerApp : Application() {

    companion object {
        private const val POSTHOG_API_KEY = "phc_V86sv23tZmpoIKoMxp8E9nd3w3BxMM5vw5GYtZlOXrY"
        private const val POSTHOG_HOST = "https://us.i.posthog.com"
    }

    override fun onCreate() {
        super.onCreate()

        val config = PostHogAndroidConfig(
            apiKey = POSTHOG_API_KEY,
            host = POSTHOG_HOST
        )

        PostHogAndroid.setup(this, config)
    }
}
