# Quotes Feature Implementation Plan (App)

## Goals
- Bring the web quotes feature to native with three flows: creation, configs, listing/details.
- Reuse the POS flow for quote creation while skipping payment.
- Gate all quotes entry points behind beta feature access.
- Keep backend contracts identical to web.

## Reference Code (reuse patterns)
- POS flow screens: `composeApp/src/commonMain/kotlin/com/teco/ventago/features/pos/ui`
- POS request builder: `composeApp/src/commonMain/kotlin/com/teco/ventago/features/pos/ui/viewmodel/PosViewModel.kt`
- Cart totals and money utils: `composeApp/src/commonMain/kotlin/com/teco/ventago/features/pos/ui/viewmodel/PosState.kt`, `composeApp/src/commonMain/kotlin/com/teco/ventago/utils/NumberUtils.kt`
- Orders list/details UI: `features/orders/ui/orders/OrdersScreen.kt`, `features/orders/ui/order_details/OrderDetailsScreen.kt`
- Settings UI: `features/settings/ui/settings/SettingsScreen.kt`
- Networking patterns: `features/orders/data/provider/OrdersProvider.kt`, `features/orders/data/repository/OrdersRepository.kt`
- Navigation wiring: `navigation/Navigation.kt`

## Target file structure / touchpoints
- New core beta module: `core/beta/{BetaFeature,BetaProvider,BetaRepository,BetaService}` plus models (`BetaFeaturesResponse`, `BetaAccessResponse`).
- New quotes module:
  - data: provider (`IQuotesProvider`, `QuotesProvider`, `QuotesRequests`), repository (`IQuotesRepository`, `QuotesRepository`).
  - domain: `QuotesService`, `QuoteRequestBuilder`, models (`Quote`, `QuoteLine`, `QuoteTotals`, `QuoteStatus`, customer snapshot), request DTOs (`CreateQuoteRequest`, `UpdateQuoteRequest`, `ListQuotesRequest`, `GetQuoteRequest`, `CancelQuoteRequest`, `SendQuoteEmailRequest`).
  - ui: `summary/QuoteSummaryScreen (+VM/state)`, `success/QuoteSuccessScreen`, `list/QuotesListScreen (+VM/state)`, `details/QuoteDetailsScreen (+VM/state)`, `settings/QuoteSettingsSection`.
- Files to modify: `AppModule.kt` (DI), `Navigation.kt` (quotes graph + PosScreens), `HomeScreen.kt` + `HomeViewModel.kt` (beta access + CTA), `PosState.kt` and `PosViewModel.kt` (quote mode + quoteId + request builder), `SettingsScreen/SettingsState/SettingsViewModel` (quote defaults), string resources (quote labels/statuses).
- Optional (confirm backend): quote settings endpoints (e.g. `GET/PUT /api/v1/business/quote-settings`) to load/save default additional info/style.

## API Endpoints (same as web)
- Beta: `GET /api/v1/beta/features`, `GET /api/v1/beta/features/quotes/access`
- Quotes: `POST /api/v1/quotes/list`, `POST /api/v1/quotes/get`, `GET /api/v1/quotes/{id}/pdf`, `POST /api/v1/quotes/send-email`, `POST /api/v1/quotes/cancel`, `POST /api/v1/quotes/create`, `POST /api/v1/quotes/update`
- Headers: `Authorization: Bearer <token>` and `X-Business-ID` (same as OrdersProvider)

### JS-to-Kotlin mapping cheat sheet (API bodies)
- List quotes body: `{ "page":0, "page_size":10, "customer_name":null, "customer_ruc":null, "quote_number":null, "status":null }`
- Get quote body: `{ "quote_id":123, "quote_number":null }`
- Send email body: `{ "quote_id":123, "recipient_email":"client@example.com" }`
- Cancel body: `{ "quote_id":123, "reason":"at least 10 chars" }`
- Create/update body (core fields):
```
{
  "customer_id": 12,
  "final_customer": false,
  "final_customer_info": {"name":"","email":"","phone":"","id_type":"","id_number":"","country":""},
  "quote_style": "style1",
  "expiry_date": "YYYY-MM-DD",
  "items": [...],
  "totals": {...},
  "include_payment_button": false,
  "additional_info": "<html or plain text>",
  "quote_id": 456 // only for update
}
```

## Plan

### 1) Core beta feature module
- Add beta models (features list + access), provider with Orders headers, repository with AUTH_001 refresh retry, service exposing `hasAccess(key)` and cached state.
- Wire beta DI in `AppModule.kt`; clear on sign out.
- Use beta service in Home/Settings/Navigation guards.

### 2) Quotes data layer (domain, provider, repository, service)
- Domain models mirror JS (`Quote`, `QuoteLine`, `QuoteTotals`, `QuoteStatus`, customer snapshot). Status: 0 DRAFT, 1 CREATED, 2 ACCEPTED, 3 REJECTED, 4 CANCELLED.
- Request models use exact JS field names: `quote_style`, `expiry_date`, `include_payment_button`, `additional_info`, etc. Reuse order item DTOs where shapes match (`OrderItem`, `OrderItemTax`, `OrderItemDiscount`, `ItemTotals`, `Charge`, `InvoiceCharge`, `InvoiceDiscount`, `NameValue`, `PharmaSale`).
- Provider functions: list, get, pdf, sendEmail, cancel, create, update with same bodies as JS.
- Repository handles ApiResponse, auth retry, and JSON normalization:
  - List endpoint has flat totals and no `lines`.
  - Get endpoint includes `lines`; parse `tax_name`/`tax_rate` strings into ITBMS/ISC/OTI; if `tax_name` empty but `tax_rate` present, treat as ITBMS.
  - Map `warranty_policies` to `additional_info` if needed; build customer snapshot from flat fields.
- Service exposes list/get/create/update/cancel/sendEmail and adapters to/from POS state.

#### 2.1) Kotlin QuoteRequestBuilder (match JS buildCreateQuoteRequest)
- Inputs: POS state (customer selection = step1Data, cart/charges/taxes = step2Data, additional info/style/expiry = step3Data), optional quoteId for updates.
- Build `itemsById` from catalog/POS items.
- For each cart line:
  - Identity: `item_id` (or 0), `code` (barcode or "0001"), `unit_measure` (unitMeasureCode or "und"), `product_type` (product.productType), `name`, `quantity`.
  - Prices: `base_unit_price` from unit price; `override_unit_price` only if custom price set.
  - Discounts: per-unit discount based on percentage or fixed; send `discounts=[{amount:dollarsToDecimalString(perUnit)}]` when >0.
  - Taxes: if taxExempt -> ITBMS 0.00 entry; else map percent to codes (7->01/0.07, 10->02/0.10, 15->03/0.15, default 00/0.00). Add ISC if `iscRate>0`, add OTI entries for each product OTI using subtotal * rate.
  - Additional charges: add `FACTURA` for itemFreight, `SEGURO` for itemInsurance.
  - Pharma sale: add `pharma_sale` when pharma; `vehicle_sale=null` always.
  - Additional info: map product.additionalInfo to `{name,value}`; validate Panama goods/service code and unit code (skip invalid/empty).
  - Item totals: before_discounts=unitPrice*qty; after_discounts=line subtotal; before_taxes=line subtotal; taxes=ITBMS+ISC+OTI; after_taxes=lineSubtotal+taxes; total=lineSubtotal+taxes+charges.
- Global charges: add `FACTURA` (global freight) and `SEGURO` (global insurance) only if no per-item freight/insurance; add `OTROS_GASTOS` if otherCharges>0. Map from `globalShippingCents`, `globalInsuranceCents`, `globalOtherChargesCents`.
- Global discounts: if `globalDiscount>0` add `{ description:"Descuento factura", amount:... }`.
- Totals: subtotalBeforeDiscount=sum(unitPrice*qty); subtotalAfterItemDiscounts=subtotal after per-line discounts; totalAfterAllDiscounts=subtotalAfterItemDiscounts - globalDiscount; totalTaxes=ITBMS+ISC+OTI; totalCharges=per-item charges + global charges; invoiceTotal=totalAfterAllDiscounts + totalTaxes + totalCharges.
- Build request: set `customer_id` or `final_customer_info`, `final_customer=true` when needed (include name/email/phone/id_type/id_number/country), `quote_style` default "style1", `include_payment_button=false`, `additional_info` from step3, `expiry_date`, and `quote_id` when updating.

#### 2.2) Money/rounding helpers
- Match JS: round to 2 decimals before string conversion; always send 2-digit decimals.
- Use `Long.toDecimalString()` for cents; add helper equivalent to JS `dollarsToDecimalString` for Double if needed.

#### 2.3) Additional info validation
- For `panama_goods_services_code`: skip empty; allow only if code exists in goods/services list (CSV already in resources).
- For `panama_goods_services_unit_code`: skip empty; allow only if code exists in UOM options.
- Pass other keys through as `{name,value}`.

### 3) Quote creation flow (POS reuse)
- Extend `PosState` with `flowMode` enum (SALE, QUOTE), `quoteId`, `quoteStyle`, `quoteExpiryDate`, `quoteAdditionalInfo`.
- `PosViewModel` adds: `setFlowMode`, `createQuote()` using QuoteRequestBuilder + QuotesService, `loadQuoteForEdit(Quote)` mapping lines to CartLine and prefilling customer + additional fields.
- Navigation: in QUOTE mode, POS -> Product -> Cart -> QuoteSummaryScreen (skip PaymentScreen) -> QuoteSuccessScreen.
- QuoteSummaryScreen: show items/totals (CartCalc), customer info, additional_info input (prefilled from settings), expiry date picker, style selector (horizontal previews), CTA “Generate quote”.
- QuoteSuccessScreen: show success, actions New Quote (reset POS), Download PDF (getQuotePdf -> Base64 decode -> PdfSharer), optional View Quote.

### 4) Quote configs in settings
- Add `QuoteSettingsSection` composable to SettingsScreen (only if hasQuotesAccess).
- Extend SettingsState with `defaultQuoteAdditionalInfo`, `defaultQuoteStyle`, `hasQuotesAccess`.
- SettingsViewModel loads/saves quote defaults (use backend endpoints if available; otherwise store locally) and surfaces beta access from BetaService.
- Rich text: store HTML string; preview via lightweight HTML renderer or plain text fallback; keep storage compatible with web.
- Style selector: reuse web style keys (style1, style2, …) with thumbnails; set defaults applied when starting a new quote.

### 5) Quotes listing and details
- QuotesListScreen: like OrdersScreen (pull-to-refresh, pagination, filters for customer_name/ruc/quote_number/status, “See more”). Quote item shows quote_number, customer, total, status, date.
- QuotesListViewModel: loads pages, refreshes, stores selected quote for navigation.
- QuoteDetailsScreen: like OrderDetailsScreen but without payment blocks; sections for header, customer, lines, totals, additional_info; actions row: Download PDF, Send Email, Cancel, Modify.
- QuoteDetailsViewModel: loadQuote, downloadPdf, sendEmail, cancelQuote (reason >=10 chars), modifyQuote (push into POS quote mode).
- Quote PDF flow: call getQuotePdf, decode base64, open via PdfSharer; filename `quote_{quote_number}.pdf` or `quote_{quote_id}.pdf`.

### 6) Navigation and entry points
- Add PosScreens entries for quotes graph: `Quotes`, `QuotesListScreen`, `QuoteDetailsScreen`, `QuoteSummaryScreen`, `QuoteSuccessScreen`.
- Add `addQuotesNavigation(navController, analyticsService)` similar to orders (graph root `Quotes`, start at list; summary/success can share POS VM owner).
- HomeScreen: add “Create Quote” CTA next to POS card, gated by `hasQuotesAccess`; add entry to open quotes list.
- Guard deep links and buttons with beta access; show snackbar/toast if not allowed.
- Add quote strings to `composeResources/values/strings.xml` (labels, statuses, actions, validation messages).

### 7) Beta gating integration
- After business selection, BetaService fetches features/access; expose `hasQuotesAccess` in shared state (AppViewModel/HomeViewModel/SettingsViewModel).
- Hide quotes UI (Home CTA, settings section, navigation entries) when access is false.
- If a blocked deep link is attempted, show a warning/snackbar and pop back.

### 8) QA checklist and validation
- Verify endpoints: create, update, list, get, pdf, email, cancel.
- Verify totals/taxes match JS builder across discounts, ISC, OTI, tax-exempt scenarios.
- Verify modify flow preserves items/customer data and reopens POS with correct quote metadata.
- Verify settings defaults applied on new quote.
- Verify gating behavior for businesses with and without access; deep links blocked when beta off.
- Verify strings/navigation wiring and PDF download/share flows.

## Navigation snippets (from merged plan)
- PosScreens additions:
```
Quotes(Res.string.quotes, false),
QuotesListScreen(Res.string.quotes, true, showBackButton = false),
QuoteDetailsScreen(Res.string.quote_details, true),
QuoteSummaryScreen(Res.string.quote_summary, true),
QuoteSuccessScreen(Res.string.quote_success, false, showBackButton = false),
```
- addQuotesNavigation skeleton:
```
private fun NavGraphBuilder.addQuotesNavigation(navController: NavHostController, analytics: AnalyticsService) {
    navigation(route = PosScreens.Quotes.name, startDestination = PosScreens.QuotesListScreen.name) {
        composable(PosScreens.QuotesListScreen.name) { backStackEntry ->
            analytics.logScreenView("QuotesListScreen")
            val owner = remember(backStackEntry) { navController.getBackStackEntry(PosScreens.Quotes.name) }
            val vm: QuotesListViewModel = koinViewModel(viewModelStoreOwner = owner)
            QuotesListScreen(vm) { route -> navController.navigate(route.name) }
        }
        composable(PosScreens.QuoteDetailsScreen.name) { backStackEntry ->
            analytics.logScreenView("QuoteDetailsScreen")
            val owner = remember(backStackEntry) { navController.getBackStackEntry(PosScreens.Quotes.name) }
            val vm: QuoteDetailsViewModel = koinViewModel(viewModelStoreOwner = owner)
            QuoteDetailsScreen(vm, navController)
        }
        // Summary/Success share POS VM via POS graph
    }
}
```

## Strings to add (key ones)
- `quotes`, `quote`, `quote_details`, `quote_summary`, `quote_success`, `create_quote`, `generate_quote`, `download_pdf`, `send_by_email`, `modify_quote`, `cancel_quote`, `new_quote`, `quote_created`, `quote_updated`, `quote_cancelled`, `quote_sent`, `expiry_date`, `additional_info`, `quote_style`, `cancel_reason_min`, `enter_email`, `quote_settings`, `default_additional_info`, `default_quote_style`, status labels (`quote_status_draft`, `quote_status_created`, `quote_status_accepted`, `quote_status_rejected`, `quote_status_cancelled`).

## Koin DI additions (example)
- ViewModels: `viewModelOf(::QuotesListViewModel)`, `viewModelOf(::QuoteDetailsViewModel)`, `viewModelOf(::QuoteSummaryViewModel)`.
- beta module singles: `BetaProvider`, `BetaRepository`, `BetaService`.
- quotes module singles: `IQuotesProvider -> QuotesProvider`, `IQuotesRepository -> QuotesRepository`, `QuotesService` (depends on BusinessService/ProductService/logger), `QuoteRequestBuilder` if injected separately.

## Implementation phases (recommended order)
1) Beta module + DI wiring; expose `hasQuotesAccess` and guard UI.
2) Quotes data layer (models, provider, repository, service, request builder) + unit tests for builder mapping against JS rules.
3) POS flow integration (flowMode/quoteId/state, viewmodel changes, request builder hook).
4) Quote summary & success screens wired to POS VM; PDF download.
5) Quote settings section + load/save defaults; apply defaults in quote flow.
6) Quotes list + details screens; actions (send email, cancel with reason validation, modify -> POS).
7) Navigation updates, Home CTA, strings, resource additions.
8) QA pass across flows, beta gating, and PDF/email/cancel edge cases.
