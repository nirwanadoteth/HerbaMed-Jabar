package edu.unikom.herbamedjabar

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.DebugLogger
import com.cloudinary.android.MediaManager
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp
import edu.unikom.herbamedjabar.migration.ScanHistoryMigrationManager
import javax.inject.Inject

@HiltAndroidApp
class HerbaAppApplication : Application(), ImageLoaderFactory {
    @Inject lateinit var scanHistoryMigrationManager: ScanHistoryMigrationManager

    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(context = this)

        if (BuildConfig.DEBUG) {
            Firebase.appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        }
        val config = mapOf("cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME)
        MediaManager.init(this, config)

        scanHistoryMigrationManager.runMigrationIfNeeded(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .respectCacheHeaders(true)
            .allowHardware(false)
            .apply { if (BuildConfig.DEBUG) logger(DebugLogger()) }
            .build()
    }
}
