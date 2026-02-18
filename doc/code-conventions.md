# VentaGo Code Conventions

## Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Packages | lowercase, dot-separated | `com.teco.ventago.features.orders.data.repository` |
| Classes / Objects | PascalCase | `LoginViewModel`, `AuthService`, `OrdersRepository` |
| Interfaces | `I` prefix + PascalCase | `IAuthService`, `IOrdersRepository`, `IAuthProvider` |
| Functions / Methods | camelCase | `emailLogin()`, `updateState()`, `showLoading()` |
| Private mutable state | `_` prefix | `_uiState`, `_events` |
| Constants | UPPER_SNAKE_CASE | `SERVER_BASE_PATH` |
| Files | Match the class they contain | `LoginViewModel.kt`, `AuthService.kt` |

---

## ViewModel Pattern

All ViewModels extend `BaseViewModel<UiState, UiEvent>`:

```kotlin
class LoginViewModel(
    private val authService: IAuthService,
) : BaseViewModel<LoginState, LoginUiEvent>(LoginState()) {

    fun emailLogin() {
        showLoading()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val response = authService.emailLogin(request)
                    withContext(Dispatchers.Main) {
                        showSuccess()
                        emitEvent(LoginUiEvent.LoginSuccess)
                    }
                } catch (e: AuthException) {
                    withContext(Dispatchers.Main) {
                        emitEvent(LoginUiEvent.MakingLoginError)
                        showError()
                    }
                } catch (_: Throwable) {
                    withContext(Dispatchers.Main) {
                        emitEvent(LoginUiEvent.GenericError)
                        showError()
                    }
                }
            }
        }
    }

    fun emailChanged(value: String) {
        updateState { copy(email = value) }
    }
}
```

### UI State

- Data class implementing `LoadableState<T>`.
- Contains all screen-level state (form fields, data, loading flags).
- Mutated via `updateState { copy(...) }`.

```kotlin
data class LoginState(
    val email: String = "",
    val password: String = "",
    val invalidEmail: Boolean = false,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<LoginState> {
    override fun withLoading(state: LoadingBottomSheetState) = copy(loadingBottomSheet = state)
}
```

### UI Events

- Sealed class for one-time events (navigation, errors, toasts).
- Emitted via `emitEvent(...)`, collected in `LaunchedEffect`.

```kotlin
sealed class LoginUiEvent {
    data object MissingBusiness : LoginUiEvent()
    data object MakingLoginError : LoginUiEvent()
    data object GenericError : LoginUiEvent()
}
```

---

## Screen (Composable) Pattern

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navigate: (PosScreens) -> Unit) {
    val viewModel: LoginViewModel = koinViewModel<LoginViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginUiEvent.LoginSuccess -> navigate(PosScreens.HomeScreen)
                is LoginUiEvent.GenericError -> { /* show error */ }
            }
        }
    }

    // UI composition using uiState
}
```

---

## Service Layer

- Orchestrates business logic between repositories.
- May expose `StateFlow` for observable state.
- Located in `features/[feature]/domain/`.

```kotlin
class QuotesService(
    private val repository: IQuotesRepository,
    private val businessService: BusinessService,
    private val logger: ILoggerService,
) {
    suspend fun listQuotes(request: ListQuotesRequest): PagedQuotes {
        val businessId = businessId() ?: throw IllegalStateException("No business selected")
        return repository.listQuotes(businessId, request)
    }
}
```

---

## Repository Layer

- Wraps provider calls with error handling and logging.
- Parses API responses into domain models.
- All errors logged via `ILoggerService` before rethrowing.

```kotlin
class AuthRepository(
    private val provider: IAuthProvider,
    private val logger: ILoggerService,
) : IAuthRepository {
    override suspend fun emailLogin(request: EmailLoginRequest): AuthResponse {
        try {
            val response = provider.emailLogin(request)
            if (response.error.isError()) getError(response)
            val map = Json.decodeFromJsonElement<Map<String, JsonElement>>(response.data as JsonObject)
            return AuthResponse.fromMap(map)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "emailLogin", e.message ?: "UNKNOWN"))
            throw e
        }
    }
}
```

---

## Provider Layer

- Raw HTTP calls using Ktor `HttpClient`.
- Returns `ApiResponse` wrapper.
- Located in `features/[feature]/data/provider/`.

```kotlin
class AuthProvider(private val client: HttpClient) : IAuthProvider {
    override suspend fun emailLogin(request: EmailLoginRequest): ApiResponse {
        val res = client.post(Configs.serverBasePath + "auth/email-login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return ApiResponse.fromJson(res.body<JsonObject>())
    }
}
```

---

## Serialization

- **Library**: Kotlinx Serialization with custom `Json` instance.
- **Config**: `ignoreUnknownKeys = true`, `isLenient = true`, `encodeDefaults = true`.
- **Field mapping**: `@SerialName("snake_case_field")` for API responses.
- **Complex parsing**: Companion object methods `fromMap()`, `listFromMap()` for manual deserialization of nested structures.

```kotlin
@Serializable
data class Item(
    val itemId: Int,
    @SerialName("barcode") val barcode: String?,
    val price: Double,
) {
    companion object {
        fun listFromMap(arr: JsonArray): List<Item> { /* ... */ }
    }
}
```

---

## Error Handling

### Custom Exceptions (`utils/ApiExceptions.kt`)

```kotlin
class AuthException(val error: ApiError) : Exception()
class NoInternetException : Exception()
class BadRequestException(override val message: String) : Exception()
class InvalidRucException : Exception()
class DomainInUseException(override val message: String) : Exception()
```

### Pattern in ViewModels

1. Wrap async operations in `try/catch`.
2. Match specific exception types first, generic `Throwable` last.
3. Emit UI events for each error case.
4. Call `showError()` to update loading state.

### Pattern in Repositories

1. Wrap provider calls in `try/catch`.
2. Log error with `logger.sendLog(Log(LogLevel.ERROR, ...))`.
3. Rethrow after logging.

---

## Loading State

### Global Loading (LoadingBottomSheet)

Built into `BaseViewModel` via `LoadableState<T>`:

- `showLoading()` — show loading indicator
- `showSuccess()` — show success feedback
- `showError()` — show error feedback
- `hideLoading()` — dismiss

### Per-Operation Loading

Boolean flags in state for granular control:

```kotlin
data class QuoteDetailsState(
    val isLoading: Boolean = false,
    val isSendingEmail: Boolean = false,
    val isDownloadingPdf: Boolean = false,
)
```

---

## Async / Coroutines

- **IO operations**: `withContext(Dispatchers.IO) { ... }`
- **UI updates**: `withContext(Dispatchers.Main) { ... }`
- **ViewModel scope**: `viewModelScope.launch { ... }`
- **Service scope**: `CoroutineScope(Dispatchers.IO + SupervisorJob())`
- **Flow collection**: `combine()`, `launchIn(scope)`, `collectAsState()`

---

## Dependency Injection (Koin)

```kotlin
// Singletons
single<IAuthService> { AuthService(store = get(), firebase = get(), repository = get()) }

// Factories (new instance per injection)
factory { SettingsService(repository = get(), businessService = get()) }

// ViewModels
viewModelOf(::LoginViewModel)
viewModel { (branchCode: String) -> BillingPointsManageViewModel(branchCode = branchCode, ...) }

// Injection in Composables
val viewModel: LoginViewModel = koinViewModel<LoginViewModel>()
val service: SnackbarService = koinInject()
```

---

## Resources & Strings

- **Location**: `composeApp/src/commonMain/composeResources/values/strings.xml`
- **Usage**: `stringResource(Res.string.login)`
- **Multi-language**: `values-es/` for Spanish translations

---

## Validation

- Regex-based email validation: `emailRegex.matches(email)`
- Field-level checks before API calls.
- Validation errors stored in state: `copy(invalidEmail = !isValidEmail())`.

---

## Logging

Centralized via `ILoggerService`:

```kotlin
logger.sendLog(Log(LogLevel.ERROR, "functionName", "error context message"))
```

Used in repositories and services to track API failures and business logic errors.
