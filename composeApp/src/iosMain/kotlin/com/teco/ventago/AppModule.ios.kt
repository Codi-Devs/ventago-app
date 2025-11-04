package com.teco.ventago

import com.teco.ventago.core.IosPdfSharer
import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.PdfSharer
import com.teco.ventago.core.SecureStorage
import com.teco.ventago.core.cache.getDatabaseBuilder
import com.teco.ventago.core.cache.room.CacheDatabase
import com.teco.ventago.core.cache.room.getCacheDatabase
import dev.gitlive.firebase.auth.FirebaseAuth
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual fun httpClient(config: HttpClientConfig<*>.() -> Unit) = HttpClient(Darwin) {
    config(this)
    engine {
        configureRequest {
            setAllowsCellularAccess(true)
        }
    }
}

actual val platformModule: Module = module {
    single { LocalStorage() }
    single { SecureStorage() }
    single<CacheDatabase> {
        val builder = getDatabaseBuilder()
        getCacheDatabase(builder)
    }
    single(named("AppScope")) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
    single<PdfSharer> { IosPdfSharer() }
}

fun test() {

}