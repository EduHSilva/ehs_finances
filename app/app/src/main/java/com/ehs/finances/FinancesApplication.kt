package com.ehs.finances

import android.app.Application
import com.ehs.finances.di.AppContainer

class FinancesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
