# VentaGo Architecture

## Overview

VentaGo is a **Kotlin Multiplatform (KMP)** application using **Compose Multiplatform** for shared UI across Android and iOS. It follows **MVVM + Clean Architecture** with a feature-based module organization inside a single `composeApp` module.

---

## Project Structure

```
VentaGo2/
├── composeApp/                    # Main KMP module
│   └── src/
│       ├── commonMain/            # Shared code (UI, business logic, data)
│       ├── androidMain/           # Android-specific implementations
│       ├── iosMain/               # iOS-specific implementations
│       └── commonTest/            # Shared tests
├── iosApp/                        # iOS native wrapper (Swift entry point)
├── doc/                           # Documentation
├── gradle/                        # Gradle wrapper
└── AGENTS.md                      # AI agent instructions
```

---

## Feature Organization

Each feature lives under `features/[featureName]/` and follows a three-layer structure:

```
features/[feature]/
├── domain/                        # Business logic layer
│   ├── [Feature]Service.kt        # Business logic orchestrator
│   ├── I[Feature]Service.kt       # Service interface
│   └── model/                     # Domain models, request/response DTOs
├── data/                          # Data access layer
│   ├── repository/
│   │   ├── [Feature]Repository.kt
│   │   └── I[Feature]Repository.kt
│   └── provider/
│       ├── [Feature]Provider.kt   # HTTP calls via Ktor
│       └── I[Feature]Provider.kt
└── ui/                            # Presentation layer
    └── [screen_name]/
        ├── [Feature]Screen.kt     # Composable screen
        ├── [Feature]Actions.kt    # Screen actions (optional)
        └── viewmodel/
            ├── [Feature]ViewModel.kt
            ├── [Feature]State.kt
            └── [Feature]UiEvent.kt
```

### Active Features

`auth`, `home`, `business`, `branches`, `customers`, `product`, `pos`, `orders`, `invoicing`, `payments`, `quotes`, `expenses`, `settings`, `user`, `financialProfile`, `onboarding`

---

## Data Flow

```
Screen (Composable)
  ↓ collects state via collectAsState()
ViewModel (extends BaseViewModel)
  ↓ calls
Service (domain business logic)
  ↓ calls
Repository (data access + error handling + logging)
  ↓ calls
Provider (HTTP client / Ktor)
  ↓ calls
Ktor HttpClient → Backend API
  ↓ caches in
Room Database (SQLite)
```

### Architecture Diagram

```
┌─────────────────────────────────────────┐
│       Compose Multiplatform UI          │  Screens, Components, Theme
│       (design_system + features/ui)     │
├─────────────────────────────────────────┤
│      ViewModels (BaseViewModel)         │  State management, Events
│     (features/*/ui/*/viewmodel)         │
├─────────────────────────────────────────┤
│        Services (Domain Layer)          │  Business logic orchestration
│     (features/*/domain/*Service)        │
├─────────────────────────────────────────┤
│    Repositories (Data Aggregation)      │  Data access abstraction
│   (features/*/data/repository)          │
├─────────────────────────────────────────┤
│   Providers (API/Network Layer)         │  HTTP calls via Ktor
│    (features/*/data/provider)           │
├─────────────────────────────────────────┤
│    Core Infrastructure Services         │  Cache, Security, Analytics
│    (core/)                              │
├─────────────────────────────────────────┤
│      Platform-Specific Implementations  │  Android/iOS adapters
│      (androidMain, iosMain)             │
└─────────────────────────────────────────┘
```

---

## Design System

Located in `design_system/`, follows an atomic design hierarchy:

```
design_system/
├── theme/
│   ├── Theme.kt          # Material3 theme (light/dark)
│   ├── Color.kt           # Color definitions
│   └── Typography.kt      # Text styles (latoFontFamily)
├── buttons/               # ButtonM, OutlinedButtonM, TextButtonS
├── textfields/            # DMTextField, DMOutlinedTextField
├── molecules/             # Reusable components (cards, badges, dialogs, pickers)
├── organism/              # Complex layouts (LoadingSheet, ItemScreenActions)
└── loaders/               # Loading indicators, shimmer
```

---

## Navigation

- **Framework**: Jetpack Navigation Compose with type-safe routes
- **Route definitions**: `PosScreens` enum/sealed class with `@Serializable` routes
- **Navigation graph**: `Navigation.kt` builds the full graph
- **Deep links**: Supported via `ExternalUriHandler` service
- **Pattern**: Screens receive `navigate: (PosScreens) -> Unit` callback
- **Global access**: `LocalNavController` CompositionLocal

---

## Dependency Injection (Koin)

- **Configuration**: `AppModule.kt` (common) + `AppModule.android.kt` / `AppModule.ios.kt` (platform)
- **Initialization**: `initKoinAndroid()` / `initKoinIOS()`
- **Scoping**: `single` (singletons), `factory` (new each time), `viewModelOf` (ViewModels)
- **Injection in Composables**: `koinViewModel<T>()`, `koinInject<T>()`
- **Platform bindings**: `expect val platformModule: Module` / `actual val platformModule`

---

## Core Infrastructure (`core/`)

| Service | Purpose |
|---|---|
| `BaseViewModel` | Base class for all ViewModels (state + events + loading) |
| `RoomCache` / `CacheDatabase` | Room SQLite local database |
| `SecureStorage` | Encrypted storage for tokens/sensitive data |
| `SnackbarService` | Global snackbar notifications |
| `AnalyticsService` | Firebase analytics wrapper |
| `LoggerService` / `ILoggerService` | Centralized logging |
| `IFlagsService` | Feature flags from backend |
| `BetaService` | Beta feature access control |
| `FirebaseService` | Firebase auth/database integration |
| `LocationService` | Location-based features |
| `ExternalUriHandler` | Deep link handling |
| `PdfSharer` | PDF export (platform-specific) |
| `ChangesManager` | Tracks data modifications for sync |

---

## Platform-Specific Code

Uses the **expect/actual** pattern for platform differences:

```kotlin
// commonMain
expect val platformModule: Module
expect fun httpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient

// androidMain — CIO engine, Room DB, ML Kit, Firebase Android SDK
// iosMain   — Darwin engine, native iOS APIs
```

**Android entry points**: `MainActivity.kt`, `MainApplication.kt`
**iOS entry point**: Swift wrapper calling `initKoinIOS()`

---

## Key Dependencies

| Library | Purpose |
|---|---|
| Compose Multiplatform | Shared UI framework |
| Ktor Client | HTTP networking (CIO/Darwin engines) |
| Room | Local SQLite database |
| Koin | Dependency injection |
| Firebase Kotlin SDK | Auth, Firestore, Realtime DB, Analytics |
| Jetpack Navigation | Screen navigation |
| Kotlinx Coroutines | Async/concurrency |
| Kotlinx Serialization | JSON serialization |
| Coil | Image loading |
