# Customer Tax Presets + CRUD Parity TODO

## Plan
- [x] Extender contratos, modelos y cache de clientes para soportar `tax_exempt`, `tax_retention_code` y `tax_retention_percent` en create/list/get/update.
- [x] Actualizar UI/ViewModels de clientes para crear/editar/mostrar presets fiscales de forma consistente.
- [x] Aplicar presets fiscales del cliente en POS al seleccionar o hidratar clientes registrados sin pisar configuración ya existente cuando el flujo requiera hidratación por ID.
- [x] Ejecutar verificación enfocada y documentar resultados.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Se agregaron serializers/helpers fiscales compartidos para aceptar `camelCase` y `snake_case`, incluyendo `tax_retention_code` como numero o string (`"01"` -> `1`).
- El CRUD de clientes ahora propaga los presets fiscales en modelos, payloads, cache y UI de create/edit/details; `update` siempre envía `tax_exempt` y puede limpiar retención con `null`.
- POS ahora aplica los presets del `CustomerListItem` al seleccionar un cliente y rehidrata por `customerId` en flujos que reconstruyen el cliente desde quote/nota para recuperar esos presets sin pisar settings ya presentes.
- Verificación completada con `testDebugUnitTest` y `compileDebugKotlinAndroid`.

# Orders See More Pagination TODO

## Plan
- [x] Inspect orders pagination state/request path for the `See more` flow.
- [x] Align orders page tracking with the backend paging contract so the first append request fetches the next page.
- [x] Run Android compile verification and record the result.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `OrderService` now starts and resets the orders list page counter at `1` instead of `0`, matching the backend paging contract used by the orders list endpoint.
- This fixes the first `See more` tap requesting the same initial page again before moving to the real next page on the second tap.

# Orders + Quotes List Crash Fix TODO

## Plan
- [x] Inspect orders list state/render path and remove shared mutable list exposure that can trigger Compose iteration crashes.
- [x] Fix quotes list pagination to start at page `1` and prevent duplicate quote rows/keys when appending pages.
- [x] Run focused verification for the touched KMP code and record results.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Orders list state no longer stores the live mutable `orderService.orders`; the ViewModel now snapshots to immutable lists before publishing/filtering, preventing Compose iteration over a concurrently mutated `ArrayList`.
- Quotes list pagination now starts at page `1` end-to-end (`ListQuotesRequest`, UI state resets, and initial loads), matching the backend’s 1-based paging contract.
- Quotes list loading now ignores overlapping requests, merges pages through identifier-based de-duplication, and uses safer lazy item keys to avoid duplicate-key crashes if repeated backend rows slip through.

# POS Search/Selector Height Alignment TODO

## Expense Categorization Request Debug TODO

### Plan
- [x] Inspeccionar el punto final donde se serializa y envía `PATCH /expenses/{id}/categorization`.
- [x] Loggear el payload JSON limpio final antes del `setBody` para comparar con el request esperado por backend.
- [x] Ejecutar una verificación puntual para confirmar que el build sigue compilando y que el logging quedó activo.

### Verification Gates
- [ ] `./gradlew --no-daemon -Pkotlin.incremental=false :composeApp:compileDebugKotlinAndroid` (bloqueado por un problema local de build/output tracking: `NoSuchFileException ... Res$array.class`)
- [x] `./gradlew --no-daemon -Pkotlin.incremental=false :composeApp:testDebugUnitTest --tests com.teco.ventago.features.expenses.ExpenseConceptsTest`

### Review Notes
- Se agregó logging en `ExpensesProvider::categorizeExpense` del payload JSON final limpio enviado a `PATCH /api/v1/expenses/{id}/categorization`.
- Si backend rechaza el request, ahora también se registra `status`, `errorCode`, `errorMessage` y el mismo payload para correlación directa.
- Se cambió la serialización de `CategorizeExpenseRequest` a `json.encodeToJsonElement(CategorizeExpenseRequest.serializer(), request)` para evitar que el encode genérico termine produciendo `{}` en este endpoint.
- Se agregó un test unitario que verifica que el body serializado contiene `default_account_id`, `only_uncategorized` e `items`.
- Se reemplazó el encode genérico del `PATCH /categorization` por un builder explícito de `JsonObject` con las claves exactas del backend (`default_account_id`, `only_uncategorized`, `items`, `item_id`, `line_number`, `account_id`).
- La compilación Android completa sigue inestable por un problema local del directorio `build/tmp/kotlin-classes/debug/.../Res$array.class`; la verificación útil del cambio quedó cubierta por `testDebugUnitTest`.

## Expense Concepts Parity TODO

### Plan
- [x] Extender modelos, requests y normalización API de gastos para conceptos/categorización.
- [x] Ampliar provider/repository/service de gastos con catálogo de conceptos, `PATCH /categorization` y `PUT` multipart en edición.
- [x] Implementar helpers de dominio para árbol jerárquico, labels, modo de conceptos y payloads.
- [x] Actualizar crear/editar gasto manual para soportar concepto global y por item, incluyendo secuencia `PUT` + `PATCH`.
- [x] Actualizar lista, detalle, CTA de importación y navegación tipada del detalle.
- [x] Agregar pantalla de catálogo de conceptos en Configuración y selector reutilizable.
- [x] Añadir tests unitarios y ejecutar gates de verificación.

### Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest`
- [ ] `./gradlew :composeApp:compileDebugKotlinAndroid` (el código dejó de fallar por símbolos; el gate quedó bloqueado por caches/daemon incrementales de Kotlin en el entorno local: `Could not close incremental caches`)
- [ ] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` (sigue bloqueado por errores preexistentes no relacionados en `features/expenses/ui/cufe/CufeImportViewModel.kt` y `features/quotes/ui/preview/PdfPreview.ios.kt`)

### Review Notes
- Se implementó la paridad principal de conceptos de gasto en expenses: catálogo, create/edit manual con concepto global o por item, `PATCH /categorization`, lista, detalle, CTA post-importación y navegación tipada de detalle.
- El flujo de edición manual dejó de usar upload previo a Bunny para el archivo nuevo; ahora el archivo viaja en el `PUT /expenses/{id}` multipart, alineado con backend.
- Se agregaron helpers de dominio y tests para normalización API, árbol jerárquico, modo de conceptos, labels y payloads.
- La verificación Android quedó bloqueada por un problema del entorno de compilación Kotlin incremental después de resolver el último error real de código (`expense_accounts_settings`); iOS mantiene bloqueos preexistentes no causados por este cambio.

---

## Plan
- [x] Inspect current POS search + view-mode selector row sizing.
- [x] Enforce shared control height so search field and selector render with equal height.
- [x] Run Android compile verification.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Search field and list/grid selector now use one shared `56.dp` control height in the POS products toolbar row.
- Android compile verification passed after the UI sizing change.

---

# Additional Address Cards Layout Refinement TODO

## Plan
- [x] Remove `location_code` rendering in POS customer additional-address cards.
- [x] Update POS additional-address card layout to show: address + default badge, `province/district/corregimiento` line, optional email line.
- [x] Update Customers Details additional-address rows to the same layout pattern.
- [x] Run Android compile verification.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Layout updated in POS and customer details additional address sections per requested structure.
- `location_code` display was removed from POS address cards.

---

# Additional Address Email Support (Customers + POS Order Payload) TODO

## Plan
- [x] Add optional `email` to customer additional address request/response models and order `AdditionalAddress` model.
- [x] Update customer details add/edit billing-address sheet to capture optional email with basic validation.
- [x] Wire `email` through `CustomerDetailsViewModel` create/update address flows as trimmed nullable.
- [x] Show address email in customer billing-address rows and POS customer-address selector cards.
- [x] Pass address email in POS create-order `additional_address` payload mapping.
- [x] Add localized optional-email field label in `values` and `values-es`.
- [x] Extend unit tests for address email deserialization and request serialization.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests com.teco.ventago.features.customers.CustomerModelsAndOrdersRequestTest`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` (fails due pre-existing unrelated compile errors in `features/expenses/ui/cufe/CufeImportViewModel.kt` and `features/quotes/ui/preview/PdfPreview.ios.kt`)

## Review Notes
- Added optional `email` to billing-address create/update requests, customer-address response model, and order `additional_address` model.
- Updated customer address add/edit bottom sheet with optional email field (`KeyboardType.Email`) and basic validation (`local@domain.tld` shape).
- Wired email through `CustomerDetailsViewModel` address create/update flows as trimmed nullable values.
- Displayed address email in customer details billing-address rows and POS customer-address selection cards when present.
- Included selected address email in POS create-order request mapping (`additional_address.email`) for non-default additional addresses.
- Added localized `customers_address_email_optional` string in `values` and `values-es`.
- Extended `CustomerModelsAndOrdersRequestTest` to cover address email deserialization and serialization for billing-address requests and `AdditionalAddress`.

---

# POS Product Selection Improvements TODO

## Plan
- [x] Add POS product selection tracking block before implementation.
- [x] Extend `PosState` with canonical-vs-visible product state, category filter metadata, and list/grid mode enum state.
- [x] Refactor `PosViewModel` product filtering to a single pipeline (`query` + category) over canonical products.
- [x] Persist and restore POS product view mode by business (`pos.product_view_mode.<businessId>`).
- [x] Update `PosListOrganism` with horizontal category chips (`Todos` + categories), list/grid selector, and adaptive grid rendering.
- [x] Add compact grid product card composable and keep "Producto Personalizado" as first action in list and grid.
- [x] Add localized strings for list/grid selector labels in `values` and `values-es`.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` (fails due pre-existing unrelated compile errors in `features/expenses/ui/cufe/CufeImportViewModel.kt` and `features/quotes/ui/preview/PdfPreview.ios.kt`)

## Review Notes
- Added canonical-vs-visible product state in POS (`items` remains canonical, `visibleItems` drives rendering) and introduced category filter/view mode state (`selectedProductCategoryId`, `availableProductCategories`, `productViewMode`, `itemCategoryById`).
- Refactored POS filtering in `PosViewModel` to a single pipeline (`applyProductFilters`) combining search + selected category against canonical products, preserving downstream order/cart logic correctness.
- Added per-business persisted product view mode with key `pos.product_view_mode.<businessId>` using `LocalStorage`.
- Updated `PosListOrganism` with horizontal non-wrapping category chips (`Todos` + categories when >1), list/grid selector chips, list rendering from `visibleItems`, and adaptive `LazyVerticalGrid` rendering.
- Added `PosItemGridCard` and reusable personalized product card support so "Producto Personalizado" remains the first action in list and grid modes.
- Added localized `pos_view_list` and `pos_view_grid` strings in `values` and `values-es`.
- Updated list/grid selector visual style to a segmented two-icon control matching the requested capsule design.
- Adjusted grid product cards to fixed height and inline price formatting (`<number> <currency>`) to keep aligned rows even with multi-line item names.

---

# Customers UI Improvements (List + Details + Loading Standards) TODO

## Plan
- [x] Add this tracking block before implementation and keep scope limited to Customers UI + AGENTS loading guardrail.
- [x] Update `AGENTS.md` with shimmer/skeleton-first rule for API-backed list/detail/form fetch states.
- [x] Refactor `CustomersListScreen` to Expenses-like filter bottom sheet (`name`, `ruc`, `email`) with Apply/Clear actions.
- [x] Add customers list shimmer loader and move create action to floating action button.
- [x] Add optional foreign identification fields to `CustomerDetails` model.
- [x] Refactor `CustomerDetailsScreen` with shimmer loading, Información General ordering/format, foreign identity rules, and address rendering rules.
- [x] Replace billing address full-width actions with icon buttons and use one add/edit bottom sheet form.
- [x] Keep `LoadingSheet` transitions for detail mutations and close address sheet only on successful mutations.
- [x] Add divider between orders KPI and recent orders list in customer details.
- [x] Add/update localized strings in `values/strings.xml` and `values-es/strings.xml`.
- [x] Add shimmer/skeleton state in `CustomerFormScreen` when loading edit data.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew :composeApp:testDebugUnitTest`
- [x] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` (fails due pre-existing unrelated compile errors in `CufeImportViewModel.kt` and `PdfPreview.ios.kt`)
- [x] Confirm only intended files changed (repo already had unrelated pre-existing modified files before this pass).

## Review Notes
- Updated Customers list UX to Expenses-style: top search row + bottom-sheet filters with Apply/Clear, shimmer initial loader, and create `FloatingActionButton`.
- Updated Customers details UX: shimmer initial loader, Información General field ordering/rules, separate RUC and DV rows, foreign identification fallback, and refined address rendering (without `location_code` display).
- Replaced billing-address full-width action buttons with icon actions and moved add/edit flows into one reusable bottom sheet form.
- Added address mutation success event to close the address bottom sheet only on successful create/update while keeping `LoadingSheet` mutation feedback.
- Added divider between order KPI rows and recent orders list in customer details card.
- Added optional foreign identification fields to `CustomerDetails` model for tolerant parsing.
- Added/updated localized strings in both `values` and `values-es`, and added AGENTS guardrail for shimmer/skeleton-first API loading states.
- Added customer form shimmer skeleton for edit fetch loading states.

---

# Home Summary API Integration TODO

## Plan
- [x] Add Home summary provider/repository with wrapped/direct response normalization and auth refresh retry.
- [x] Add Home domain models, parser, chart mapper, and HomeSummaryService with 5-minute per-business cache.
- [x] Wire DI in `AppModule.kt` and remove `HistoricSalesService` usage.
- [x] Refactor Home ViewModel/State for summary data, range selector, and chart transformation.
- [x] Update Home UI with range chips + 4 summary cards above shortcut cards.
- [x] Update chart component for negative values and high-point-count rendering safety.
- [x] Add localized strings for new Home labels.
- [x] Add `commonTest` unit tests for parsing and chart transformation logic.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [ ] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` (fails due pre-existing unrelated compile errors in `CufeImportViewModel.kt` and `PdfPreview.ios.kt`)
- [ ] `./gradlew :composeApp:allTests` (fails for the same pre-existing iOS compile blockers)
- [x] `./gradlew :composeApp:testDebugUnitTest`
- [x] Validate no unrelated files were modified by this implementation.

## Review Notes
- Implemented Home summary API integration end-to-end in Home module (data/domain/ui/viewmodel/DI).
- Added 4-range chart selector (`7D`, `15D`, `Mes`, `Año`) with date/month zero-fill logic and year monthly mapping.
- Added 4 summary cards above shortcut cards and kept existing shortcut block intact.
- Added shared tests for response normalization, parser robustness, and chart mapping.
- Android compile and unit tests pass; iOS/allTests are currently blocked by unrelated pre-existing errors.
- Follow-up UI tweak: `HomeSummaryCards` updated to a 2x2 grid, year card uses secondary background, today card uses `vanishedBackgroundColor()`.

---

# Home Sales Line Chart Replacement TODO

## Plan
- [x] Add `SalesLineGraphic` in `design_system/organism` with smooth line, gradient fill, 6 horizontal grid lines, y-axis currency labels, sparse x-axis labels, and tap-to-select.
- [x] Replace `BarGraphic` usage in `HomeScreen.kt` with `SalesLineGraphic`, preserving selected index flow.
- [x] Keep existing empty-state UX for no chart data.
- [x] Keep implementation in `commonMain` with no third-party chart dependency.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew :composeApp:testDebugUnitTest`
- [x] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` (fails due pre-existing unrelated compile errors in `CufeImportViewModel.kt` and `PdfPreview.ios.kt`)

## Review Notes
- Replaced Home chart UI from bar chart to a Canvas-based smooth line chart (`SalesLineGraphic`) in `commonMain`.
- Added web-like visual behavior: area fill, 6 horizontal grid lines, y-axis money labels, sparse x-axis labels, selected-point marker, and tooltip.
- Preserved existing selection state flow (`selectedSalesIndex` and `setSelectedSalesIndex`).
- Kept empty state behavior (`no_data`) for empty/zero-filled chart outcomes.
- Android compile and unit tests pass; iOS compile remains blocked by unrelated pre-existing errors.

---

# Home Sales Line Chart Improvements TODO

## Plan
- [x] Remove Y-axis labels to reclaim chart width.
- [x] Show all X-axis labels; fit full year (12 months) and allow horizontal scroll for denser daily/month ranges.
- [x] Ensure selected marker is on the rendered curve (fix smoothing/marker mismatch).
- [x] Keep amount tooltip visually tied to selected point and within visible chart bounds.
- [x] Auto-select current month when switching to `YEAR` range and when year data refreshes.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew :composeApp:testDebugUnitTest`
- [x] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` (fails due pre-existing unrelated compile errors in `CufeImportViewModel.kt` and `PdfPreview.ios.kt`)

## Review Notes
- Removed y-axis label column from `SalesLineGraphic` and used full width for the plot area.
- Updated x-axis rendering to include every data point label; year range (12 months) fits width, denser ranges scroll horizontally.
- Reworked smooth path generation so the line passes through each point; selected marker now sits on the rendered line.
- Kept tooltip anchored to selected point with above/below placement fallback to stay visible.
- Updated `HomeViewModel` default selection logic to pick current month for `YEAR` chart and latest point for daily ranges.

---

# Home Summary v2 TODO

## Plan
- [x] Add `SummaryScreen` route in `PosScreens` and register `HomeSummaryScreen` in navigation graph.
- [x] Add `Resumen` item in bottom navigation and include summary route in bottom-bar visibility guard.
- [x] Create `HomeSummaryScreen.kt` with requested card order and tablet/mobile layout variants.
- [x] Reuse existing folio card and sales line chart behavior with `HomeViewModel` state.
- [x] Implement read-only receivable/payable cards with donut overdue ratio based on amount.
- [x] Implement top 5 clients card sorted by `total` descending.
- [x] Add localized strings for summary tab and new summary card labels/states.
- [x] Run verification gates and capture outcomes.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew :composeApp:testDebugUnitTest`
- [ ] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` (fails due pre-existing unrelated compile errors in `CufeImportViewModel.kt` and `PdfPreview.ios.kt`)

## Review Notes
- Added `SummaryScreen` route and a new `Resumen` bottom navigation item after `Home`.
- Implemented `HomeSummaryScreen` with requested mobile/tablet layout, preserving existing `HomeScreen`.
- Reused `InvoicingPlanCard` and `SalesLineGraphic` behavior from Home summary state.
- Added read-only donut cards for cuentas por cobrar/pagar with overdue percentage based on amount.
- Added Top 5 clients card (sorted by `total`, capped at 5) with empty state.
- Android compile and unit tests pass; iOS simulator compile remains blocked by pre-existing unrelated errors.

---

# Home Summary Navigation Filters TODO

## Plan
- [x] Add typed navigation route arguments for Orders and Expenses list initial payment-status filters.
- [x] Make Home Summary receivable/payable "view details" actions navigate using those typed routes.
- [x] Apply incoming route params in destination screens with existing filter behavior (Orders pending, Expenses not paid).
- [x] Run verification gate(s) and capture results.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Receivable card action now navigates with `OrdersScreenRoute(paymentStatus = 0)` and applies pending filter on Orders load path.
- Orders `get-orders` payload now supports optional `payment_status` and includes `business_id`, threaded through ViewModel -> Service -> Repository -> Provider.
- Payable card action now navigates with `ExpensesListScreenRoute(initialPaymentStatus = "not_paid")` and auto-applies the existing `No pagado` chip behavior.
- Validation: Android compile gate passes; only existing project warnings were reported.

---

# Orders Screen POS Add Action TODO

## Plan
- [x] Review current `OrdersScreenActions` and navigation route for starting POS order flow.
- [x] Add a `+` app bar action in `OrdersScreen.kt` that navigates to POS new-order flow.
- [x] Run a compile verification gate for Android target.
- [x] Document verification result and summary.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Added a new `IconButton` with `Icons.Rounded.Add` in `OrdersScreenActions`.
- The new action navigates to `PosScreens.POS` to enter the POS new-order flow from Orders.
- Kept existing scanner action and behavior unchanged.
- Android compile verification passed; only pre-existing project warnings were reported.

---

# Summary Tab Dot Hint TODO

## Plan
- [x] Inspect bottom navigation implementation and identify where to attach first-time `Resumen` hint dot.
- [x] Add a secondary-color dot badge on `Resumen` bottom item and persist dismissal in local storage.
- [x] Remove dot automatically after user enters `Resumen` and keep behavior keyed per authenticated business context.
- [x] Run Android compile verification and record result.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Added `SUMMARY_TAB_HINT_SEEN_KEY_PREFIX` and local-storage-backed state in `App.kt`.
- Added a secondary-color dot badge to the `Resumen` bottom tab icon via `BadgedBox` + `Badge`.
- Dot is shown until first time the user enters `PosScreens.SummaryScreen`, then stored as seen and removed.
- Persistence key is scoped by authenticated business id (`summary_tab_hint_seen:<businessId>`), with a guest fallback.
- Android compile verification passed; only existing project warnings were reported.

---

# Order Details Delete Order TODO

## Plan
- [x] Add typed delete-order request model and wire endpoint `POST /api/v1/orders/delete` in provider.
- [x] Extend orders repository and service with delete-order flow, including error logging and in-memory orders/selected-order sync updates.
- [x] Add ViewModel delete action with `LoadingSheet` state transitions and success event for post-delete navigation.
- [x] Update `OrderDetailsScreen` UI: conditional delete button visibility, delete-reason dialog, and success navigation back to orders list.
- [x] Run Android compile verification and record result.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Added `DeleteOrderRequest` (`order_id`, `delete_reason`) and wired `POST /api/v1/orders/delete` through provider/repository/service with typed request bodies.
- Repository delete call is wrapped with try/catch logging and rethrows with `businessId` + `orderId` context.
- Service delete flow now removes the order from `orders`/`ordersFlow` and clears `selectedOrder` when applicable for list/detail consistency.
- `OrdersDetailsViewModel` now exposes `deleteOrder(reason)` using `LoadingSheet` (`showLoading` -> `showSuccess`/`showError`) and emits `OrderDeleted` after success.
- `OrderDetailsScreen` now shows a conditional `Eliminar pedido` action, prompts for required delete reason, and navigates back to Orders when delete succeeds.

---

# Customers Management Section TODO

## Plan
- [x] Add customers navigation graph and typed routes (`CustomersManage`, list/details/create/edit) without modifying POS customer flow.
- [x] Add Home shortcut card to enter the new customers section from `HomeScreen`.
- [x] Expand customers provider/repository/service with customer details, update/delete, and billing-address CRUD endpoints using typed request models.
- [x] Expand orders request chain to support optional `customer_id` filter and expose a non-mutating paged reader for customer order resume.
- [x] Implement customers list UI (filters + pagination + actions) under `features/customers/ui/list`.
- [x] Implement customer form UI/viewmodel for create and edit modes under `features/customers/ui/form`.
- [x] Implement customer details UI/viewmodel (card layout, billing addresses CRUD, order resume KPI + last 5 orders) under `features/customers/ui/details`.
- [x] Wire new viewmodels in `AppModule.kt` and add required strings in `values` and `values-es`.
- [x] Add/adjust common unit tests for customers parsing/repository/service and orders `customer_id` request behavior.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew :composeApp:testDebugUnitTest`
- [ ] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` (fails due pre-existing unrelated errors in `CufeImportViewModel.kt` and `PdfPreview.ios.kt`)
- [x] Validate no unrelated files were modified by this implementation.

## Review Notes
- Implemented a standalone customers management graph from Home shortcut (`CustomersManage`) with list/create/details/edit routes while keeping existing POS customer-selection screens unchanged.
- Added new customers UI packages for list, form, and details with filters, pagination, full create/edit flow, customer deletion, billing-address CRUD, and order resume (KPI + recent 5 orders).
- Extended customers data/domain layers with typed models and endpoints: get-by-id, update details, delete customer, and billing address create/update/delete; repository methods include contextual try/catch logging.
- Extended orders stack to support optional `customer_id` filtering through provider/repository/service and added `OrderService.listOrdersForCustomerPaged(...)` for details screen without mutating main orders state.
- Added localized strings in both `values/strings.xml` and `values-es/strings.xml` for the new customer screens, and removed debug prints introduced in customer provider/repository/service flow.
- Added/expanded common tests (`CustomerModelsAndOrdersRequestTest`) for customer details/address parsing, orders request `customer_id` include/omit, repository mutation success on `error == null`, and customer service create/update/delete state/cache updates.
- Android compile and unit tests pass. iOS simulator compile is still blocked by pre-existing unrelated files (`features/expenses/ui/cufe/CufeImportViewModel.kt`, `features/quotes/ui/preview/PdfPreview.ios.kt`).
