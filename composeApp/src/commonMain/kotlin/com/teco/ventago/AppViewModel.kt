package com.teco.ventago

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.firebase.getToken
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.auth.domain.model.User
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.payments.domain.PaymentService
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.product.domain.model.Products
import com.teco.ventago.navigation.SessionNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class AppViewModel(
    private val authService: IAuthService,
    private val businessService: BusinessService,
    private val productService: ProductService,
    private val paymentsService: PaymentService,
    private val financialProfileService: FinancialProfileService,
    private val customerService: CustomerService,
    private val branchService: BranchService,
    private val logger: ILoggerService,
) : ViewModel() {
    private val _mainState = MutableStateFlow(MainState())
    val mainState = _mainState.asStateFlow()

    private lateinit var loadDataJob: Job

    init {
        viewModelScope.launch {
            authService.sessionResolved()
                .combine(authService.getUser()) { resolved, user ->
                    resolved to user
                }.collect { (resolved, user) ->
                    if (!resolved) {
                        _mainState.value = _mainState.value.copy(sessionResolved = false)
                        return@collect
                    }
                    val wasAuthenticated = _mainState.value.isAuthenticated
                    val previousAuthenticated =
                        if (_mainState.value.sessionResolved) wasAuthenticated else null
                    val nextAuthenticated = user != null
                    val nextEpoch = SessionNavigation.nextSessionEpoch(
                        currentEpoch = _mainState.value.sessionEpoch,
                        previousAuthenticated = previousAuthenticated,
                        nextAuthenticated = nextAuthenticated,
                    )
                    if (user != null) {
                        val loginTransition = _mainState.value.sessionResolved && !wasAuthenticated
                        _mainState.value = _mainState.value.copy(
                            isAuthenticated = true,
                            missingBusiness = user.missingBusiness,
                            sessionResolved = true,
                            sessionEpoch = nextEpoch,
                        )
                        val shouldLoadBusiness =
                            !user.missingBusiness &&
                                (loginTransition || businessService.business.value == null)
                        if (shouldLoadBusiness) {
                            if (this@AppViewModel::loadDataJob.isInitialized && loadDataJob.isActive) {
                                loadDataJob.cancel()
                            }
                            loadBusinessData(user = user)
                        } else if (this@AppViewModel::loadDataJob.isInitialized) {
                            if (loadDataJob.isActive) {
                                loadDataJob.cancel()
                            }
                        }
                    } else {
                        _mainState.value = MainState(
                            sessionResolved = true,
                            sessionEpoch = nextEpoch,
                        )
                        clearFeatureStateAfterSignOut()
                    }
                }
        }

        viewModelScope.launch {
            financialProfileService.observe().onEach { profile ->
                profile?.let {
                    try {
                        withContext(Dispatchers.Main) {
                            _mainState.value = _mainState.value.copy(
                                invoicingConfigured = profile.invoicingActive,
                                paymentsConfigured = financialProfileService.paymentsConfigured())
                        }
                        if (profile.invoicingActive && !branchService.isInitialized) {
                            branchService.initialize(profile.businessId)
                        }

                    } catch (e: Exception) {
                        logger.sendLog(
                            Log(
                                LogLevel.ERROR,
                                "AppViewModel::init()",
                                "Error updating paymentsConfigured state: ${e.message ?: "UNKNOWN"}"
                            )
                        )
                    }
                }
            }.launchIn(this)
        }
        getToken()
    }

    private fun loadBusinessData(user: User? = null) {
        loadDataJob = viewModelScope.launch(Dispatchers.IO) {
            var userAux: User? = user
            if (user == null) {
                userAux = authService.getUserSync()
            }
            userAux?.let { user ->
                try {
                    supervisorScope {
                        val businessId = user.businessIds.first().businessId

                        val businessJob = async {
                            runCatching { businessService.getBusinessById(businessId) }
                        }

                        val productsJob = async {
                            runCatching { productService.getProductsByBusinessId(businessId) }
                        }

                        val financialJob = async {
                            runCatching { financialProfileService.setBusiness(businessId, true) }
                        }

                        val businessRes = businessJob.await()
                        val financialRes = financialJob.await()
                        val productRes = productsJob.await()

                        businessRes.exceptionOrNull()?.let {
                            logger.sendLog(
                                Log(
                                    LogLevel.ERROR,
                                    "AppViewModel::loadBusinessData()",
                                    "getBusinessById failed: ${it.message ?: "UNKNOWN"}"
                                )
                            )
                        }
                        financialRes.exceptionOrNull()?.let {
                            logger.sendLog(
                                Log(
                                    LogLevel.ERROR,
                                    "AppViewModel::loadBusinessData()",
                                    "setBusiness (financialProfile) failed: ${it.message ?: "UNKNOWN"}"
                                )
                            )
                        }
                        productRes.exceptionOrNull()?.let {
                            println("ASDASD: getProductsByBusinessId failed: ${it.message ?: "UNKNOWN"}")
                            logger.sendLog(
                                Log(
                                    LogLevel.ERROR,
                                    "AppViewModel::loadBusinessData()",
                                    "getProductsByBusinessId failed: ${it.message ?: "UNKNOWN"}"
                                )
                            )
                        }

                        val ok = businessRes.isSuccess && productRes.isSuccess

                        withContext(Dispatchers.Main) {
                            if (ok) {
                                _mainState.value =
                                    _mainState.value.copy(
                                        business = businessRes.getOrNull(),
                                        products = productRes.getOrNull(),
                                        isAuthenticated = true,
                                        paymentsConfigured = financialProfileService.paymentsConfigured(),
                                        invoicingConfigured = financialProfileService.invoicingEnabled(),
                                    )
                            }
                        }

                        customerService.initialize(businessId, loadData = true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    logger.sendLog(
                        Log(
                            LogLevel.ERROR,
                            "AppViewModel::loadBusinessData()",
                            "Error loading business data: ${e.message ?: "UNKNOWN"}"
                        )
                    )
                }
            } ?: println("ASDASD: No user getUserSync() is null")
        }

    }


    fun setHideAppVar(hideAppVar: Boolean) {
        _mainState.value = _mainState.value.copy(hideAppVar = hideAppVar)
    }

    private fun clearFeatureStateAfterSignOut() {
        businessService.clear()
        productService.signOut()
        financialProfileService.clear()
        customerService.clear()
        branchService.clear()
    }
}

data class MainState(
    val sessionResolved: Boolean = false,
    val sessionEpoch: Int = 0,
    val isAuthenticated: Boolean = false,
    val missingBusiness: Boolean = false,
    val hideAppVar: Boolean = false,
    val business: Business? = null,
    val products: Products? = null,
    val paymentsConfigured: Boolean = true,
    val invoicingConfigured: Boolean = true,
)
