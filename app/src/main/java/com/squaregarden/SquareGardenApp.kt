package com.squaregarden

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class SquareGardenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}
