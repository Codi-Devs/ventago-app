package com.teco.ventago

import com.teco.ventago.features.printers.domain.PrinterDiscoveryEngine
import com.teco.ventago.features.printers.domain.PrinterEngine
import com.teco.ventago.printer.h10p.H10pPrinterDiscoveryEngine
import com.teco.ventago.printer.h10p.H10pPrinterEngine
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidDistributionModules(): List<Module> = listOf(
    module {
        single { AppDistribution(isPosBuild = true) }
        single<PrinterEngine> { H10pPrinterEngine(androidContext(), get()) }
        single<PrinterDiscoveryEngine> { H10pPrinterDiscoveryEngine() }
    }
)
