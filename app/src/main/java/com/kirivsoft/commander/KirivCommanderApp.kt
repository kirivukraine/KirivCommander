package com.kirivsoft.commander

import android.app.Application
import com.topjohnwu.superuser.Shell

class KirivCommanderApp : Application() {

    companion object {
        lateinit var instance: KirivCommanderApp
            private set
    }

    init {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(15)
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
