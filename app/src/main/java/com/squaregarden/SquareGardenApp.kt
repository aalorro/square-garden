package com.squaregarden

import android.app.Application
import com.google.android.gms.games.PlayGamesSdk

class SquareGardenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PlayGamesSdk.initialize(this)
    }
}
