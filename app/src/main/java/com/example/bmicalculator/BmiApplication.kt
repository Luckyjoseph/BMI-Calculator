package com.example.bmicalculator

import android.app.Application
import com.google.android.material.color.DynamicColors

class BmiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
