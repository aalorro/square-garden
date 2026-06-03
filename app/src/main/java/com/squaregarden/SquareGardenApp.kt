package com.squaregarden

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class SquareGardenApp : Application() {
    companion object {
        const val DB_URL = "https://square-garden-217f9-default-rtdb.firebaseio.com"
    }
}
