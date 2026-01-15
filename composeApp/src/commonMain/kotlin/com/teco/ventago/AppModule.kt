package com.teco.ventago

import com.teco.ventago.core.SnackbarService
import com.teco.ventago.core.beta.BetaProvider
import com.teco.ventago.core.beta.BetaRepository
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.cache.RoomCache
import com.teco.ventago.core.changes.ChangesManager
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.LoggerService
import com.teco.ventago.core.logger.printLog
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
import com.teco.ventago.features.financialProfile.data.provider.FinancialProfileProvider
import com.teco.ventago.features.financialProfile.data.repository.FinancialProfileRepository
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.home.domain.HistoricSalesService
import com.teco.ventago.features.home.ui.viewmodel.HomeViewModel
import com.teco.ventago.features.orders.data.provider.OrdersProvider
import com.teco.ventago.features.orders.data.repository.OrdersRepository
import com.teco.ventago.features.orders.domain.OrderService
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrdersDetailsViewModel
import com.teco.ventago.features.orders.ui.order_history.viewModel.OrderHistoryViewModel
import com.teco.ventago.features.orders.ui.order_invoice.viewModel.OrderInvoiceViewModel
import com.teco.ventago.features.orders.ui.orders.viewmodel.OrdersViewModel
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
    viewModelOf(::HomeViewModel)
    viewModelOf(::CategoriesManageViewModel)
    viewModelOf(::AddCategoryViewModel)
    viewModelOf(::EditCategoryViewModel)
    viewModelOf(::AddItemViewModel)
    viewModelOf(::EditItemViewModel)
    viewModelOf(::ModifyCategoryViewModel)
    viewModelOf(::PosViewModel)
    viewModelOf(::QuotesListViewModel)
    viewModelOf(::QuoteDetailsViewModel)
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
    viewModelOf(::AddCustomerViewModel)
    viewModelOf(::ClientListViewModel)
    viewModelOf(::SearchCustomerViewModel)
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


}


internal fun appModule() = module {
    single<IChangesManager> {
        ChangesManager(get())
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

    single<HttpClient> {
        client.plugin(HttpSend).intercept { request ->
            val originalUA = request.headers[HttpHeaders.UserAgent] ?: ""
            val customUA = "App ver ${AppInfo.VERSION}"
            request.headers.remove(HttpHeaders.UserAgent)
            request.headers.append(HttpHeaders.UserAgent, "$customUA $originalUA".trim())
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

    single<HistoricSalesService> {
        HistoricSalesService(get())
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
            client = get()
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
                    authService = get()
                ),
                logger = get(),
                json = json
            ),
            authService = get(),
            storage = get(),
            json = json
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
}
