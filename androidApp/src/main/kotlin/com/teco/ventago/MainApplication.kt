package com.teco.ventago

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.imagekit.android.ImageKit
import com.imagekit.android.entity.TransformationPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.logger.AndroidLogger
import org.koin.dsl.bind
import org.koin.dsl.module

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        FirebaseApp.initializeApp(this@MainApplication)
        initKoinAndroid(
            AndroidLogger(),
            listOf(
                module {
                    single<Context> { this@MainApplication } bind Context::class
                    single<Application> { this@MainApplication }
                    single<ComponentActivity> { get<MainActivityHolder>().activity }
                    single { MainActivityHolder() }
                },
            ) + androidDistributionModules(),
        )
        CoroutineScope(Dispatchers.IO).launch {
            ImageKit.init(
                applicationContext,
                getString(R.string.imagekit_public_key),
                getString(R.string.imagekit_end_point),
                TransformationPosition.PATH,
                )
        }
    }
}
