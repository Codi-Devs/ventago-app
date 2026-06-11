package com.teco.ventago

import com.teco.ventago.core.SnackbarService
import com.teco.ventago.core.beta.BetaProvider
import com.teco.ventago.core.beta.BetaRepository
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.cache.RoomCache
import com.teco.ventago.core.changes.ChangesManager
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.core.flags.FlagsService
import com.teco.ventago.core.flags.IFlagsService
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.LoggerService
import com.teco.ventago.core.logger.printLog
import com.teco.ventago.core.FingerPrintService
import com.teco.ventago.core.network.FINGERPRINT_HEADER_NAME
import com.teco.ventago.core.session.ISessionIdService
import com.teco.ventago.core.session.SecureStorageSessionIdStore
import com.teco.ventago.core.session.SessionIdBackendUrlMatcher
import com.teco.ventago.core.session.SessionIdConstants
import com.teco.ventago.core.session.SessionIdService
import com.teco.ventago.core.session.SessionIdStore
import com.teco.ventago.features.auth.data.provider.AuthProvider
import com.teco.ventago.features.auth.data.provider.IAuthProvider
import com.teco.ventago.features.auth.data.repository.AuthRepository
import com.teco.ventago.features.auth.data.repository.IAuthRepository
import com.teco.ventago.features.auth.domain.AuthService
import com.teco.ventago.features.auth.domain.FirebaseService
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.auth.domain.IFirebaseService
import com.teco.ventago.features.auth.ui.login.viewmodel.LoginViewModel
import com.teco.ventago.features.auth.ui.register.business.viewmodel.BusinessRegisterViewModel
import com.teco.ventago.features.auth.ui.register.user.viewmodel.RegisterViewModel
import com.teco.ventago.features.branches.data.provider.BranchProvider
import com.teco.ventago.features.branches.data.repository.BranchRepository
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.branches.ui.billing_point.add.viewmodel.AddBillingPointViewModel
import com.teco.ventago.features.branches.ui.billing_point.edit.viewmodel.EditBillingPointViewModel
import com.teco.ventago.features.branches.ui.billing_point.manage.viewmodel.BillingPointsManageViewModel
import com.teco.ventago.features.branches.ui.branches.manage.viewmodel.BranchesManageViewModel
import com.teco.ventago.features.business.data.provider.BusinessProvider
import com.teco.ventago.features.business.data.repository.BusinessRepository
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.customers.data.provider.CustomerProvider
import com.teco.ventago.features.customers.data.repository.CustomerRepository
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.customers.ui.details.viewmodel.CustomerDetailsViewModel
import com.teco.ventago.features.customers.ui.form.viewmodel.CustomerFormViewModel
import com.teco.ventago.features.customers.ui.list.viewmodel.CustomersListViewModel
import com.teco.ventago.features.financialProfile.data.provider.FinancialProfileProvider
import com.teco.ventago.features.financialProfile.data.repository.FinancialProfileRepository
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.home.data.provider.HomeSummaryProvider
import com.teco.ventago.features.home.data.provider.IHomeSummaryProvider
import com.teco.ventago.features.home.data.repository.HomeSummaryRepository
import com.teco.ventago.features.home.data.repository.IHomeSummaryRepository
import com.teco.ventago.features.home.domain.HomeSummaryService
import com.teco.ventago.features.home.ui.viewmodel.HomeViewModel
import com.teco.ventago.features.invoicing.data.provider.IInvoicingSettingsProvider
import com.teco.ventago.features.invoicing.data.provider.InvoicingSettingsProvider
import com.teco.ventago.features.invoicing.data.repository.IInvoicingSettingsRepository
import com.teco.ventago.features.invoicing.data.repository.InvoicingSettingsRepository
import com.teco.ventago.features.invoicing.domain.InvoicingSettingsService
import com.teco.ventago.features.invoicing.domain.InvoicingSettingsStore
import com.teco.ventago.features.invoicing.domain.LocalStorageInvoicingSettingsStore
import com.teco.ventago.features.orders.data.provider.OrdersProvider
import com.teco.ventago.features.orders.data.repository.OrdersRepository
import com.teco.ventago.features.orders.domain.OrderService
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrdersDetailsViewModel
import com.teco.ventago.features.orders.ui.order_history.viewModel.OrderHistoryViewModel
import com.teco.ventago.features.orders.ui.order_invoice.viewModel.OrderInvoiceViewModel
import com.teco.ventago.features.orders.ui.orders.viewmodel.OrdersViewModel
import com.teco.ventago.features.notifications.data.provider.INotificationsProvider
import com.teco.ventago.features.notifications.data.provider.NotificationsProvider
import com.teco.ventago.features.notifications.data.repository.INotificationsRepository
import com.teco.ventago.features.notifications.data.repository.NotificationsRepository
import com.teco.ventago.features.notifications.domain.INotificationsService
import com.teco.ventago.features.notifications.domain.NotificationsService
import com.teco.ventago.features.notifications.ui.viewmodel.NotificationsViewModel
import com.teco.ventago.features.printers.data.provider.IPrinterProvider
import com.teco.ventago.features.printers.data.provider.PrinterProvider
import com.teco.ventago.features.printers.data.repository.IPrinterRepository
import com.teco.ventago.features.printers.data.repository.PrinterRepository
import com.teco.ventago.features.printers.domain.PrinterCacheSyncService
import com.teco.ventago.features.printers.domain.PrinterDiscoveryEngine
import com.teco.ventago.features.printers.domain.PrinterDiscoveryService
import com.teco.ventago.features.printers.domain.PrinterEngine
import com.teco.ventago.features.printers.domain.PrinterService
import com.teco.ventago.features.printers.ui.viewmodel.PrinterOnboardingViewModel
import com.teco.ventago.features.printers.ui.viewmodel.PrintersViewModel
import com.teco.ventago.features.reports.data.provider.IRealTimeReportsProvider
import com.teco.ventago.features.reports.data.provider.RealTimeReportsProvider
import com.teco.ventago.features.reports.data.repository.IRealTimeReportsRepository
import com.teco.ventago.features.reports.data.repository.RealTimeReportsRepository
import com.teco.ventago.features.reports.domain.RealTimeReportsService
import com.teco.ventago.features.reports.ui.viewmodel.ReportDefinitionViewModel
import com.teco.ventago.features.payments.data.provider.PaymentsProvider
import com.teco.ventago.features.payments.data.provider.PaypalProvider
import com.teco.ventago.features.payments.data.provider.YappyProvider
import com.teco.ventago.features.payments.data.repository.PaymentsRepository
import com.teco.ventago.features.payments.data.repository.PaypalRepository
import com.teco.ventago.features.payments.data.repository.YappyRepository
import com.teco.ventago.features.payments.domain.PaymentService
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentMethodsViewModel
import com.teco.ventago.features.payments.ui.paypal.viewmodel.PaypalViewModel
import com.teco.ventago.features.payments.ui.yappy.viewmodel.YappyViewModel
import com.teco.ventago.features.pos.domain.PosService
import com.teco.ventago.features.pos.ui.customer.add.viewmodel.AddCustomerViewModel
import com.teco.ventago.features.pos.ui.customer.list.viewmodel.ClientListViewModel
import com.teco.ventago.features.pos.ui.customer.search.viewmodel.SearchCustomerViewModel
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.features.product.data.provider.category.CategoryProvider
import com.teco.ventago.features.product.data.provider.item.ItemProvider
import com.teco.ventago.features.product.data.provider.product.ProductProvider
import com.teco.ventago.features.product.data.repository.ProductsRepository
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.quotes.data.provider.QuotesProvider
import com.teco.ventago.features.quotes.data.repository.QuotesRepository
import com.teco.ventago.features.quotes.domain.QuotesService
import com.teco.ventago.features.expenses.data.provider.ExpensesProvider
import com.teco.ventago.features.expenses.data.repository.ExpensesRepository
import com.teco.ventago.features.expenses.domain.ExpensesService
import com.teco.ventago.features.expenses.ui.create.NewExpenseViewModel
import com.teco.ventago.features.expenses.ui.cufe.CufeImportViewModel
import com.teco.ventago.features.expenses.ui.details.ExpenseDetailsViewModel
import com.teco.ventago.features.expenses.ui.list.ExpensesListViewModel
import com.teco.ventago.features.expenses.ui.accounts.ExpenseAccountsViewModel
import com.teco.ventago.features.product.ui.category.add.viewmodel.AddCategoryViewModel
import com.teco.ventago.features.product.ui.category.add.viewmodel.ModifyCategoryViewModel
import com.teco.ventago.features.product.ui.category.edit.viewmodel.EditCategoryViewModel
import com.teco.ventago.features.product.ui.category.manage.viewmodel.CategoriesManageViewModel
import com.teco.ventago.features.product.ui.item.add.viewmodel.AddItemViewModel
import com.teco.ventago.features.product.ui.item.edit.EditItemViewModel
import com.teco.ventago.features.quotes.ui.list.QuotesListViewModel
import com.teco.ventago.features.quotes.ui.details.QuoteDetailsViewModel
import com.teco.ventago.features.settings.data.provider.SettingsProvider
import com.teco.ventago.features.settings.data.repository.SettingsRepository
import com.teco.ventago.features.settings.domain.SettingsService
import com.teco.ventago.features.settings.ui.address.viewmodel.SetAddressViewModel
import com.teco.ventago.features.settings.ui.logo.viewmodel.ChangeImageViewModel
import com.teco.ventago.features.settings.ui.name.viewmodel.ChangeNameViewModel
import com.teco.ventago.features.settings.ui.settings.viewmodel.SettingsViewModel
import com.teco.ventago.features.user.data.provider.UserProvider
import com.teco.ventago.features.user.data.repository.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = buildVariant() == BuildVariant.SANDBOX
    isLenient = true
    encodeDefaults = true
}

val client = httpClient {
    developmentMode = buildVariant() == BuildVariant.SANDBOX
    install(Logging)
    install(ContentNegotiation) {
        json(json)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 20000_000
        connectTimeoutMillis = 10000_000
        socketTimeoutMillis = 20000_000
    }
    install(HttpCache)
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 2)
        exponentialDelay()
    }
    install(DefaultRequest) {
        header(HttpHeaders.Accept, "*/*")
        header(HttpHeaders.ContentType, "application/json")
    }
}

fun initKoinAndroid(logger: Logger, additionalModules: List<Module>) {
    startKoin {
        logger(logger)
        modules(additionalModules + getBaseModules())
    }
}

/**
 * Initializes Koin for iOS
 * Used from ios swift code
 */
fun initKoinIOS() {
    startKoin {
        logger(IOSLogger())
        modules(getBaseModules())
    }
}


internal class IOSLogger : Logger() {
    override fun display(level: Level, msg: MESSAGE) {
        printLog(level.name, msg)
    }
}

expect val platformModule: Module

internal fun getBaseModules() = viewModels + platformModule + appModule()

internal val viewModels = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::BusinessRegisterViewModel)
    viewModelOf(::AppViewModel)
    viewModel {
        HomeViewModel(
            authService = get(),
            businessService = get(),
            productService = get(),
            financialProfileService = get(),
            homeSummaryService = get(),
            betaService = get(),
            notificationsService = get(),
            ioDispatcher = Dispatchers.Default
        )
    }
    viewModelOf(::CategoriesManageViewModel)
    viewModelOf(::AddCategoryViewModel)
    viewModelOf(::EditCategoryViewModel)
    viewModelOf(::AddItemViewModel)
    viewModelOf(::EditItemViewModel)
    viewModelOf(::ModifyCategoryViewModel)
    viewModelOf(::PosViewModel)
    viewModelOf(::QuotesListViewModel)
    viewModelOf(::QuoteDetailsViewModel)
    viewModelOf(::ExpensesListViewModel)
    viewModelOf(::ExpenseDetailsViewModel)
    viewModelOf(::NewExpenseViewModel)
    viewModelOf(::CufeImportViewModel)
    viewModelOf(::ExpenseAccountsViewModel)
    viewModelOf(::OrdersViewModel)
    viewModelOf(::ChangeImageViewModel)
    viewModelOf(::ChangeNameViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::PaymentMethodsViewModel)
    viewModelOf(::SetAddressViewModel)
    viewModelOf(::PaypalViewModel)
    viewModelOf(::YappyViewModel)
    viewModelOf(::OrdersDetailsViewModel)
    viewModelOf(::OrderInvoiceViewModel)
    viewModelOf(::OrderHistoryViewModel)
    viewModelOf(::BranchesManageViewModel)
    viewModelOf(::PrintersViewModel)
    viewModelOf(::AddCustomerViewModel)
    viewModelOf(::ClientListViewModel)
    viewModelOf(::SearchCustomerViewModel)
    viewModel {
        NotificationsViewModel(
            notificationsService = get(),
            ioDispatcher = Dispatchers.Default
        )
    }
    viewModel { (reportKey: String) ->
        ReportDefinitionViewModel(
            reportKey = reportKey,
            reportsService = get(),
            pdfSharer = get(),
            ioDispatcher = Dispatchers.Default
        )
    }
    viewModelOf(::CustomersListViewModel)
    viewModelOf(::CustomerDetailsViewModel)
    viewModelOf(::CustomerFormViewModel)
    viewModel { (branchCode: String) ->
        BillingPointsManageViewModel(
            branchService = get(),
            businessService = get(),
            logger = get(),
            branchCode = branchCode
        )
    }
    viewModel { (branchCode: String) ->
        AddBillingPointViewModel(
            branchService = get(),
            businessService = get(),
            loggerService = get(),
            branchCode = branchCode
        )
    }
    viewModel { (branchCode: String, billingPoint: String) ->
        EditBillingPointViewModel(
            branchService = get(),
            businessService = get(),
            loggerService = get(),
            branchCode = branchCode,
            billingPoint = billingPoint
        )
    }
    viewModel { (entryContext: String, branchCode: String?, billingPointCode: String?, startAtConfig: Boolean) ->
        PrinterOnboardingViewModel(
            printerService = get(),
            discoveryService = get(),
            branchService = get(),
            logger = get(),
            analyticsService = get(),
            entryContext = entryContext,
            preselectedBranchCode = branchCode,
            preselectedBillingPointCode = billingPointCode,
            startAtConfig = startAtConfig
        )
    }


}


internal fun appModule() = module {
    single<IChangesManager> {
        ChangesManager(get())
    }

    single<IFlagsService> {
        FlagsService(
            logger = get(),
            appScope = get(named("AppScope"))
        )
    }

    single<ICacheService> {
        RoomCache(get(), get())
    }

    single<AnalyticsService> {
        AnalyticsService()
    }

    single<SnackbarService> {
        SnackbarService()
    }

    single<SessionIdStore> {
        SecureStorageSessionIdStore(get())
    }

    single<ISessionIdService> {
        SessionIdService(store = get())
    }

    single<HttpClient> {
        val sessionIdService = get<ISessionIdService>()
        val fingerPrintService = get<FingerPrintService>()
        client.plugin(HttpSend).intercept { request ->
            val originalUA = request.headers[HttpHeaders.UserAgent] ?: ""
            val customUA = "App ver ${AppInfo.VERSION}"
            request.headers.remove(HttpHeaders.UserAgent)
            request.headers.append(HttpHeaders.UserAgent, "$customUA $originalUA".trim())
            if (SessionIdBackendUrlMatcher.isVentaGoBackendUrl(request.url.toString())) {
                request.headers.remove(SessionIdConstants.HEADER_NAME)
                request.headers.append(
                    SessionIdConstants.HEADER_NAME,
                    sessionIdService.sessionIdForBackendRequest()
                )
            }
            if (SessionIdBackendUrlMatcher.isInvoiceBackendUrl(request.url.toString())) {
                request.headers.remove(FINGERPRINT_HEADER_NAME)
                request.headers.append(FINGERPRINT_HEADER_NAME, fingerPrintService.getFingerPrint())
            }
            execute(request)
        }
        client
    }

    single<ILoggerService> {
        LoggerService(
            client = get(),
            secure = get()
        )
    }

    single<IFirebaseService> {
        FirebaseService()
    }

    single<IHomeSummaryProvider> {
        HomeSummaryProvider(
            client = get(),
            authService = get()
        )
    }

    single<IHomeSummaryRepository> {
        HomeSummaryRepository(
            provider = get(),
            logger = get()
        )
    }

    single {
        HomeSummaryService(
            repository = get(),
            storage = get(),
            logger = get(),
            json = json
        )
    }

    single<IRealTimeReportsProvider> {
        RealTimeReportsProvider(
            client = get(),
            authService = get()
        )
    }

    single<IRealTimeReportsRepository> {
        RealTimeReportsRepository(
            provider = get(),
            logger = get()
        )
    }

    single {
        RealTimeReportsService(
            repository = get(),
            businessService = get()
        )
    }

    single<IAuthProvider> {
        AuthProvider(
            client = get()
        )
    }

    single<IAuthRepository> {
        AuthRepository(
            provider = get(),
            logger = get()
        )
    }

    single<IAuthService> {
        AuthService(
            store = get(),
            firebase = get(),
            repository = get(),
            userRepository = UserRepository(
                provider = get(),
                userProvider = UserProvider(
                    client = get()
                ),
                logger = get()
            ),
            cache = get(),
            changesManager = get(),
            client = get(),
            sessionIdService = get()
        )
    }

    factory {
        SettingsService(
            repository = SettingsRepository(
                provider = SettingsProvider(
                    client = get(),
                    authService = get()
                )
            ),
            businessService = get(),
        )
    }

    single {
        OrderService(
            repository = OrdersRepository(
                provider = OrdersProvider(
                    client = get(),
                    authService = get()
                ),
                logger = get()
            )
        )
    }

    single {
        FinancialProfileService(
            cache = get(),
            changesManager = get(),
            repo = FinancialProfileRepository(
                provider = FinancialProfileProvider(
                    client = get(),
                    authService = get()
                ),
                logger = get()
            ),
            loggerService = get(),
            appScope = get(named("AppScope"))
        )
    }

    single {
        CustomerService(
            repository = CustomerRepository(
                provider = CustomerProvider(
                    client = get(),
                    authService = get()
                ),
                logger = get()
            ),
            loggerService = get(),
            cache = get(),
            changesManager = get(),
            appScope = get(named("AppScope"))
        )
    }

    single {
        BranchService(
            branchRepository = BranchRepository(
                provider = BranchProvider(
                    client = get(),
                    authService = get(),
                    logger = get()
                ),
                logger = get()
            ),
            cache = get(),
            changesManager = get(),
            appScope = get(named("AppScope"))
        )
    }

    single {
        ProductService(
            productsRepository = ProductsRepository(
                productProvider = ProductProvider(
                    client = get()
                ),
                categoryProvider = CategoryProvider(
                    client = get()
                ),
                itemProvider = ItemProvider(
                    client = get(),
                    authService = get()
                ),
                logger = get()
            ),
            cache = get(),
            changesManager = get(),
            authService = get()
        )
    }

    single {
        BusinessService(
            repository = BusinessRepository(
                provider = BusinessProvider(
                    client = get(),
                    authService = get()
                ),
                logger = get()
            ),
            cache = get(),
            changesManager = get(),
            authService = get(),
            productService = get(),
        )
    }

    single {
        BetaService(
            repository = BetaRepository(
                provider = BetaProvider(
                    client = get(),
                    authService = get(),
                ),
                logger = get(),
                json = json
            ),
            authService = get(),
            businessService = get(),
            storage = get(),
            json = json,
            appScope = get(named("AppScope"))
        )
    }

    single {
        QuotesService(
            repository = QuotesRepository(
                provider = QuotesProvider(
                    client = get(),
                    authService = get()
                ),
                logger = get()
            ),
            businessService = get(),
            logger = get(),
            authService = get(),
            storage = get(),
            json = json
        )
    }

    single<IInvoicingSettingsProvider> {
        InvoicingSettingsProvider(
            client = get(),
            authService = get(),
        )
    }

    single<IInvoicingSettingsRepository> {
        InvoicingSettingsRepository(
            provider = get(),
            logger = get(),
        )
    }

    single<InvoicingSettingsStore> {
        LocalStorageInvoicingSettingsStore(
            storage = get(),
        )
    }

    single {
        InvoicingSettingsService(
            repository = get(),
            businessService = get(),
            authService = get(),
            store = get(),
            json = json,
            appScope = get(named("AppScope")),
        )
    }

    single {
        ExpensesService(
            repository = ExpensesRepository(
                provider = ExpensesProvider(
                    client = get(),
                    authService = get(),
                    logger = get()
                ),
                logger = get()
            ),
            businessService = get(),
            logger = get(),
            authService = get(),
            storage = get(),
            json = json
        )
    }

    single {
        PosService(
            repository = OrdersRepository(
                provider = OrdersProvider(
                    client = get(),
                    authService = get()
                ),
                logger = get()
            )
        )
    }

    single<IPrinterProvider> {
        PrinterProvider(
            client = get(),
            authService = get()
        )
    }

    single<IPrinterRepository> {
        PrinterRepository(
            provider = get(),
            logger = get()
        )
    }

    single {
        PrinterCacheSyncService(
            storage = get(),
            logger = get(),
            appScope = get(named("AppScope"))
        )
    }

    single {
        PrinterService(
            repository = get(),
            engine = get<PrinterEngine>(),
            businessService = get(),
            storage = get(),
            logger = get(),
            cacheSyncService = get(),
            json = json,
            appScope = get(named("AppScope"))
        )
    }

    single {
        PrinterDiscoveryService(
            engine = get<PrinterDiscoveryEngine>(),
            logger = get(),
        )
    }

    single {
        PaymentService(
            paypalRepository = PaypalRepository(
                provider = PaypalProvider(
                    client = get(),
                    authService = get()
                ),
                logger = get()
            ),
            yappyRepository = YappyRepository(
                provider = YappyProvider(
                    client = get(),
                    authService = get()
                ),
                logger = get()
            ),
            paymentsRepository = PaymentsRepository(
                provider = PaymentsProvider(
                    client = get(),
                    authService = get()
                ),
                logger = get()
            ),
            financialProfileService = get()
        )
    }

    single<INotificationsProvider> {
        NotificationsProvider(
            client = get(),
            authService = get()
        )
    }

    single<INotificationsRepository> {
        NotificationsRepository(
            provider = get(),
            logger = get()
        )
    }

    single<INotificationsService> {
        NotificationsService(
            repository = get(),
            businessService = get(),
            localStorage = get(),
            appScope = get(named("AppScope")),
            json = json,
            authService = get(),
            analyticsService = get()
        )
    }
}
