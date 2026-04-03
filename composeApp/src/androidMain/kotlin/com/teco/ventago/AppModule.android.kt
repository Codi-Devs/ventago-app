package com.teco.ventago

import com.teco.ventago.core.AndroidPdfSharer
import com.teco.ventago.core.FingerPrintService
import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.PdfSharer
import com.teco.ventago.core.SecureStorage
import com.teco.ventago.core.cache.getDatabaseBuilder
import com.teco.ventago.core.cache.room.CacheDatabase
import com.teco.ventago.core.cache.room.getCacheDatabase
import com.teco.ventago.features.printers.domain.AndroidEpsonPrinterDiscoveryEngine
import com.teco.ventago.features.printers.domain.AndroidEpsonPrinterEngine
import com.teco.ventago.features.printers.domain.PrinterDiscoveryEngine
import com.teco.ventago.features.printers.domain.PrinterEngine
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module


actual fun httpClient(config: HttpClientConfig<*>.() -> Unit) = HttpClient(CIO) {
//    BuildConfig.BUILD_TYPE
    config(this)
    engine {
        maxConnectionsCount = 1000
        endpoint {
            maxConnectionsPerRoute = 100
            pipelineMaxSize = 20
            keepAliveTime = 5000
            connectTimeout = 5000
            connectAttempts = 5
        }
    }
}

actual val platformModule: Module = module {
    single { LocalStorage(get()) }
    single { SecureStorage(get(), "login") }
    single { FingerPrintService(get()) }
    single<CacheDatabase> {
        val builder = getDatabaseBuilder(get())
        getCacheDatabase(builder)
    }
    single(named("AppScope")) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
    single<PdfSharer> { AndroidPdfSharer(androidContext()) }
    single<PrinterEngine> { AndroidEpsonPrinterEngine(androidContext(), get()) }
    single<PrinterDiscoveryEngine> { AndroidEpsonPrinterDiscoveryEngine(androidContext()) }
}
