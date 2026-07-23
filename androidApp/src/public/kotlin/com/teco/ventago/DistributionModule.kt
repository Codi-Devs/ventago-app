package com.teco.ventago

import com.teco.ventago.features.printers.domain.AndroidEpsonPrinterDiscoveryEngine
import com.teco.ventago.features.printers.domain.AndroidEpsonPrinterEngine
import com.teco.ventago.features.printers.domain.PrinterDiscoveryEngine
import com.teco.ventago.features.printers.domain.PrinterEngine
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidDistributionModules(): List<Module> = listOf(
    module {
        single { AppDistribution(isPosBuild = false) }
        single<PrinterEngine> { AndroidEpsonPrinterEngine(androidContext(), get()) }
        single<PrinterDiscoveryEngine> { AndroidEpsonPrinterDiscoveryEngine(androidContext()) }
    }
)
