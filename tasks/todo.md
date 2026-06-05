# Customer Foreign Country List TODO

## Plan
- [x] Replace the truncated foreign-customer country dropdown with the full provided country list.
- [x] Add focused regression coverage for the customer form country catalog.
- [x] Run targeted verification and document results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.customers.CustomerModelsAndOrdersRequestTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause: `CustomerFormState.countryOptions` was hardcoded to five countries, so the foreign-customer country dropdown could not expose the full backend-accepted country list.
- Added `CustomerCountries.options` with the 219-entry provided catalog and wired the customer form state default to that list.
- Added a customer regression test that verifies the full catalog size and representative countries near the beginning/end of the list.
- Verification passed with existing Gradle warnings about KSP version, cinterop commonization, expect/actual beta, and deprecations.

# Invoice Bottom Note Settings TODO

## Plan
- [x] Add bottom-note settings API models, provider, repository, service, and LocalStorage cache.
- [x] Add Settings UI to create/update/delete "Texto predeterminado para facturas" with title/body/include defaults.
- [x] Add POS Additional information checkbox in the commercial information section with cache-first/background-refresh behavior.
- [x] Extend create-order payload with nullable `include_bottom_note`.
- [x] Add focused tests and run compile/unit verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.invoicing.BottomNoteSettingsServiceTest --tests com.teco.ventago.features.orders.CreateOrderRequestTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Added invoicing bottom-note settings integration for `GET/POST/PUT/DELETE /api/v1/invoicing/settings/bottom-note`, with business-scoped `LocalStorage` cache and a shared observable service.
- Settings now shows `Preferencias de Facturación` with `Texto predeterminado para facturas`, opening a rich-text modal for title/body/include default plus delete.
- POS Additional information now shows an `Información comercial` checkbox only when a complete bottom-note config is available and the latest refresh did not fail.
- Order creation now sends nullable `include_bottom_note`; refresh failure or unavailable config sends `null`.
- Focused service/cache and request serialization tests passed, and KMP/Android compile verification passed with existing project warnings.
- Correction: bottom-note body is sanitized before saving so rich editor helper markup like `ql-ui` spans, `data-list`, and `contenteditable` is not sent to the backend.
- Correction: bottom-note save now reads the current rich editor HTML at click time and keeps body sync active across multiple edits in one focus session, preventing stale body payloads.

## Correction Notes
- [x] Sanitize rich editor helper markup before saving bottom-note body so backend receives clean HTML without `ql-ui` spans or `data-list` attributes.
- [x] Fix bottom-note rich editor synchronization so saving after multiple body edits sends the latest editor HTML, not the previous ViewModel value.
- [x] Move the default include control out of the bottom-note editor sheet, show it as a settings-card switch above the edit button, and use a switch instead of a checkbox in the POS commercial information section.
- [x] Adjust bottom-note settings UI: move the switch below the configure action, save switch changes immediately with a local skeleton loader, show GET skeleton loading, disable the switch with no configured note, add body placeholder copy, and use secondary color for save/switch controls.

# Invoicing Customer Address Preference TODO

## Plan
- [x] Add general invoicing settings API models and provider/repository/service methods for customer-address invoice visibility.
- [x] Add Settings ViewModel state and immediate PUT handling with loading/rollback behavior.
- [x] Render the address switch in the existing `Preferencias de Facturación` card with requested copy and secondary switch styling.
- [x] Add focused service coverage and run verification gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.invoicing.BottomNoteSettingsServiceTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Added `GET /api/v1/invoicing/settings` and `PUT /api/v1/invoicing/settings/include-address` integration through provider, repository, service, and business-scoped LocalStorage cache.
- Settings now renders `Incluir dirección del cliente en la factura` in the existing `Preferencias de Facturación` card with the requested help text, secondary switch styling, GET skeleton, and PUT skeleton/rollback behavior.
- Focused service tests cover cache hydration and the explicit update path for `include_address_on_invoice`.

# Branch Logo and Trade Name TODO

## Plan
- [x] Extend branch domain parsing with `trade_name` and `logo_url`.
- [x] Add branch logo upload/delete API integration through provider, repository, and service.
- [x] Update Branches UI to show trade names, logo status/actions, upload/delete loading, and confirmation.
- [x] Apply branch-first logo fallback to POS invoice preview.
- [x] Add focused tests and run compile verification.

## Verification Gates
- [ ] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `Branch` now parses and serializes optional `trade_name` and `logo_url` without breaking payloads that omit them.
- Branch logo upload/delete flows call `/api/v1/invoicing/branches/{branchCode}/logo` with `X-Business-ID`, use multipart upload, and reload branches after successful mutations.
- Branches UI now shows trade name, branch code, logo status, add/replace/view/delete actions, inline logo loaders, and Spanish snackbar/confirm copy.
- POS invoice preview now resolves document logo as branch logo, then business logo, then empty fallback, and displays selected branch identity.
- Focused verification passed: `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.branches.BranchModelTest --tests com.teco.ventago.features.branches.BranchServiceLogoTest --tests com.teco.ventago.features.pos.InvoicePreviewBuilderTest`.
- Full `:composeApp:testDebugUnitTest` is currently blocked by existing unrelated failure `AuthzNavigationTest.subUserBottomNavOmitsSummaryAndUnauthorizedSections` at `AuthzNavigationTest.kt:65`.

# Config Summary Active Subscriptions Response TODO

## Plan
- [x] Update the financial-profile DTOs for `invoice_plan.active_subscriptions[]` while retaining legacy cached date fields.
- [x] Derive the existing Home aggregate folio-plan dates from the earliest activation and latest expiry dates.
- [x] Remove temporary config-summary debug prints and refresh the payment replication contract example.
- [x] Add focused parsing regression tests for the new endpoint payload and legacy cached payload.
- [x] Run targeted unit and KMP/Android compile verification gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.financialProfile.FinancialProfileParsingTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `InvoiceSummary` now decodes `active_subscriptions[]`, keeps optional legacy cache dates, and exposes aggregate date helpers for the earliest activation and latest expiry.
- Home keeps its existing aggregate folio-plan card and now uses the aggregate date helpers without changing UI layout.
- Config-summary payment defaults remain intact: omitted `pending_charges` is `0`, string fee amounts decode to cents, and omitted ACH `enabled` remains `true`.
- Temporary config-summary `ASDASD` prints were removed while repository failures still log structured context including `businessId`.
- Focused parser regression test and KMP/Android compile gate passed with existing project warnings (`ksp` version, cinterop commonization, expect/actual beta, deprecations).

# Add Customer Validation Feedback TODO

## Plan
- [x] Document the add-customer regression caused by silent validation failure after the address rule change.
- [x] Keep the minimum-address rule, but surface explicit submit-level feedback so the add button no longer appears dead.
- [x] Run focused verification and capture the lesson learned.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.pos.ui.customer.add.viewmodel.AddCustomerValidatorsTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause: `createCustomer()` already blocked invalid submits, but after tightening the address rule the screen still gave no submit-level feedback, so the primary CTA looked dead when validation failed.
- The add-customer state now carries a `validationMessage`, the ViewModel sets `Revisa los campos marcados en rojo para continuar.` on failed submit, and that message clears on subsequent edits.
- Both reduced and full add-customer variants render the validation message immediately above the primary button, while preserving field-level errors and the 5 non-blank character address rule.
- Verification passed after the usual local cache cleanup for this workspace (`./gradlew --stop`, remove `composeApp/build/kspCaches`, `composeApp/build/generated/ksp`, and `composeApp/build/tmp/kotlin-classes/debug`), then rerun the focused test and compile gates.

# Add Customer Address Minimum Length TODO

## Plan
- [x] Require address input to contain at least 5 non-blank characters in add-customer validation for invoicing.
- [x] Keep the existing field-level error rendering on `AddCustomerScreen`.
- [x] Add a focused validator test for blank, short, and valid address inputs.
- [x] Run focused test and KMP/Android compile verification and record the result.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.pos.ui.customer.add.viewmodel.AddCustomerValidatorsTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Address validation now requires at least 5 non-blank characters, so whitespace-only padding cannot satisfy the field.
- The existing `addressLineError` path remains the single UI error source for both reduced and full add-customer forms.
- Added `AddCustomerValidatorsTest` to cover null, blank, short, and valid address values.
- Verification passed. A stale local KSP/Kotlin build cache had to be cleared first (`composeApp/build/kspCaches`, `composeApp/build/generated/ksp`, `composeApp/build/tmp/kotlin-classes/debug`), then both Gradle gates succeeded with existing project warnings.

# Backend Session ID Header TODO

## Plan
- [x] Add shared session-id service with SecureStorage-backed persistence, 3-day sliding TTL, forced rotation, and clear behavior.
- [x] Append `X-SESSION-ID` to VentaGo backend requests only through the shared Ktor client interceptor.
- [x] Rotate the session id before login/register backend calls and clear it on logout/account deletion.
- [x] Add focused common tests for id format, reuse, expiry, TTL refresh, rotation, clearing, and backend URL matching.
- [x] Run targeted unit and KMP/Android compile verification gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.core.session.SessionIdServiceTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Added `SessionIdService` with a SecureStorage-backed store, 32-character alphanumeric UUID-derived ids, 3-day sliding TTL refresh, forced rotation, and clear support.
- The shared Ktor client now appends `X-SESSION-ID` only for parsed `Configs.serverBasePath` and `Configs.ordersBasePath` origins, avoiding external uploads and lookalike hosts.
- Login/register flows rotate the session before their backend request; logout and successful account deletion clear it, including finally-style cleanup paths.
- Focused session tests cover id format, reuse, TTL refresh, expiry replacement, forced rotation, clearing, and backend URL matching.
- Verification passed with existing project warnings (`ksp` version, cinterop commonization, expect/actual beta, deprecations).

# Orders Yesterday Filter Payload TODO

## Plan
- [x] Make quick emission-date chips submit date-only filters so `Yesterday` matches the web request payload.
- [x] Centralize Orders list request serialization/date formatting so the exact API payload can be tested.
- [x] Add a focused serialization test for the requested May 7, 2026 payload.
- [x] Run targeted test and compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.orders.ListOrdersRequestTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Quick date chips now execute immediately as date-only searches, clearing payment status, invoice type, customer RUC, and customer ID filters before refreshing orders.
- `ListOrdersRequest.toApiJsonString()` omits null filters and preserves default pagination fields, matching the web payload shape.
- `ListOrdersRequestTest.yesterdayDateOnlyRequestMatchesWebPayloadShape` verifies the exact requested payload for `2026-05-07T00:00:00-05:00` through `2026-05-07T23:59:59-05:00`.
- Verification passed with existing project warnings (`ksp` version, cinterop commonization, expect/actual beta, deprecations).

# Orders List Filters TODO

## Plan
- [x] Review existing Orders and Quotes list filtering flows plus project architecture/conventions.
- [x] Replace the Orders list API body for `get-orders` with a typed request model covering page, date range, invoice type, customer RUC, and payment status.
- [x] Thread the typed filters through provider, repository, service, and `OrdersViewModel` without changing unrelated order flows.
- [x] Add Orders list filter UI: payment status chips, filter sheet, invoice type selector, customer RUC, calendar-backed start/end dates, and quick date range chips.
- [x] Run focused compile verification and document results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Added a typed `ListOrdersRequest` for `/api/v1/orders/get-orders`; null filter fields are omitted in the serialized list payload.
- Orders filters now flow through provider, repository, service, and `OrdersViewModel` with existing pagination preserved.
- Orders list now shows payment-status filter chips and a filter sheet for invoice type, customer RUC, emission start/end dates, and quick ranges: today, yesterday, this week, this month, and last 30 days.
- Compile verification passed with existing project warnings (`ksp` version, cinterop commonization, expect/actual beta, deprecations).

# POS Success Home Tap Regression TODO

## Plan
- [x] Fix the success-screen home icon tap target so the visible icon receives clicks.
- [x] Keep the existing direct Home navigation behavior unchanged.
- [x] Run KMP/Android compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause: the full-screen scrollable `Column` was composed after the home `IconButton`, so it could sit above the visible icon in hit-test order.
- The home `IconButton` is now composed after the scroll content inside the `Box`, preserving the visual position while making the tap target active.
- Compile gate passed with existing project warnings (`ksp` version, cinterop commonization, manifest provider replacement, expect/actual beta, deprecations).

# POS Success Home/Icon Follow-up TODO

## Plan
- [x] Route the success-screen home icon directly to the Home screen.
- [x] Switch the success-screen home, client, RUC, and email icons to outlined variants.
- [x] Run KMP/Android compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Success-screen home now navigates directly with `navController.navigate(PosScreens.HomeScreen.name)` and clears the current stack so the icon exits POS reliably.
- Home, client, RUC, and email icons now use `Icons.Outlined`.
- Compile gate passed with existing project warnings (`ksp` version, cinterop commonization, manifest provider replacement, expect/actual beta, deprecations).

# POS Success Screen Follow-up Fixes TODO

## Plan
- [x] Show compact POS order numbers on success cards (`#0000000521` instead of the full prefixed order number).
- [x] Use a document-style icon for RUC rows.
- [x] Fix invoice sharing so Android uses a real send/share PDF intent.
- [x] Make the top home icon navigate to Home instead of starting another POS sale.
- [x] Prevent order details auto-open from re-triggering after returning to the orders list.
- [x] Run KMP/Android compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Success order display now strips prefixed order numbers to their final segment, e.g. `ORD-4-000-865-0000000521` renders as `#0000000521`.
- RUC rows now use the document icon treatment.
- The top success home icon now resets POS state and navigates to `HomeScreen`; `Hacer otra orden` still returns to POS for a new sale.
- `OrdersScreenRoute(orderNumber=...)` now consumes the initial order-number auto-open once per back stack entry, preventing detail re-open loops after pressing back from details.
- Android PDF sharing now uses `ACTION_SEND` with `EXTRA_STREAM`, `ClipData`, and URI grants, instead of trying to share through an `ACTION_VIEW` intent.
- Compile gate passed with existing project warnings (`ksp` version, cinterop commonization, manifest provider replacement, expect/actual beta, deprecations).


# POS Success Screen UI Remake TODO

## Plan
- [x] Keep existing POS order creation, invoicing, PDF, payment-link, notification, and navigation logic unchanged.
- [x] Replace the successful mobile/tablet content in `SuccessScreen.kt` with the requested friendly card-based invoice and payment-link variants.
- [x] Use existing `PosViewModel` state for order number, amount, customer details, payment method totals, PDF, and payment link data.
- [x] Run KMP/Android compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `SuccessScreen` now renders a friendly success hero on `vanishedBackgroundColor()` with a centered card layout for both phone and tablet success flows.
- Invoice success shows order amount/detail navigation, optional customer rows, payment method plus paid amount in the same summary card, a PDF invoice card action, a secondary `Hacer otra orden` CTA, and an outlined share action.
- Payment-link success shows order/customer details, QR generation from the existing payment link, copy and WhatsApp actions, a 24-hour expiry notice, a secondary new-order CTA, and an outlined link share action.
- Failed order handling still delegates to the existing `PosSuccessScreen` failure UI, and notification permission logic remains scoped to non-failed successful results.
- `PosViewModel.sharePdfDocument()` was added as a small wrapper around the existing `PdfSharer.sharePdf` dependency so the invoice share CTA can share the generated PDF without touching order creation or invoicing logic.
- Compile gate passed with existing project warnings (`ksp` version, cinterop commonization, expect/actual beta, deprecations).

# Order Cancel Reason Minimum Length TODO

## Plan
- [x] Add a shared cancel-order reason minimum length rule for order details.
- [x] Block the `Anular pedido` confirmation until the trimmed reason has at least 15 characters and show field-level guidance.
- [x] Guard the ViewModel cancel path with the same validation to prevent programmatic bypass.
- [x] Add focused validator tests for blank, short, and valid reasons.
- [x] Run targeted test and KMP/Android compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.orders.ui.order_details.viewModel.OrdersDetailsViewModelCxcTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Cancel-order reason validation now uses a shared 15-character trimmed minimum rule.
- The cancel-order bottom sheet shows minimum-length guidance, marks short reasons as an input error, and disables `Anular` until valid.
- `OrdersDetailsViewModel.cancelOrder(...)` trims the reason and rejects blank/short reasons before opening `LoadingSheet`, covering programmatic calls.
- Targeted validator test and KMP/Android compile gates passed with existing KSP/Kotlin, cinterop commonization, and deprecation warnings.

# Customers List Search UX TODO

## Plan
- [x] Show a secondary-colored circular loader in the top search icon button while search/filter refresh is running.
- [x] Add a filter-specific RUC label so add/edit customer forms keep their existing tax-identification label.
- [x] Run focused compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Top search action now displays a 22.dp secondary `CircularProgressIndicator` while `applyFilters()`/reset refresh is loading.
- Customer filter sheet now uses `customers_filter_ruc_optional` (`RUC (opcional)` / `RUC (Optional)`) instead of the shared add/edit customer fiscal-identification label.
- Compile verification passed with existing KSP/Kotlin, cinterop commonization, manifest, and deprecation warnings.

# POS Customer Picker Redesign TODO

## Plan
- [x] Update `CustomersListScreen` so callers can receive the full `CustomerListItem` while preserving customer-management navigation.
- [x] Wire POS customer picker routes to the reusable customers list UI with FAB creation and saved-state return to POS.
- [x] Preserve the existing invoice-customer guard for POS selection when invoicing is enabled.
- [x] Run focused compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- POS `SearchCustomerScreen` and the compatible `CustomersScreen` route now render `CustomersListScreen` directly.
- Customer rows now return the full `CustomerListItem`; the customer-management graph still opens details using `customer.id`.
- POS selection writes the serialized customer into the POS graph saved state and pops back to `POSScreen`.
- POS picker routes use the existing FAB to open `AddCustomerScreen`; the old app-bar add action was removed from these routes.
- Compile verification passed with existing KSP/Kotlin, cinterop commonization, and deprecation warnings.

# POS Invoice Preview Screen TODO

## Follow-up: Secondary Section Headers
- [x] Set invoice preview section icons and titles to `MaterialTheme.colorScheme.secondary`.
- [x] Run a focused compile verification.

## Follow-up: Secondary Bold Totals
- [x] Set bold ITBMS breakdown total text to `MaterialTheme.colorScheme.secondary`.
- [x] Set final invoice total label and amount to `MaterialTheme.colorScheme.secondary`.
- [x] Run a focused compile verification.

## Follow-up: Preview Button Position
- [x] Move the invoice preview button under the `Confirmar cobro` primary action.
- [x] Run a focused compile verification.

## Plan
- [x] Add a pure invoice preview model/builder from current POS state and business data.
- [x] Add a full-screen POS invoice preview route that shares the existing POS ViewModel.
- [x] Add an optional `Vista previa` action on the payment step without changing payment confirmation behavior.
- [x] Render the mobile Compose preview with banner, DGI header, issuer/receptor/meta, items, ITBMS, payments, and totals.
- [x] Add focused common tests for preview model data, totals, tax grouping, and payments.
- [x] Run KMP/Android compile and targeted test verification gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.pos.InvoicePreviewBuilderTest`

## Review Notes
- Follow-up: invoice preview now renders beneath the primary payment action in manual/installment and payment-link sections.
- Follow-up: bold ITBMS breakdown total and final invoice total now use `MaterialTheme.colorScheme.secondary`.
- Follow-up: invoice preview section icons and titles now use `MaterialTheme.colorScheme.secondary`.
- Added a local-only POS invoice preview builder and full-screen Compose preview route sharing the POS graph ViewModel.
- `PaymentScreen` now exposes an optional `Vista previa` action without changing confirm, draft, or payment-link submission paths.
- Preview renders issuer, receptor, fiscal metadata, line items, ITBMS breakdown, payments, and totals from current POS state.
- Verification passed with existing project warnings about KSP/Kotlin version, cinterop commonization, and deprecated APIs.

# Payment Methods Channels Responsive Layout TODO

## Plan
- [x] Inspect the current channels row and relevant project UI conventions.
- [x] Redesign available channel rows so title text, status, fee, and chevron cannot overlap on narrow screens.
- [x] Keep the change scoped to `PaymentMethodsScreen.kt` and preserve existing navigation/configuration behavior.
- [x] Run the KMP/Android compile verification gate.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `ChannelRow` now separates channel identity from metadata: logo/name/subtitle stay in the primary content column, while configured/commission pills render in a wrapping `FlowRow` below the title.
- The chevron has a fixed tap/visual slot, so it no longer competes with the badges for horizontal space.
- Visible rows are filtered before rendering so dividers only appear between rendered channels.
- Compile gate passed with existing project warnings only.

# POS Payment Link Badge TODO

## Plan
- [x] Move the `Nuevo` badge out of the Material `BadgedBox` overlay so it cannot cover the payment-link tab text.
- [x] Keep the badge visible only for `showPaymentLinkNewBadge`.
- [x] Verify the POS UI compile gate.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- POS payment-link tab now renders `Nuevo` inline after the label with spacing instead of using `BadgedBox`, preventing overlay on the tab text.
- Compile gate passed with existing project warnings only.

# Settings Quotes Collapse TODO

## Plan
- [x] Make the Quotes settings card collapsible and default it collapsed.
- [x] Use the existing iOS disclosure arrow to indicate collapsed/expanded state.
- [x] Change only the `Pagos y cobros` new badge to use secondary color.
- [x] Verify the Compose/KMP compile gate.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `QuoteSettingsSection` now renders the quote settings card collapsed by default with an iOS disclosure arrow that rotates when expanded.
- `SettingsTextButton` accepts an optional `badgeColor`; `Pagos y cobros` passes `MaterialTheme.colorScheme.secondary` for the `Nuevo` badge.
- Compile gate passed with existing project warnings only.

# Payments + Settings + Notifications Replication TODO

## Iteration 6 Payment Link Confirmed Order Facturar Visibility (Current)

## Plan
- [x] Confirm why order `3835` does not show `Facturar` in Order Details.
- [x] Extend the existing invoice action eligibility only for unpaid, not-invoiced confirmed `payment_link` orders.
- [x] Preserve existing permission, invoice-status, payment-status, and positive-total guards.
- [x] Verify compile gate for KMP/Android.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause: `OrdersDetailsViewModel.canInvoiceDraftOrder(...)` only allowed `OrderStatus.DRAFT`; order `3835` is `OrderStatus.CONFIRMED` with `payment_flow_type = payment_link`, no invoice status, unpaid, and positive total.
- Eligibility now also allows confirmed `payment_link` orders when they are not invoiced (`NONE`/`PENDING`), unpaid, have positive total, and the user still has `canMarkPaid`.
- The button rendering in `OrderDetailsScreen` remains unchanged; only the ViewModel eligibility rule was broadened.

## Iteration 6 Order Cancel Bottom Sheet (Current)

## Plan
- [x] Replace the cancel order AlertDialog with a ModalBottomSheet.
- [x] Preserve the existing cancel reason state and `viewModel.cancelOrder(...)` callback.
- [x] Use existing design-system fields/buttons and contextual destructive styling.
- [x] Verify compile gate for KMP/Android.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `OrderDetailsScreen` now opens cancellation as a bottom sheet using the existing `showCancelDialog` state.
- `CancelOrderBottomSheet` keeps the same reason input and confirms through `viewModel.cancelOrder(cancelReason.trim())`.
- The sheet uses destructive styling, a warning panel, and existing `DMOutlinedTextField`, `OutlinedButtonM`, and `ButtonM` components.

## Iteration 6 Order Details Visual Refresh (Current)

## Plan
- [x] Apply secondary-tinted circular icon treatment to Order Details cards.
- [x] Highlight the header total row with a soft secondary container and secondary amount text.
- [x] Make the items card collapsible, default expanded, with arrow status icon.
- [x] Separate primary invoicing actions from secondary/destructive actions with an action divider.
- [x] Convert lower actions to outlined buttons with contextual colors and icons while preserving existing visibility logic.
- [x] Verify compile gate for KMP/Android.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `OrderDetailsScreen` now uses a shared `CardSectionIcon` treatment: secondary icon inside a circular `vanishedBackgroundColor()` container for detail cards.
- The header total row now renders as a soft secondary-tinted pill with the amount in secondary color.
- The items card is collapsible and defaults expanded, with up/down arrow state feedback.
- Order actions are visually split: primary invoicing actions remain above `Acciones del pedido`, while additional actions render as outlined contextual buttons with icons.
- Existing conditions and callbacks for rendering/action behavior were preserved.

## Iteration 6 ACH Proof Download Save-to-Device (Current)

## Plan
- [x] Replace ACH proof download behavior that relies on external viewers with native save-to-device behavior.
- [x] Add platform-specific persistence path: image proofs to gallery/photos and PDF/files to documents/downloads.
- [x] Route ACH proof download success handler by MIME type and show explicit save-result feedback.
- [x] Verify compile gate for KMP/Android.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `PdfSharer` now exposes explicit save APIs (`saveImageToGallery`, `saveFileToDocuments`) so ACH proof download does not depend on installed viewer apps.
- Android implementation writes bytes directly into `MediaStore` (`Pictures/VentaGo` for images, `Downloads/VentaGo` for documents).
- iOS implementation saves images to Photos and files to app Documents, avoiding viewer-based fallback for ACH proof downloads.
- `OrdersDetailsViewModel.handleAchProofDownloadSuccess(...)` now always persists locally by MIME type and shows success/error snackbar accordingly.

## Iteration 6 Void Action Hidden for Automatic Payments (Current)

## Plan
- [x] Hide `Anular pago` action for payments with `is_automatic = true`.
- [x] Add defensive ViewModel guard to block void sheet opening for automatic payments.
- [x] Verify compile gate for KMP/Android.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- En `RegisteredPaymentsCard`, la acción `Anular pago` ahora requiere explícitamente `!payment.isAutomatic`.
- En `OrdersDetailsViewModel.openVoidPaymentSheet`, se agregó guardia para retornar sin abrir sheet cuando el `paymentId` corresponde a un pago automático.

## Iteration 6 Cancelled Order Payments Visibility (Current)

## Plan
- [x] Ensure `Pagos registrados` card is rendered even when order status is `CANCELLED`.
- [x] Keep existing action guards unchanged for cancelled orders.
- [x] Verify compile gate for KMP/Android.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Se movió el render de `RegisteredPaymentsCard` fuera del bloque `order.status != CANCELLED`, para que los pagos se muestren también en órdenes anuladas.
- Las acciones de orden (`Facturar`, `Anular`, etc.) se mantienen bloqueadas para órdenes canceladas, sin cambios funcionales.

## Iteration 6 Order 3796 Partial Auto-Payment Actions (Current)

## Plan
- [x] Fix manual payment sheet total for `Facturar` to use outstanding balance instead of full order total.
- [x] Hide `Eliminar pedido` when the order already has an automatic non-voided payment.
- [x] Verify compile gate for KMP/Android.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `OrderScreenManualPaymentBottomSheetHost` ahora usa `uiState.manualPayment.totalToChargeCents` (fallback a total de orden solo si viene en 0), evitando que una orden parcialmente pagada pida el monto completo al facturar.
- `canDeleteOrder(order)` ahora retorna `false` si existe al menos un pago automático no anulado con monto neto positivo (`charged - refunded > 0`), por lo que el botón `Eliminar pedido` deja de mostrarse en ese escenario.

## Iteration 6 Order Details Generate Link Sheet UX (Current)

## Plan
- [x] Prefill `Monto a cobrar` with current pending balance when opening generate-link sheet.
- [x] Update generate action button to secondary color in sheet.
- [x] Verify compile gate for KMP/Android.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `openGeneratePaymentLinkSheet()` ahora prellena `amountInput` con saldo pendiente (`totalOpenReceivableCents`) formateado como decimal de 2 dígitos.
- En el modal, el botón `Generar` usa `MaterialTheme.colorScheme.secondary` + `onSecondary` para alinearse al guideline visual.

## Iteration 6 Order 3796 Generate Link Eligibility (Current)

## Plan
- [x] Reproduce and isolate why `canGeneratePaymentLink` blocks draft order `3796` while web allows generation.
- [x] Fix open-balance computation for orders without `receivable_terms` using `total_amount - net_paid` fallback.
- [x] Add focused unit tests for fallback scenarios (no terms, partial payments, voided payments).
- [x] Verify with focused tests + compile gate.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests '*OrderReceivableResolverTest*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause confirmado: `totalOpenReceivableCents(...)` solo sumaba `receivable_terms.open_amount`; cuando el backend no envía `receivable_terms` (caso orden `3796`), el saldo quedaba en `0` y ocultaba `Generar Link de Pago`.
- Se agregó `OrderReceivableResolver.totalOpenCents(...)` con fallback cuando no hay `receivable_terms`: `max(total_amount - pagos_netos_no_anulados, 0)`.
- `OrdersDetailsViewModel.totalOpenReceivableCents(...)` ahora delega en el resolver, por lo que el botón vuelve a mostrarse para órdenes abiertas sin términos CxC.
- Se añadieron tests para fallback sin términos, parcial, ignorar pagos anulados y prioridad de términos cuando existen.

## Iteration 6 Order Details Copy Link on `requires_action` (Current)

## Plan
- [x] Reproduce/validate why `Copiar Link de Pago` is hidden for order `3797` with `payment_links.status=requires_action`.
- [x] Fix `OrderDetails` visibility rule so copy/share uses open-link semantics (non-terminal) instead of only active-link.
- [x] Add regression coverage for `requires_action` link classification (`open=true`, `active=false`).
- [x] Verify with focused tests + compile gate.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests '*PaymentLinkResolverTest*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause confirmado: `canCopyOrSharePaymentLink()` estaba atado a `hasActiveLink`, por lo que links en estado `requires_action` no habilitaban `Copiar Link de Pago` aunque fueran enlaces abiertos válidos.
- Fix aplicado en ViewModel: `canCopyOrSharePaymentLink()` ahora usa `PaymentLinkResolver.hasOpenLink(...)` (manteniendo la exclusión para órdenes pagadas).
- Se agregó test de regresión (`treatsRequiresActionAsOpenButNotActive`) para blindar clasificación de estado `requires_action` y evitar regresiones de visibilidad.

## Iteration 6 Order Details Copy Payment Link Visibility (Current)

## Plan
- [x] Fix payment-link resolver to choose the right link candidate (status/source priority + most recent timestamp per source).
- [x] Ensure `hasActiveLink/hasOpenLink` evaluate across all available sources (`payment_links`, `payment_link`, `links`) instead of a single resolved item.
- [x] Add focused unit tests for mixed-status/mixed-source payment-link payloads.
- [x] Verify with focused tests + compile gate.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests '*PaymentLinkResolverTest*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `PaymentLinkResolver.resolveCurrent(...)` dejó de depender del primer elemento de lista: ahora prioriza link activo por fuente y escoge el más reciente por `created_at` en `payment_links`/`links`.
- `hasPendingLink/hasActiveLink/hasOpenLink` ahora evalúan sobre todas las fuentes, evitando falsos negativos cuando el primer candidate es terminal.
- Se añadieron tests para:
- selección del link más reciente en `payment_links`,
- detección de link abierto/activo en `links` aunque `payment_links` traiga terminal.

## Iteration 6 Payment Link Config Check UX (Current)

## Plan
- [x] Remove blocking `config-summary` refresh on every click of `Crear enlace de pago` tab and `Generar enlace` button.
- [x] Keep validation cache-first using loaded `FinancialProfileService` state to avoid interrupting UI.
- [x] Move config refresh to background with throttle so freshness is preserved without click-latency.
- [x] Verify compile gate for Android/KMP.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `PosViewModel.checkPaymentMethodsConfigured(...)` now resolves `paymentsConfigured` immediately from in-memory state (`FinancialProfileService` loaded state / local UI fallback) and returns without waiting network.
- Added background sync `refreshPaymentConfigInBackground(...)` with in-flight guard + 60s throttle to avoid repeated `GET /api/v1/business/config-summary` hits.
- `onPaymentScreenVisible()` now opportunistically triggers this background sync, so data freshness is recovered outside direct user click paths and not from tab/button clicks.
- Added `FinancialProfileService.hasLoadedProfile()` for explicit cache-readiness checks.

## Iteration 3 ACH Proof Download Cache-First (Current)

## Plan
- [x] Remove LoadingSheet when ACH proof binary is already cached from preview.
- [x] Make ACH proof download resolve detail/cache by canonical `payment_intent_id` aliases to avoid unnecessary re-download failures.
- [x] Keep current behavior for network fallback (loader + feedback) only when binary is not cached.
- [x] Verify compile gate for Android/KMP.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `openAchProofDocumentForDownload` ahora es cache-first: si el binario del comprobante ya existe, abre/descarga directo sin `showLoading()` ni `LoadingSheet`.
- Se agregó resolución robusta de detalle ACH y llaves de caché por alias (`paymentIntentId` solicitado, `detail.paymentUid`, llave activa de preview y llaves del mapa de estados), evitando re-download innecesario cuando cambia el origen de navegación (órdenes/notificaciones).
- El fallback de red se mantiene para casos sin caché: ahí sí se conserva `showLoading()` + `showSuccess()/showError()` según resultado.

## Iteration 3 ACH Approve/Reject Action Fix (Current)

## Plan
- [x] Fix ACH approve/reject action handlers in ViewModel to avoid silent no-op when no selected order is present.
- [x] Align reject payload `reason_text` defaults with descriptive backend-friendly text.
- [x] Keep post-action refresh behavior: always refresh ACH detail; refresh order only when order context exists.
- [x] Verify compile gate for Android/KMP.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause: `confirmApproveAchPayment()` y `confirmRejectAchPayment()` retornaban temprano con `uiState.order == null`, generando no-op silencioso en vista de detalle ACH fuera de contexto de orden.
- Fix aplicado: ambas acciones ahora resuelven `businessId` con fallback `business?.businessId ?: order?.businessId`, muestran `snackbar` si no hay negocio válido y no dependen de que exista orden seleccionada.
- Post-action refresh: detalle ACH se refresca siempre; refresh de orden solo se ejecuta cuando existe contexto de orden.
- Payload reject: `reason_text` ahora usa texto descriptivo por `reason_code` (`fraud`, `invalid_proof`, `amount_mismatch`, `reference_mismatch`), manteniendo `customReasonText` para `other`.
- Follow-up notificaciones: en `AchPaymentReviewScreen` approve/reject/preview ahora usan el `payment_intent_id` canónico (`detail.paymentUid`) en vez del `paymentUid` de ruta; además `NotificationActionResolver` acepta `payment_intent_id`, `payment_id` e `id` como fallback.

## Iteration 3 ACH Review UI Visual Refresh (Current)

## Plan
- [x] Redesign ACH review visual language (hero, cards, chips, spacing, hierarchy) to match provided mobile references while preserving behavior.
- [x] Refactor commercial summary and expected-vs-detected presentation for clearer, app-style cards and parity labels.
- [x] Polish findings, proof, and timeline sections with stronger visual grouping and readability on mobile.
- [x] Verify compile gate for Android/KMP after UI changes.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Se aplicó refresh visual de la pantalla ACH con dirección UI más cercana a los mocks: hero más jerárquico con badges (`estado`, `riesgo`, `score`), bloques con radios suaves y espaciado consistente.
- `Resumen comercial` pasó a formato de tarjetas compactas en grid de 2 columnas con iconografía por campo para lectura rápida.
- `Esperado vs Detectado` se transformó en filas comparativas tipo card con etiqueta de estado (`Coincide`/`No coincide`) para resaltar discrepancias.
- `Hallazgos` se reorganizó en buckets verticales (riesgo sube/baja) para mejor legibilidad en mobile.
- `Comprobante` ahora usa panel enmarcado con acciones y nota contextual más clara cuando preview/download no aplica.
- `Timeline del pago` adoptó trazo vertical con puntos de estado y eventos en tarjetas secundarias.

## Iteration 3 ACH Review Loader + Web Labels Follow-up (Current)

## Plan
- [x] Fix ACH proof preview infinite skeleton state by handling in-flight detail fetches and clearing loading state on early error exits.
- [x] Align ACH review labels/texts with web copy for sections and fields (hero/commercial summary/comparison/findings/proof/timeline).
- [x] Improve timeline event labeling/messages to web-equivalent wording (`Checkout ACH creado`, `Comprobante subido`, `OCR procesado`, etc.).
- [x] Verify compile gate for Android/KMP and focused ACH normalizer tests.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests '*AchPaymentNormalizerTest*'`

## Review Notes
- Se corrigió el loop de shimmer infinito en revisión ACH: cuando el detalle estaba en `in-flight`, ahora se espera el resultado (`waitForAchDetailInFlight`) y en salidas tempranas se limpia `achProofPreviewState.isLoading=false`.
- En `loadAchReview`, si falla la carga de detalle/comprobante, el estado del preview ya no queda colgado en loading.
- Se alineó copy de la pantalla ACH con web:
- `Control antifraude`, `Sugerencia`, `Correo`, `Pedido`, `Banco Destino`, `Fecha de pago`, `Hallazgos y sugerencia de decisión`, `Timeline del pago`, `Descargar comprobante`.
- `Esperado vs Detectado` ahora incluye `Cuenta destino`, `Banco` y `Fecha` además de monto/referencia.
- Timeline ACH desde eventos backend quedó con labels/mensajes de paridad web (`Checkout ACH creado`, `Comprobante subido`, `OCR procesado`, etc.) y chips de estado (`Checkout Created`, `En revisión`, `Aprobado`).

## Iteration 3 ACH Details Nested Payload + UUID Proof (Current)

## Plan
- [x] Adapt ACH detail normalization to current nested API response (`payment/account/proofs/events/latest_*`) with backward-compatible aliases.
- [x] Support UUID string IDs for ACH `paymentId` and `proofId` across model/service/repository/provider so proof download endpoint works.
- [x] Render proof preview correctly for image and PDF when proof is available only through authenticated binary endpoint.
- [x] Update/expand ACH normalizer tests with nested payload coverage.
- [x] Verify compile + focused tests.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests '*AchPaymentNormalizerTest*'`

## Review Notes
- `AchPaymentNormalizer` ahora soporta contrato anidado del backend ACH (`payment`, `account`, `proofs[]`, `events`, `latest_fraud`, `latest_ocr`) y conserva compatibilidad con aliases legacy.
- IDs ACH de pago/comprobante se migraron a `String` (UUID-safe) en modelo, servicio, repositorio y provider para construir correctamente `/payments/{paymentId}/proofs/{proofId}/file`.
- Vista previa de comprobante ahora soporta PDF binario autenticado: el ViewModel genera `data:application/pdf;base64,...` y `PdfPreview` (Android/iOS) renderiza también data URIs además de enlaces remotos.
- Se reforzó la UI de revisión ACH para leer `latest_fraud.features`/`rules`/`explanations`, y se evita mostrar `Abrir enlace` cuando la fuente es data URI.
- Cobertura de tests ampliada en `AchPaymentNormalizerTest` con caso real de payload anidado + UUID + proof PDF.

## Iteration 6 Notifications Cache Merge + Pull Refresh Flicker (Current)

## Plan
- [x] Preserve loaded notifications in memory/cache during pull refresh and merge first-page server payload by `id` instead of replacing.
- [x] Keep cache de-duplicated on both restore and refresh merges to prevent duplicate rows after repeated reloads.
- [x] Prevent empty-state flicker while pull-to-refresh is active by gating empty rendering with `!isRefreshing`.
- [x] Verify compile + notification tests.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests '*Notification*'`

## Review Notes
- `NotificationsService.refresh()` now merges `offset=0` refresh payload into existing loaded items when cache/list already exists, preserving immediate UI while pushing new notifications.
- Merge path uses de-duplication by notification `id` and stable descending sort, applied for both live refresh and cached restore.
- `NotificationsScreen` empty state now waits for refresh completion (`!isRefreshing`) so pull reload never flashes an empty list between frames.

## Iteration 4 Payments Authz Scopes for SubUsers (Current)

## Plan
- [x] Add payments route authz policy gated by `invoice:create_payment_link`, `ach_payment:view`, `ach_payment:approve`, `ach_payment:reject` (+ payments beta).
- [x] Map payments screens to the new route key so global authz redirection applies to all payments routes.
- [x] Gate `Pagos y cobros` settings entry visibility with the same authz policy for authenticated sub-users.
- [x] Verify compile and authz unit tests.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest --tests '*AuthzEvaluatorTest*'`

## Review Notes
- Se agregó `RouteKey.PAYMENTS_PAGE` con policy de acceso por cualquiera de los scopes: `invoice:create_payment_link`, `ach_payment:view`, `ach_payment:approve`, `ach_payment:reject`, además de beta `PAYMENTS`.
- Se mapearon todas las pantallas de pagos (`Payments`, `PaymentsHomeScreen`, `PaymentsYappyScreen`, `PaymentsTransferenceScreen`, `PaymentsPaypalScreen`, `PaymentsPaypalOnboardingScreen`) al nuevo `RouteKey.PAYMENTS_PAGE` para que aplique redirección authz global.
- El botón `Pagos y cobros` en Settings ahora se muestra solo cuando `uiState.hasPaymentsAccess` es `true` (incluye bypass de owner y validación real para subusuarios).
- Se añadieron pruebas en `AuthzEvaluatorTest` para `RouteKey.PAYMENTS_PAGE` con y sin scopes/beta.

## Iteration 6 Notifications Screen Visual Parity (Current)

## Plan
- [x] Rediseñar `NotificationsScreen` para paridad visual con mock (header, tabs, CTA y cards) manteniendo componentes del design system.
- [x] Agregar filtro local por pestañas (`Todas`, `No leídas`, `Importantes`, `Transacciones`) y agrupar lista por día (`Hoy`, `Ayer`, fecha).
- [x] Mantener acciones funcionales existentes (`tap`, `dismiss`, `dismiss all`, `load more`) y agregar `Marcar todas como leídas` para items cargados.
- [x] Ajustar strings ES/EN para nuevos textos de UI.
- [x] Ejecutar gates de compilación y pruebas de notificaciones.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests '*Notification*'`

## Review Notes
- `NotificationsScreen` se rediseñó con layout cercano al mock: chips superiores con conteos, CTA `Marcar todas como leídas`, secciones por día (`Hoy`, `Ayer`, fecha), tarjetas de notificación con ícono por tipo, punto de no leído en color secundario y banner informativo final.
- Se añadió filtrado local por pestañas (`Todas`, `No leídas`, `Importantes`, `Transacciones`) sin cambiar contrato API ni paginación existente.
- Se mantuvieron acciones previas (`tap`, `dismiss`, `dismiss all`, `load more`) y se agregó acción masiva `markAllLoadedAsRead()` en `NotificationsViewModel` usando actualización optimista + sync de `unread_count`.
- Se añadieron nuevos textos i18n ES/EN para filtros, secciones, CTA y banner.
- Se agregó ícono de ajustes en el app bar de `PosScreens.NotificationsScreen` para alinear cabecera con referencia visual.
- Se añadió cobertura unitaria para `markAllLoadedAsRead()` en `NotificationsViewModelTest`.
- Follow-up de paleta: tabs, íconos de acento y botones de notificaciones migrados de `primary` a `secondary`.
- Gates en verde (warnings existentes de KSP/KMP sin bloquear).

## Iteration 6 Analytics Events Payments Core + Notifications (Current)

## Plan
- [x] Extender `AnalyticsService` con helpers tipados para todos los eventos nuevos y utilidades de parámetros compartidos.
- [x] Instrumentar eventos de Payments Core en métodos de pago (settings/onboarding/config), printer onboarding/config, gastos y órdenes.
- [x] Instrumentar `order_creation_payment_option_selected` en creación de orden (MANUAL|LINK|DRAFT).
- [x] Instrumentar eventos de Notifications (`received/opened/read/archived`) con `business_id`, `user_id`, `user_email`, `notification_id`, `notification_type`.
- [x] Ajustar DI/tests impactados y verificar compilación + pruebas de notificaciones.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests '*Notification*'`

## Review Notes
- `AnalyticsService` ahora expone helpers tipados para todos los eventos de este alcance (Payments Core + Printer + Expense/Order payments + Notifications), con base params (`surface`, `flow`, `action`) y sanitización de `error_code`.
- Se instrumentó Payments Settings/Onboarding (viewed/started/blocked/completed/failed/skipped) y configuración de métodos (`payment_method_config_attempted/succeeded/failed`) en acciones Yappy/ACH/PayPal.
- Se instrumentó printer onboarding/config (`opened`, `step_viewed`, `dismissed`, `completed`, `action_clicked`, `config_attempted/succeeded/failed`) en pantalla y ViewModel.
- Se instrumentó gastos y órdenes para acciones y submits de pago (`opened`, `submit_attempted/succeeded/failed`, `delete_succeeded/failed`, `link_action`) y selección de opción de pago en creación de orden (`MANUAL|LINK|DRAFT`).
- Notifications ahora registran `notification_received/opened/read/archived` con `business_id`, `user_id`, `user_email`, `notification_id`, `notification_type` desde el servicio de dominio.
- Se ajustó DI para nuevas dependencias (`AnalyticsService` y `IAuthService`) y se actualizaron tests de notifications para el contrato nuevo.
- Gates ejecutados y en verde (warnings preexistentes de KSP/Kotlin sin bloqueo).

## Iteration 5 In-App Notifications MVP (Current)

## Plan
- [x] Extend notifications domain model with optional `priority` and `status`, plus derived state helpers for unread/read/dismissed/removed.
- [x] Implement notifications list ViewModel + screen with first load, pagination (`loadMore`), dismiss one, dismiss all loaded, unread dot, status/priority chips, and shimmer first-load.
- [x] Resolve notification actions by URL contract (ACH in-app route, unknown absolute external, unknown relative unsupported message).
- [x] Integrate Home header bell with unread badge cap (`99+`) and navigation to notifications screen.
- [x] Add route wiring (`PosScreens.NotificationsScreen`) and authz mapping for the new screen.
- [x] Add notification-focused unit tests for action resolver, model/badge mapping, and ViewModel workflows.
- [x] Fix test determinism by injecting `ioDispatcher` into `NotificationsViewModel` and using test dispatcher in unit tests.
- [x] Add missing Spanish resource strings for notifications and align fallback kind icon to bell.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests '*Notification*'`

## Review Notes
- Home top card now supports an optional trailing action and renders a notifications bell with red unread badge (`1..99`, `99+`, hidden at `0`).
- Notifications screen supports list visibility filtering (`!dismissed && !removed`), item dismiss, dismiss-all-loaded, load-more pagination, and unread secondary dot.
- Notification kind-to-icon mapping includes the requested registered kinds and now uses bell icon fallback.
- Row tap marks unread notifications seen optimistically in UI and sends background `markSeen`; dismiss actions use `LoadingSheet` success/error feedback.
- Notification action resolver behavior matches the locked assumptions for in-app, external, and unsupported relative URLs.
- Notification unit tests now pass after dispatcher injection refactor in ViewModel.

## Follow-up Fixes (Apr 22, 2026)
- [x] Update Home bell icon to outlined visual style and move badge closer to icon.
- [x] Enforce Home top-card layout as `[logo][business name][spacer][bell]` with bell pinned at top-right.
- [x] Fix crash when opening notifications by changing Koin registration for `NotificationsViewModel` from `viewModelOf(::NotificationsViewModel)` to explicit factory with only `notificationsService`.
- [x] Increase bell icon size and vertically align bell container with business name row.

### Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests '*Notification*'`

## Iteration 4 PayPal Onboarding 4-Step Parity (Current)

## Plan
- [x] Replace generic PayPal onboarding with dedicated 4-step flow (Cómo funciona, Requisitos, Costos, Configuración).
- [x] Implement PayPal onboarding step content/UI parity including hero image, bullets, alert, and status rows with icons.
- [x] Keep step-4 actions wired to existing endpoints (`connect` and `billing agreement`) and open returned URLs in browser.
- [x] Transition onboarding to success screen once both PayPal statuses are complete.
- [x] Verify compile gate for Android/KMP after PayPal onboarding changes.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- PayPal onboarding now follows dedicated 4-step wizard (info, requisitos, costos, configuración) with its own stepper and footer actions.
- Step 4 renders connection status for `Cuenta PayPal conectada` and `Autorización de cobro` with icon + status pill, plus CTA buttons that open connect and billing-agreement URLs.
- Finalization validates fresh profile state and advances to success screen only when both PayPal states are complete.
- Compile gate passed successfully (warnings only).

## Iteration 4 Commissions Tabs Secondary Color (Current)

## Plan
- [x] Update `Transacciones` / `Ciclos de cobro` tab selector styling to secondary color palette.
- [x] Verify compile gate for Android/KMP after style change.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Tabs now use `secondary` styling: selected pill background + onSecondary label, unselected label in secondary.
- Compile gate passed successfully (warnings only).

## Iteration 4 Duplicate Content Titles Cleanup (Current)

## Plan
- [x] Remove in-content duplicate title `Métodos de pago` from payments home content section.
- [x] Remove in-content duplicate method titles (`Yappy`, `ACH con comprobante`, `PayPal`) from method detail content.
- [x] Verify compile gate for Android/KMP after UI cleanup.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Removed duplicated content titles so app bar remains the single source of page naming in payments home and method detail routes.
- Compile gate passed successfully (warnings only).

## Iteration 4 ACH Persistence Bug Fix (Current)

## Plan
- [x] Corregir persistencia de estado configurado ACH al volver desde Home cuando `config-summary` no envía `enabled`.
- [x] Forzar refresh de `GET /api/v1/payments/ach/status` cuando se confirme acceso de pagos para evitar race de bootstrap.
- [x] Verificar compilación KMP/Android.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `AchMethod.enabled` ahora asume `true` por defecto cuando backend omite el campo en `config-summary`, evitando falso negativo de método no configurado.
- Se añadió refresh explícito de status ACH en `observeBetaAccess()` para garantizar estado consistente al reingresar a la pantalla.

## Iteration 4 ACH Configured Parity Fixes (Current)

## Plan
- [x] En ACH configurado ocultar código de banco y usar selección por banco (nombre) con code+name interno.
- [x] Mapear tipo de cuenta con labels `Ahorros`/`Corriente` y códigos `savings`/`checking`.
- [x] Mostrar número de cuenta enmascarado desde estado ACH y exigir reingreso para actualizar.
- [x] Quitar input de instrucciones en ACH configurado.
- [x] Estilar botón `Desactivar ACH` con color de warning/error.
- [x] Permitir actualización de configuración ACH vía `PUT /api/v1/payments/ach/account`.
- [x] Verificar compilación KMP/Android.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- El detalle configurado de ACH ahora muestra el banco por nombre (sin exponer `bank_code`) y el número enmascarado.
- Para actualizar ACH, el usuario debe ingresar nuevamente el número de cuenta; el formulario no reutiliza el valor enmascarado.
- El refresco de estado ACH sigue usando `GET /api/v1/payments/ach/status` como fuente de estado en pantalla.

## Iteration 4 General Switch Color (Current)

## Plan
- [x] Cambiar el color del switch de configuración general a `secondary`.
- [x] Verificar compilación Android.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileDebugKotlinAndroid`

## Review Notes
- El switch de `Emitir factura automáticamente...` ahora usa `MaterialTheme.colorScheme.secondary` en thumb y track.

## Iteration 4 Fee Badge Mapping Fix (Current)

## Plan
- [x] Ajustar comisiones mostradas por método configurado según regla de negocio.
- [x] Verificar compilación Android.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Fee badges actualizados:
- Yappy `1%`
- ACH `$0.27`
- PayPal `1%`

## Iteration 4 Header Cleanup (Current)

## Plan
- [x] Remover botón `Atrás` del header de detalle de método en Payments onboarding/config.
- [x] Limpiar import no usado asociado al ícono de back.
- [x] Verificar compilación Android.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileDebugKotlinAndroid`

## Review Notes
- El header de detalle de método queda solo con título centrado, sin acción de retroceso en esquina izquierda.

## Iteration 4 ACH Bank Catalog Parity (Current)

## Plan
- [x] Reemplazar lista corta de bancos ACH por catálogo completo (código+nombre) provisto para dropdown de onboarding ACH.
- [x] Mantener mapeo de selección para guardar `bankCode` y `bankName` en `achForm`.
- [x] Verificar compilación KMP/Android.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- El selector ACH ahora usa el catálogo completo alineado al enum/opciones web, incluyendo nombres oficiales de bancos.

## Iteration 4 ACH Onboarding Carousel (Current)

## Plan
- [x] Implement ACH onboarding as a guided 3-step flow aligned with Yappy onboarding pattern.
- [x] Add Step 1 with promo image and Spanish "Cómo funciona" copy.
- [x] Add Step 2 "Costos y cobros" content with bullet structure.
- [x] Add Step 3 ACH configuration form with bank dropdown, account type dropdown, account number and account name fields.
- [x] Add ACH onboarding footer with step-aware CTA labels (`Continuar` / `Guardar configuración ACH`).
- [x] Apply secondary color styling to ACH onboarding action buttons.
- [x] Verify compile gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- ACH onboarding now follows dedicated method flow with stepper parity and method-specific footer actions.
- Step 3 saves ACH config through existing typed request flow and transitions to success state in onboarding mode.

## Iteration 4 Yappy Onboarding Visual Polish (Current)

## Plan
- [x] Set payments screen background to `MaterialTheme.colorScheme.background`.
- [x] Center Yappy title horizontally in method onboarding header.
- [x] Add `Guías útiles` title and style guide links with secondary color in requirements step.
- [x] Apply bold emphasis to requested fee/cost tokens in Yappy costs copy.
- [x] Add eye icon to tutorial text button in Yappy config step.
- [x] Verify compile gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Yappy onboarding copy/styles now match requested final polish details.
- Header keeps a centered title while preserving back action at the left side.

## Iteration 4 Yappy Onboarding Carousel (Current)

## Plan
- [x] Convert Yappy onboarding into 4 guided steps with explicit stepper progression.
- [x] Implement Step 1 product info with remote promo image and Spanish copy parity.
- [x] Implement Step 2 requirements with bullet list + external PDF guides.
- [x] Implement Step 3 fees/costs explanation with bullet list and compatibility alert.
- [x] Implement Step 4 configuration form + tutorial link + `Conectar Yappy` action.
- [x] Update Yappy success behavior to show success state and auto-return to payment methods.
- [x] Verify compile gates after ViewModel + Compose updates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Yappy now uses a dedicated onboarding path with stepper `1..4`; ACH/PayPal onboarding flow remains unchanged.
- Step content matches requested web parity structure and keeps all copy in Spanish.
- External links open through the existing payment UI event pipeline (`OpenExternalUrl`) to preserve platform behavior.
- On successful Yappy connection, screen enters success state and returns automatically to the methods list.

## Iteration 4 Payments Web Parity Polish (Current)

## Plan
- [x] Align settings entry with web parity by adding `Nuevo` badge in `Pagos y cobros`.
- [x] Align payment channels UX so full row tap opens method detail (not only chevron tap).
- [x] Align PayPal onboarding step-2 behavior to show warning toast on continue attempt when not configured.
- [x] Keep compile verification green after UI/design-system changes.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Added `badgeText` support to `SettingsTextButton` and applied `Nuevo` badge for `Pagos y cobros` entry in Settings.
- Updated channels list interaction in payments home so each visible method row is fully tappable and opens method detail.
- Updated onboarding footer behavior for PayPal step 2: primary action remains tappable and delegates validation to ViewModel, which now surfaces the expected warning when PayPal is not fully configured.
- Compile gates passed on April 17, 2026.

## Settings Payment Entry Visibility Fix TODO

## Plan
- [x] Identify why payment methods option is not visible in Settings screen.
- [x] Restore settings entry and route wiring to payments flow.
- [x] Verify Android compile.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause: payment-methods entry was fully disabled in `SettingsScreen` behind a temporary "HIDDEN" block.
- Fix: restored "Pagos y cobros" directly in the Settings action list (`SettingsTextButton`) and wired CTA to `PosScreens.Payments`.
- Result: all users with Settings access now see and can open payment methods configuration from Settings.

## Plan
- [x] Create and persist multi-iteration implementation plan artifact.
- [x] Iteration 1 foundation: authz + contracts + providers/repositories/services + model extensions + error mappings.
- [x] Iteration 2 orders creation/detail payment-link + retry invoice UX.
- [x] Iteration 3 ACH review (inline + dedicated screen).
- [ ] Iteration 4 settings/payments redesign + ACH + fees.
- [ ] Iteration 5 in-app notifications module + topbar dropdown UX.
- [ ] Iteration 6 analytics parity + hardening + localization sweep.

## Iteration 3 Checklist
- [x] Add ACH inline section in order detail payment rows with lazy detail load, cache and in-flight de-dup.
- [x] Add ACH approve/reject actions from order detail with permission gates and success refresh.
- [x] Add order detail proof preview modal/action for ACH payments.
- [x] Add dedicated ACH review route + screen (loading/error/content states).
- [x] Add dedicated ACH review reject modal and score-help modal.
- [x] Add proof panel behavior in review screen (blob-first attempt, URL fallback, download policy).
- [x] Verify compile/tests and document outcome in `tasks/lessons.md`.

## Verification Gates
- [x] `./gradlew :composeApp:compileKotlinMetadata`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew :composeApp:testDebugUnitTest --tests '*PaymentLinkResolverTest*' --tests '*AchPaymentNormalizerTest*' --tests '*AuthzEvaluatorTest*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest --tests '*PaymentLinkResolverTest*' --tests '*AchPaymentNormalizerTest*' --tests '*AuthzEvaluatorTest*'`

## Review Notes
- Iteration 0 artifacts created:
- `tasks/payments-settings-notifications-kmp-replication-plan.md`
- tracking section in `tasks/todo.md`.
- Iteration 1 completed with foundation scope:
- authz + beta additions for payment links and ACH actions/routes.
- `Order` model extensions for payment links array/fallback aliases and ACH-automatic flag.
- financial profile/payment summary contract extensions (ACH, fee billing, auto-invoice).
- orders provider/repository/service additions for create payment link, ACH detail/approve/reject, proof download.
- payments provider/repository/service additions for auto-invoice, ACH status/account/config/disable, fee summary/transactions/batches.
- new in-app notifications module (provider/repository/service + cache scaffolding) wired in DI.
- new shared normalizers/utilities:
- `PaymentLinkResolver`
- `AchPaymentNormalizer`
- extended API error code mapping for `O_RP_001/002/004/005`, `PAY_001`, `PAY_002`, `PAY_PP_001`, `INV_001`, `INV_002`.
- tests added and passing:
- `PaymentLinkResolverTest`
- `AchPaymentNormalizerTest`
- `AuthzEvaluatorTest` (new payment/ACH gate coverage).
- Iteration 2 completed:
- POS payment-link preflight now forces `FinancialProfileService.refresh(...)` before enabling link flow or showing settings redirect dialog.
- POS link-tab selection marks `Nuevo` as seen and stores a full payment-step checkpoint snapshot before redirecting to payment settings.
- Order detail now resolves link source via `PaymentLinkResolver` and applies action matrix:
- `Generar Link de Pago` when eligible (authz + configured methods + unpaid + pending balance + no open link).
- `Copiar Link de Pago` and `Compartir Link de Pago` when an active link exists.
- New generate-link modal in order detail with optional amount, expiry presets (`1h`, `12h`, `24h`, `3 días`) and custom minutes validation.
- Order detail now renders shimmer skeleton on first-load, including payment-link/action surfaces while detail data is null.
- Retry invoice flow now shows:
- success dialog (`Descargar factura`, `Compartir factura`) when invoice gets issued,
- warning dialog with backend message fallback when still pending verification.
- Invoice success dialog now includes structured visual parity details for `Total facturado` and `CUFE`, and share payload includes those fields.
- Iteration 3 completed:
- Order detail now renders ACH automatic-payment inline metadata/actions with lazy detail fetch, in-memory cache and in-flight request dedupe.
- ACH approve/reject actions are enabled from order detail and dedicated ACH review, gated by authz and state, with post-action order/detail refresh.
- Added dedicated ACH review route/screen with loading/error/content states plus sections for hero, commercial summary, `Esperado vs Detectado`, hallazgos split, proof panel and timeline.
- Added ACH modals in dedicated review context: reject with reason-code/custom text and score explanation.
- Proof handling now enforces policy parity: blob-first preview/download, URL fallback, no preview for rejected payments, and download button only for approved statuses.
- Verification passed on April 17, 2026:
- `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest --tests '*AchPaymentNormalizerTest*' --tests '*AuthzEvaluatorTest*'`

# iOS Network Images Not Loading TODO

## Plan
- [x] Validate current Coil/iOS network image wiring and identify root-cause candidate.
- [x] Apply minimal-impact fix for iOS network image loading and add runtime error visibility for failed requests.
- [x] Run verification gates (shared + iOS compile).

## Verification Gates
- [x] `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
- [x] `./gradlew :composeApp:compileKotlinMetadata`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- The target CDN URL responds correctly (`HTTP 200`, `image/jpeg`) and serves a valid TLS chain; this suggests an app-side iOS image pipeline issue rather than a broken asset URL.
- Root-cause candidate: implicit/default image loader setup on iOS was not explicit, and failures were silent in UI because `AsyncImage` in `HomeLogo` had no error callback.
- Root-cause confirmed by runtime log: `TLS sessions are not supported on Native platform.` The iOS classpath was receiving `ktor-client-cio` from `commonMain`, which can force Coil/Ktor image requests into a Native-incompatible TLS path.
- Applied a minimal-impact hardening:
- Added a singleton Coil `ImageLoader` factory in `App.kt` with explicit `KtorNetworkFetcherFactory()` registration.
- Updated `HomeLogo` (`Cards.kt`) to use `getImageRequest(LocalPlatformContext.current, url.trim())` and added `onError` logging so iOS failures are observable.
- Moved `ktor-client-cio` dependency from `commonMain` to `androidMain` in `composeApp/build.gradle.kts` so iOS resolves only Darwin engine (`ktor-client-darwin`) for Ktor/Coil networking.
- Confirmed iOS dependency graph no longer includes `ktor-client-cio` in `iosSimulatorArm64CompileKlibraries`.
- Verification passed on April 3, 2026:
- `./gradlew :composeApp:compileKotlinMetadata`
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
- `./gradlew :composeApp:compileDebugKotlinAndroid`

# iOS QR Scanner Crash (Skia/Metal) TODO

## Plan
- [x] Inspect iOS scanner stack trace and identify shared UI path used by all scanner entry points.
- [x] Apply minimal-impact fix in scanner overlay rendering to avoid iOS Skia crash path.
- [x] Run iOS verification gates (KMP compile + iOS app build + simulator launch).

## Verification Gates
- [x] `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
- [x] `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'id=640A022E-3291-4ABD-9F49-442B6FFDB32F' build`
- [x] `xcrun simctl launch booted com.teco.ventago.VentaGo`

## Review Notes
- Crash reported at scanner open: `EXC_BAD_ACCESS` in `SkPictureRecorder::finishRecordingAsPicture` (`MetalRedrawer` draw path).
- Root-cause candidate was scanner scrim rendering in `QRGenerator.kt` using `clipPath(..., ClipOp.Difference)` over full-screen canvas, which is unstable on Compose iOS/Skia in some runtime/device combinations.
- Fix: replaced clip-path difference approach with a deterministic 4-rect scrim draw around the cutout (no clip operations), preserving scanner visual behavior while avoiding the Skia crash path.
- File updated: `composeApp/src/commonMain/kotlin/com/teco/ventago/utils/QRGenerator.kt`.
- Verification passed on April 3, 2026:
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'id=640A022E-3291-4ABD-9F49-442B6FFDB32F' build`
- `xcrun simctl launch booted com.teco.ventago.VentaGo` (pid `30327`)

# iOS Camera/Permissions/Firebase Integration TODO

## Plan
- [x] Fix iOS gallery picker and camera capture launch flow (presenter lookup + safe picker callbacks + unavailable source handling).
- [x] Correct iOS camera/gallery permission handling for all relevant iOS authorization statuses.
- [x] Add missing iOS privacy usage descriptions required for camera/photo access and scanner flows.
- [x] Implement iOS notification/token wiring (permission request + APNs/FCM token bridge) and remove empty iOS `getToken()` behavior.
- [x] Run iOS verification gates (KMP compile + iOS app build + simulator launch).

## Verification Gates
- [x] `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
- [x] `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'id=640A022E-3291-4ABD-9F49-442B6FFDB32F' build`
- [x] `xcrun simctl launch booted com.teco.ventago.VentaGo`

## Review Notes
- Fixed iOS image picker presentation in `CameraManager.ios.kt` and `GalleryManager.ios.kt` by replacing direct `keyWindow` usage with shared `getRootViewController()` resolution and by handling picker cancel paths safely.
- Prevented picker runtime issues by replacing unsafe `Map.getValue(...)` extraction with nullable lookups and by returning `null` when source types are unavailable (e.g., camera in simulator).
- Updated `PermissionsManager.ios.kt` to handle all relevant statuses without crashes:
- Camera: `Authorized` -> granted, `NotDetermined` -> request, `Denied/Restricted` -> denied.
- Gallery: `Authorized/Limited` -> granted, `NotDetermined` -> request, `Denied/Restricted` -> denied.
- Added missing privacy keys in iOS app plist:
- `NSCameraUsageDescription`
- `NSPhotoLibraryUsageDescription`
- Implemented iOS notification/token integration:
- `Platform.ios.kt` now requests notification authorization.
- `core/firebase/FirebaseMessaging.kt` iOS actual now requests notification authorization (no longer empty) and triggers APNs registration after grant.
- `iosApp/iOSApp.swift` now wires `UNUserNotificationCenterDelegate` + `MessagingDelegate`, maps APNs token to Firebase Messaging, and persists FCM tokens into Firestore `device_tokens/{uid}` (arrayUnion), mirroring Android behavior.
- APNs registration from Kotlin is executed via Objective-C selector (`registerForRemoteNotifications`) with `@OptIn(ExperimentalForeignApi::class)` to keep Kotlin/Native compatibility.
- Scanner crash root cause (`TCC Code 0` missing usage description) is addressed by the new camera usage plist key.
- Verification passed on April 3, 2026:
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'id=640A022E-3291-4ABD-9F49-442B6FFDB32F' build`
- `xcrun simctl launch booted com.teco.ventago.VentaGo` (pid `26714`)

# iOS Simulator Build Enablement TODO

## Plan
- [x] Reproduce iOS simulator compile failures on current branch.
- [x] Fix Kotlin Multiplatform iOS compile errors with minimal-impact changes.
- [x] Verify iOS simulator target compiles successfully.

## Verification Gates
- [x] `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
- [x] `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'id=640A022E-3291-4ABD-9F49-442B6FFDB32F' build`

## Review Notes
- Fixed iOS compile blockers in `LoginViewModel` and `CufeImportViewModel` by importing `kotlinx.coroutines.IO` so `Dispatchers.IO` resolves to the public multiplatform extension on Native.
- Fixed `PdfPreview.ios.kt` by using `androidx.compose.ui.viewinterop.UIKitInteropProperties`, adding `kotlinx.cinterop.readValue` + `ExperimentalForeignApi` opt-in, and using Foundation wildcard imports for NSURL percent-encoding APIs.
- Initial `xcodebuild` failed at link stage due missing Epson symbols (`Epos2Printer`, `Epos2Discovery`, `Epos2FilterOption`) from `ComposeApp.framework`.
- Added simulator/device-specific Epson static library linkage in `iosApp.xcodeproj` via `OTHER_LDFLAGS[sdk=iphonesimulator*]` and `OTHER_LDFLAGS[sdk=iphoneos*]`.
- Verification passed on April 3, 2026:
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'id=640A022E-3291-4ABD-9F49-442B6FFDB32F' build`
- Installed and launched on booted simulator successfully: `xcrun simctl launch booted com.teco.ventago.VentaGo` (pid `2458`).

# Printer QR Onboarding Entry TODO

## Plan
- [x] Add QR deep-link contract updates (`fromQr` route arg, typed deep links, Android manifest app-link path).
- [x] Thread QR source flag into `PrinterOnboardingScreen` LANDING step and apply CTA overrides only for QR entries.
- [x] Persist pending QR onboarding when user is unauthenticated and trigger one-time post-login prompt after business data load.
- [x] Run verification gates.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [ ] `./gradlew :composeApp:compileKotlinIosSimulatorArm64`

## Review Notes
- Added `fromQr` to `PrinterOnboardingRoute` and wired deep links for `https://tecodigi.com/printer-onboarding` (including `?fromQr=true`) in `Navigation.kt`.
- Added Android app-link intent filter for `android:pathPrefix="/printer-onboarding"` in `composeApp/src/androidMain/AndroidManifest.xml`.
- Threaded `fromQr` through `PrinterOnboardingScreen` flow so LANDING hides `Adquirir mi impresora` and renames `Ya tengo mi impresora` to `Configurar impresora` only for QR entry.
- Added `PrinterQrEntry.KEY_PENDING_ONBOARDING` and, on QR deep-link arrival while unauthenticated, persist pending onboarding in `LocalStorage`.
- Added one-time post-login prompt in `App.kt` that appears only after business data is loaded (`mainState.business != null`), clears pending state on both confirm/dismiss, and routes confirm to `PrinterOnboardingRoute(entryContext = SETTINGS, fromQr = true)`.
- Verification passed on April 3, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.
- iOS verification remains blocked by pre-existing unrelated compile errors in:
- `features/auth/ui/login/viewmodel/LoginViewModel.kt` (`Dispatchers.IO` visibility)
- `features/expenses/ui/cufe/CufeImportViewModel.kt` (`Dispatchers.IO` visibility)
- `iosMain/features/quotes/ui/preview/PdfPreview.ios.kt` (unresolved iOS preview symbols)

# Printer Onboarding UX + Save State Fix TODO

## Plan
- [x] Change config action buttons (`Atrás`, `Probar impresión`, `Guardar`) from single row to stacked column layout.
- [x] Fix automatic mode selection persistence so changing branch/billing point keeps discovered-printer endpoint data and allows save re-enable after test print without reselecting printer.
- [x] Run Android compile verification gate.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Updated `PrinterConfigStep` action controls from a 3-button row to a vertical full-width stack (`Atrás`, `Probar impresión`, `Guardar`) for better readability and touch targets.
- Fixed auto-mode state reset in `applyPrinterForCurrentSelection(...)`: when there is no saved printer for the newly selected branch/billing point and a discovered printer is still selected, the form now preserves discovered endpoint values (model/host/port) instead of clearing them.
- This prevents the stale UX where users had to tap the discovered printer again after changing billing point before save could become enabled again after a successful test print.
- Verification passed on April 3, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.


# Billing Point Edit Sheet Buttons TODO

## Plan
- [x] Re-layout edit sheet actions into a two-row layout with modify/delete on top and printer config below.
- [x] Switch printer config action to an outlined secondary button.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Updated the billing point edit sheet actions to match the two-row layout requirement.
- Printer configuration action now uses `OutlinedButtonM` with secondary color styling.
- Verification passed with existing KSP/Kotlin version warnings.

# Billing Point Printer Indicator TODO

## Plan
- [x] Extend `FiscalBillingPointItem` trailing slot to show a printer icon in secondary color when a printer is configured.
- [x] Pass printer-configured flag from `BillingPointsManageScreen` per billing point.
- [x] Run Android compile verification gate.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Updated `FiscalBillingPointItem` to accept `hasConfiguredPrinter` and render a trailing printer icon (`Icons.Rounded.Print`) tinted with `MaterialTheme.colorScheme.secondary`.
- The printer icon is shown only when `hasConfiguredPrinter` is true, and is placed before the existing options (`MoreHoriz`) button in the trailing slot.
- `BillingPointsManageScreen` now computes `hasConfiguredPrinter` per billing point using cached printer configs matching current `selectedBranchCode` + item `billingPoint`.
- Verification passed on April 3, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.


# Printers List UI Refresh TODO

## Plan
- [x] Simplify `PrinterCard` content to show only printer name, branch-billing point, and compact status badge.
- [x] Replace inline action buttons with a 3-dots overflow action for modify/delete actions.
- [x] Run Android compile verification gate for the updated screen.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Updated `PrintersScreen` list items to a compact three-line layout:
- Line 1: printer name (`bodyMediumBold`) with trailing 3-dots overflow action.
- Line 2: `branch - billingPoint` in muted text (`onSurfaceVariant`).
- Line 3: small pill badge for status (`Habilitada`/`Deshabilitada`).
- Replaced inline `Editar`/`Eliminar` buttons with overflow menu actions: `Modificar` and `Eliminar`.
- Verification passed on April 3, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.


# Draft Invoice Payment Method Sheet TODO

# Printer Invoice Web/Mobile Parity TODO

## Plan
- [x] Compare web adapter (`epson-epos.adapter.js`) render behavior vs KMP parser/engines for reprint invoice payload.
- [x] Patch shared parser/models so block rendering matches web semantics (align/emphasis, key-value layout, labeled dividers, weighted table columns, QR sizing).
- [x] Patch native engines to support image blocks from both base64 and remote URL sources.
- [x] Add/adjust parser regression tests and run verification gates.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests '*TicketLayoutParserTest*'`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [ ] `./gradlew :composeApp:compileKotlinIosSimulatorArm64`

## Review Notes
- Root cause: KMP reprint path diverged from web adapter rendering rules. Web uses software text alignment, inline key/value for non-totals, labeled single-line dividers, weighted tables, and QR width clamping by paper width.
- Implemented parity updates in shared parser/model:
- Software-aligned padded text output for `left/center/right`, avoiding centered text clipping on 57mm.
- `key_value.values[]` rendered inline (`Label: Value`) by default; totals keep split/right-aligned value behavior.
- Divider labels rendered as a single line (`----- Label -----`) instead of label + separate divider line.
- Table rendering now respects column alignment and `weight` metadata (`7/3` products section).
- QR module sizing now uses paper-aware max size and `width_hint` percent.
- Image blocks now pass source values (base64 or URL) to engines instead of being dropped when source is URL.
- Mobile image rendering now honors web-like paper canvas widths and `image.width_hint` percent, so logos are scaled/centered within 57/80mm printable area instead of using raw source dimensions.
- Native engine update:
- Android Epson engine now supports `PrintCommand.Image.source` as either base64/data URI or HTTP(S) URL download.
- iOS Epson engine now supports `PrintCommand.Image.source` as either base64/data URI or HTTP(S) URL download.
- Verification passed on April 2, 2026:
- `./gradlew :composeApp:testDebugUnitTest --tests '*TicketLayoutParserTest*'`
- `./gradlew :composeApp:compileDebugKotlinAndroid`
- iOS verification remains blocked by pre-existing unrelated compile errors in login/cufe/quotes preview files.

# Order Reprint Ticket Layout Compatibility TODO

## Plan
- [x] Compare backend invoice ticket payload shape with web adapter behavior (`epson-epos.adapter.js`).
- [x] Patch KMP `TicketLayoutParser` to support backend/web block schema used by reprint.
- [x] Add regression test for order reprint payload shape and run verification gates.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests '*TicketLayoutParserTest*'`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause: KMP parser expected simplified blocks (`key/value`, `headers`, flat `qr`/`image`) while backend reprint payload uses web shape (`align`, `emphasis`, `key_value.values[]`, `table.columns + rows.values`, nested `qr.content`, nested `image.source`).
- This mismatch caused parser exceptions before printing on `OrderDetailsScreen` reprint flow.
- KMP parser now mirrors web compatibility:
- `align` alias support and string emphasis (`"bold"`).
- `key_value.values[]` expansion into printable key/value lines.
- `table.columns` + `rows.values` mapping into table rows.
- `qr.content` inside nested `qr` object.
- Divider labels rendered as labeled separators.
- Remote `image.source` URLs are treated as optional and skipped (no hard-fail on mobile engines that only decode base64 image payloads).
- Added regression test `parse_acceptsBackendInvoiceLayoutShapeUsedByOrderReprint`.
- Verification passed on April 2, 2026:
- `./gradlew :composeApp:testDebugUnitTest --tests '*TicketLayoutParserTest*'`
- `./gradlew :composeApp:compileDebugKotlinAndroid`

# Printer Discovery MAC vs IP Host Persistence TODO

## Plan
- [x] Reproduce and trace why auto-discovery persists MAC-like target values into printer host.
- [x] Apply minimal root-cause fix in discovery normalization to persist IP/host endpoint values only.
- [x] Add regression test for MAC target + valid IP candidate behavior.
- [x] Run verification gates.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests '*PrinterOnboardingDiscoveryTest*'`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause: `toDiscoveredPrinter()` prioritized parsed `target` host even when it contained MAC-like values (for example `64:C6:D2:FA:8D:70`), so onboarding saved MAC into `host`.
- Fix: reject MAC-like host candidates (`target` and `ipAddress`) and prefer real IP/host endpoint when building `DiscoveredPrinter`.
- Added regression test `toDiscoveredPrinter_prefersRealIpWhenTargetContainsMacAddress`.
- Verification passed on April 2, 2026:
- `./gradlew :composeApp:testDebugUnitTest --tests '*PrinterOnboardingDiscoveryTest*'`
- `./gradlew :composeApp:compileDebugKotlinAndroid`

# Printer Test Print 57mm Clip Follow-up TODO

## Plan
- [x] Reproduce root cause from user feedback: centered title clipping on 57mm test print.
- [x] Apply minimal-impact fix only in onboarding `testPrint` formatting path.
- [x] Run Android compile verification gate.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Updated `PrinterService.testPrint(...)` to use a conservative width (`24` cols) on narrow paper for test-print lines.
- This avoids centered-title clipping (`Prueba de impresora exitosa`) on physical 57mm rolls while keeping normal ticket parser behavior intact.
- Verification passed on April 2, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Epson Test Print Cut + 57mm Width Handling TODO

## Plan
- [x] Centralize paper-column mapping so both 57mm and 58mm use narrow width.
- [x] Make onboarding `testPrint` text width-aware to prevent overflow on 57mm.
- [x] Add full-cut to partial-cut compatibility fallback in Android and iOS Epson engines.
- [x] Extend shared printer tests for 57mm wrapping and run verification gates.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests '*TicketLayoutParserTest*'`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [ ] `./gradlew :composeApp:compileKotlinIosSimulatorArm64`

## Review Notes
- Added shared paper-width column mapping (`57`/`58` => `32`, default wide => `48`) in `TicketPrintModels.kt` and reused it from `TicketLayoutParser`.
- Updated `PrinterService.testPrint(...)` to generate width-aware text lines (wrapped by paper columns) and keep `Cut()` at the end.
- Added full-cut compatibility fallback in platform engines:
- Android: `FULL_CUT_FEED` retries with `CUT_FEED` on `ERR_PARAM`/`ERR_ILLEGAL`/`ERR_UNSUPPORTED`.
- iOS: `EPOS2_FULL_CUT_FEED` retries with `EPOS2_CUT_FEED` on `EPOS2_ERR_PARAM`/`EPOS2_ERR_ILLEGAL`/`EPOS2_ERR_UNSUPPORTED`.
- Extended `TicketLayoutParserTest` with a `57mm` narrow-width wrapping assertion.
- Verification passed on April 2, 2026:
- `./gradlew :composeApp:testDebugUnitTest --tests '*TicketLayoutParserTest*'`
- `./gradlew :composeApp:compileDebugKotlinAndroid`
- iOS verification remains blocked by pre-existing unrelated errors in:
- `features/auth/ui/login/viewmodel/LoginViewModel.kt` (`Dispatchers.IO` visibility)
- `features/expenses/ui/cufe/CufeImportViewModel.kt` (`Dispatchers.IO` visibility)
- `iosMain/features/quotes/ui/preview/PdfPreview.ios.kt` (unresolved iOS preview symbols)
- Android follow-up (April 2, 2026): cut still failed on device despite successful print text.
- Root cause per `ePOS_SDK_Android_um_en_revAK.pdf` (`addCut`): command can be ignored unless called at the beginning of a line, and `FULL_CUT_FEED` is not consistently executed across models.
- Applied Android-specific reliability fix in `AndroidEpsonPrinterEngine`:
- Force line boundary before `addCut` (`addText("\\n")`).
- Use compatibility-first cut mode order: `CUT_FEED` -> `PARAM_DEFAULT` -> `FULL_CUT_FEED`.
- Keep retry on compatible SDK errors (`ERR_PARAM`/`ERR_ILLEGAL`/`ERR_UNSUPPORTED`) with stage logging.
- Verification passed on April 2, 2026:
- `./gradlew :composeApp:compileDebugKotlinAndroid`

# Printer Test Print Cut Fix TODO

## Plan
- [x] Trace `PrinterOnboardingViewModel.testPrint()` and downstream print call path.
- [x] Apply minimal fix so test print always executes with cutter enabled.
- [x] Run compile verification gate and capture result.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Updated `PrinterOnboardingViewModel.testPrint()` to force `supportsCutter = true` on the draft config used for test prints.
- This keeps save/update behavior untouched while ensuring test prints request cutter behavior consistently.
- Verification passed on April 2, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Printer Advanced Config Collapse TODO

## Plan
- [x] Wrap printer `SwitchRow` advanced options into a collapsible card titled `Otras configuraciones`.
- [x] Keep it collapsed by default and expand/collapse on user interaction.
- [x] Run compile verification gate and capture result.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Moved `Imprimir automáticamente al facturar` and `Impresora habilitada` switches into a new card section.
- Added collapse state with default `false` (`rememberSaveable`) and chevron toggle icon (`ExpandMore/ExpandLess`).
- Verification passed on April 2, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Printer Discovery Header Reload UX TODO

## Plan
- [x] Replace `Reescanear` button with trailing reload action in the `Impresoras disponibles` header row.
- [x] Show primary-colored spinner in place of the reload icon while `SCANNING`.
- [x] Run compile verification gate and capture result.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `AutomaticDiscoverySection` now renders header layout as requested:
- `[Impresoras disponibles][spacer][reload icon/spinner]`.
- Reload uses `Icons.Rounded.Refresh` with `MaterialTheme.colorScheme.primary`.
- Spinner uses `CircularProgressIndicator` with `MaterialTheme.colorScheme.primary`.
- Verification passed on April 2, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Printer Discovery Status UX TODO

## Plan
- [x] Remove automatic-discovery status text card from `PrinterOnboardingScreen`.
- [x] Show loading indicator in `Reescanear` button and keep it disabled while discovery status is `SCANNING`.
- [x] Handle `UNSUPPORTED` by switching to manual mode and showing alert message.
- [x] Handle `ERROR` by showing alert message.
- [x] Run compile verification gate and capture result.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Updated `PrinterConfigStep` with status side effects:
- `UNSUPPORTED` => switches to manual mode and shows `Este dispositivo no soporta búsqueda automática.`
- `ERROR` => shows `No se pudo iniciar la búsqueda automática.`
- Removed the `statusMessage` UI card from automatic discovery section.
- `Reescanear` now displays `CircularProgressIndicator` while scanning and remains disabled.
- Verification passed on April 2, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Printer Onboarding WhatsApp Icon CTA TODO

## Plan
- [x] Locate the `Adquirir mi impresora` CTA in printer onboarding landing and confirm existing icon resources/imports.
- [x] Add a WhatsApp-style icon to the CTA with minimal visual impact and consistent white content color.
- [x] Run compile verification gate and capture result.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Updated `PrinterOnboardingScreen.kt` so the `Adquirir mi impresora` button now shows a leading chat icon (`Icons.AutoMirrored.Rounded.Chat`) plus white text for WhatsApp CTA consistency.
- Verification passed on April 2, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Printer Auto Discovery Onboarding (Android + iOS) TODO

## Plan
- [x] Add shared discovery contracts/models/service for Epson LAN discovery and target parsing/dedup.
- [x] Add Android/iOS Epson discovery engines and wire them in platform DI modules.
- [x] Integrate onboarding ViewModel state/actions for automatic mode default, 8s scan window, selection flow, and discovery lifecycle stop/start.
- [x] Update onboarding step 3 UI with `Automático`/`Manual` modes, discovered printers list, reduced auto config, and save-button enable gate after successful test.
- [x] Add shared unit tests for discovery mapping/state flow and onboarding auto/manual behavior.
- [x] Run verification gates and document results.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew :composeApp:compileKotlinMetadata`
- [x] `./gradlew :composeApp:testDebugUnitTest --tests '*PrinterOnboarding*'`

## Review Notes
- Added shared discovery stack:
- `PrinterDiscoveryEngine` contract.
- `PrinterDiscoveryService` flow state with start/stop/clear and dedupe by target/IP host key.
- Discovery models (`DiscoveredPrinter`, `PrinterDiscoveryState`, parser helpers for `TCP/TCPS` targets).
- Added Android discovery implementation using Epson `Discovery.start/stop` + `FilterOption` with printer-only TCP scan; emulator detected as unsupported (SDK limitation).
- Added iOS discovery implementation using `Epos2Discovery.start/stop` + delegate bridge and mapped callbacks into shared discovery candidates.
- Wired DI:
- platform modules now provide `PrinterDiscoveryEngine`.
- common module provides `PrinterDiscoveryService`.
- `PrinterOnboardingViewModel` now receives `discoveryService`.
- Updated onboarding state/VM:
- new `PrinterConfigMode` (`AUTOMATIC` default, `MANUAL` fallback).
- automatic mode starts discovery on CONFIG entry, auto-stops after 8s, supports rescan, stops on mode/step exits and on clear.
- auto-mode selection populates host/port and locks endpoint editing in reduced flow; manual mode preserves full input form.
- save button is now disabled until successful test (`uiState.canSave`).
- Updated Step 3 UI:
- mode selector cards (`Automático`/`Manual`).
- automatic discovery status panel, discovered-printer selectable list, empty state, rescan/manual fallback actions.
- reduced auto config view (detected printer summary + branch/billing + test/save).
- Added shared tests in `PrinterOnboardingDiscoveryTest`:
- target parsing (`TCP`, `TCPS`, custom port),
- discovery service dedupe and reset behaviors,
- onboarding state save eligibility in automatic mode.
- Additional iOS compilation check run:
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64` still fails due pre-existing unrelated issues in login/cufe/quotes iOS files, not in printer discovery files.

# Printer Onboarding Custom Loading Animation TODO

## Plan
- [x] Ensure `print-invoice-an.lottie` is present in shared compose resources files.
- [x] Allow onboarding loading sheet to use a custom loading animation file.
- [x] Wire printer onboarding loading sheet to `files/print-invoice-an.lottie`.
- [x] Run Android compile verification gate.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Copied `/Users/oscar/Downloads/print-invoice-an.lottie` into `composeApp/src/commonMain/composeResources/files/print-invoice-an.lottie`.
- Extended `LoadingSheet`/`LoadingBottomSheet` with optional `loadingAnimationFile` parameter while preserving trailing-lambda compatibility.
- Wired printer onboarding loading sheet to `loadingAnimationFile = "files/print-invoice-an.lottie"`.
- Verification passed: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Epson Test Print Cut Command TODO

## Plan
- [x] Ensure printer test print always appends cut command.
- [x] Run Android compile verification gate.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Updated `PrinterService.testPrint(...)` to always append `PrintCommand.Cut()` so test prints trigger cutter regardless of saved `supports_cutter`.
- Verification passed: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Android Epson Emulator Timeout Follow-up TODO

## Plan
- [x] Compare current Android implementation against Epson v2.36 sample/docs and isolate emulator timeout root cause for `192.168.0.12`.
- [x] Patch Android Epson print path with minimal-impact fixes (network security + robust target/timeout handling).
- [x] Run focused Android verification gates and capture evidence in review notes.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew :composeApp:testDebugUnitTest --tests '*TicketLayoutParserTest*'`

## Review Notes
- Verified Epson v2.36 Android sample requires app-level network security config with cleartext traffic enabled (`ePOS2_Printer` sample manifest + `res/xml/network_security_config.xml`). Our app was missing that setup.
- Hardened host/target normalization in `PrinterConfig`:
- Strips protocol/path noise from host input (`http://`, `tcp://`, trailing path, etc.).
- Supports non-default explicit ports by trying `TCP:host:port` first, then `TCP:host` fallback.
- Keeps existing `443` behavior (`TCP:host`, then `TCPS:host`).
- Updated Android engine transport behavior:
- Reuses one `Printer` instance across retries (per Epson guidance to avoid create/destroy in tight loops).
- Uses configured `timeoutMs` for both `connect(...)` and `sendData(...)` transport timeout argument.
- Added Android manifest config parity with Epson sample:
- `android:networkSecurityConfig="@xml/network_security_config"`
- `android:usesCleartextTraffic="true"`
- Added focused regression tests for host/port target resolution variants in `TicketLayoutParserTest`.
- Verification passed:
- `./gradlew :composeApp:compileDebugKotlinAndroid`
- `./gradlew :composeApp:testDebugUnitTest --tests '*TicketLayoutParserTest*'`

# Android Epson Print Timeout Debug TODO

## Plan
- [x] Trace the Android Epson SDK flow against the official sample and isolate why mobile printing times out while web printing succeeds for `192.168.0.12`.
- [x] Patch the shared target resolution and Android Epson engine with the minimal root-cause fix plus stage-aware diagnostics.
- [x] Run Android verification gates and document the confirmed cause and outcome.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process :composeApp:testDebugUnitTest --tests '*TicketLayoutParserTest*'`

## Review Notes
- Root cause found: Android was treating `port == 443` as `TCPS:host`, but the Epson mobile SDK docs/samples use `TCP:host` and the working web flow negotiates host+port separately. That mismatch was producing `Epos2Exception.ERR_TIMEOUT (3)` against `192.168.0.12`.
- The Android engine also returned immediately after `sendData()` and disconnected in `finally`, instead of waiting for the Epson receive callback that confirms the print result.
- Fix applied:
- `PrinterConfig.connectionTarget()` now uses plain `TCP:host`, while `connectionTargets()` keeps `TCPS:host` only as a secondary fallback candidate for configured `443` printers.
- `AndroidEpsonPrinterEngine` now calls `addTextLang(...)` before the print data, uses `Printer.PARAM_DEFAULT` for SDK transport calls, waits for `ReceiveListener.onPtrReceive(...)`, retries disconnect cleanly on `ERR_PROCESSING`, and logs the failing stage/target with Epson symbolic error names.
- Added a regression assertion for the shared target resolution in `TicketLayoutParserTest`.

# KMP Epson Thermal Printing TODO

## Plan
- [x] Add shared printer models, Epson printer engine abstractions, CRUD repository/service flow, realtime cache sync, and ticket layout parser/command mapper.
- [x] Wire Android/iOS Epson SDK adapters and project build config for direct LAN printing.
- [x] Integrate ticket-aware POS order creation, post-order auto print, and order-details ticket reprint flow.
- [x] Add settings/branch entry points plus printer onboarding/manage/config UI flows.
- [x] Add focused shared parser tests and run verification gates.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew :composeApp:compileKotlinMetadata`
- [x] `./gradlew :composeApp:testDebugUnitTest --tests '*TicketLayoutParserTest*'`
- [ ] `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`

## Review Notes
- Added a new shared `features/printers` slice with Epson printer CRUD, local printer cache, `changed_printer` realtime invalidation, and strict ticket JSON parsing/command mapping.
- Vendored Epson Android (`ePOS2.jar` + `libepos2.so`) and iOS (`libepos2-static.xcframework`) SDK assets, then wired platform `PrinterEngine` implementations for LAN printing targets (`TCPS:host` for `443`, otherwise `TCP:host`).
- POS now conditionally requests `TICKET` when an active printer exists for the selected branch/billing point and attempts direct mobile printing after confirmed order creation without failing the sale on print errors.
- Order details now exposes `Reimprimir ticket` only for eligible orders and follows the required 0/1/many-printer reprint flow, including `INV_002` handling.
- Added printer settings/manage, first-time onboarding, and direct config screens, plus entry points from `SettingsScreen` and billing-point management.
- Added focused shared tests in `TicketLayoutParserTest`.
- iOS simulator link is still blocked by pre-existing repo issues unrelated to printers:
- `LoginViewModel.kt` and `CufeImportViewModel.kt` use `Dispatchers.IO` in a way that fails for iOS compilation.
- `composeApp/src/iosMain/kotlin/com/teco/ventago/features/quotes/ui/preview/PdfPreview.ios.kt` has unresolved iOS preview API references.
- Epson iOS cinterop itself is configured and generated successfully before those unrelated errors stop the build.

## Plan
- [x] Remove the draft-specific invoice sheet additions and keep a single payment-capture modal path.
- [x] Rewire `Facturar` to open `OrderScreenManualPaymentBottomSheetHost` for multi-method allocation before invoicing.
- [x] Ensure manual payment sheet seeds payable total correctly from open balance (fallback to order total).
- [x] Run compile verification gate and document outcomes.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Reverted the new draft-specific payment sheet and reused `OrderScreenManualPaymentBottomSheetHost` / `ManualPaymentBottomSheet` as requested.
- `Facturar` now opens the existing multi-payment modal so users can select all payment methods/amounts before invoicing.
- `showManualPaymentSheet(true)` now seeds `totalToChargeCents` with open receivable balance when available (fallback to order total), so invoice payment allocation targets pending balance.
- Verification passed on March 28, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Order Details Draft Invoice CTA TODO

## Plan
- [x] Trace current draft-order action gating in `OrderDetailsScreen` and identify why draft orders cannot be invoiced.
- [x] Add a draft-order invoicing action in `OrdersDetailsViewModel` that posts to manual payments endpoint with typed payload and loading feedback.
- [x] Show a primary `Facturar` button for eligible draft orders in `OrderDetailsScreen` and wire it to the new ViewModel action.
- [x] Run compile verification gate and document outcomes.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause: `OrderDetailsScreen` had no actionable draft-invoice CTA; the `Generar factura electrónica` branch used `onClick = { }`, so draft orders could not trigger invoicing.
- Added `canInvoiceDraftOrder()` + `invoiceDraftOrder()` in `OrdersDetailsViewModel`. The new flow sends a typed manual payment payload to `/api/v1/orders/{id}/payments/manual` using method type `3` (tarjeta crédito), order total amount, and Panama date-time, then refreshes the order and shows loading success/error feedback.
- Updated `OrderDetailsScreen` to show a primary success-style `Facturar` button for eligible draft orders and wired pending+paid invoices to `retryElectronicInvoice()` instead of a no-op.
- Verification passed on March 28, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Expenses UI Copy + Badge Color TODO

## Plan
- [x] Update `ExpenseDetailsScreen` action label from `Descargar Archivo` to `Descargar factura`.
- [x] Update free-limited-time badge text color in expense update/create form to light white text.
- [x] Run compile verification gate and document outcomes.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Updated expense details download CTA copy to `Descargar factura`.
- Updated `Gratis por tiempo limitado` badge text color to white in the expense form view.
- Verification passed on March 28, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Expense Edit Categorization Fresh Item IDs TODO

## Plan
- [x] Trace edit submit flow and confirm where stale pre-update item IDs are used for categorization payload.
- [x] Update edit flow to fetch refreshed expense detail after `updateExpense` and before `categorizeExpense`.
- [x] Map concept selections to refreshed items and build categorization payload with backend item IDs.
- [x] Run compile verification gate and document results.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause: `saveEditedExpense` built categorization payload from `state.items` right after `updateExpense`, so it could send stale pre-update `item_id` values.
- Fix: after `updateExpense`, the flow now fetches refreshed expense detail (`getExpense(expenseId)`) before building categorization payload.
- Added remapping helper that aligns selected concepts to refreshed backend items by `lineNumber` (fallback by index) and sends refreshed `item_id` values in `items[]`.
- Verification passed on March 28, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Expense Details Concept Selector UX TODO

## Plan
- [x] Review current `ExpenseDetailsScreen` concept sheet + selector component and define minimal-impact changes.
- [x] Add search input in concept selector bottom sheet with normalized matching (ignore spaces + case).
- [x] Add collapsible parent/folder behavior in concept selector tree to expand/collapse child branches.
- [x] Update concept selector card copy/color in `ExpenseDetailsScreen` to indicate it is clickable for assigning concepts.
- [x] Run compile verification gate and document outcomes.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Updated `ExpenseAccountSelectorField` with a top search input and normalized filtering (`lowercase` + whitespace removed) so concept-name search ignores spaces and case.
- Added folder-like parent branch collapsing/expanding in `ExpenseAccountPickerSheetContent`, including parent toggle affordance and collapsed child rendering.
- Updated expense concept selector cards in `ExpenseDetailsScreen` to show secondary-colored main text and explicit tap labels indicating concept assignment action.
- Verification passed on March 28, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Expense Details Edit Concepts CTA Color TODO

## Plan
- [x] Locate `Editar conceptos` CTA in `ExpenseDetailsScreen`.
- [x] Apply secondary color style to the CTA.
- [x] Run compile verification gate and document result.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Updated `Editar conceptos` CTA text color to `MaterialTheme.colorScheme.secondary` while preserving existing behavior/layout.
- Verification passed on March 28, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Expenses List Payment Status Payload Fix TODO

## Plan
- [x] Trace `expenses/list` request construction and confirm current `payment_status` payload shape.
- [x] Update request model + list ViewModel mapping so `payment_status` is serialized as a single string (not array).
- [x] Add regression test coverage for serialized request payload.
- [x] Run verification gate(s) and document outcomes.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests '*ExpenseConceptsTest*'`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause: `ListExpensesRequest.paymentStatus` was typed as `List<String>?`, so serialized payload emitted an array (`"payment_status": ["not_paid"]`) instead of backend-required string.
- Fixed by changing request field to `String?` and mapping from UI filter state with `state.paymentStatuses.firstOrNull()`.
- Added regression test `listExpensesRequestSerializesPaymentStatusAsString` to lock payload contract.
- Verified payload contract now serializes as:
- `{"business_id":4,"page":1,"page_size":10,"payment_status":"not_paid"}`

# Order Details Bottom Sheets Full-Screen Scroll Fix TODO

## Plan
- [x] Trace current `OrderDetailsScreen` modal sheet configs for register payment and receivables reprogramming.
- [x] Apply full-screen modal behavior (`skipPartiallyExpanded = true`) to those two sheets.
- [x] Ensure both sheet contents use full-height, internal vertical scroll so all dynamic rows remain reachable.
- [x] Run compile verification gate and document result.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `Register payment` and `Reprogram terms` sheets now use full-screen modal states (`skipPartiallyExpanded = true`) with scrollable full-height content containers to keep long dynamic content reachable.
- Follow-up UI tune applied per request: order-details `Registrar pago` outlined CTA now uses secondary color/border, and reschedule submit CTA now uses `TextButtonS` with secondary color.
- Verification passed on March 28, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Build Performance Investigation TODO

## Plan
- [ ] Capture baseline build time and hottest tasks via Gradle profile (`./gradlew :composeApp:assembleDebug --profile`).
- [x] Review current Gradle/KMP settings for caching, parallelism, KSP, and iOS target config.
- [ ] Propose/implement low-risk flags (parallel, VFS watch, incremental KSP) and optional iOS-target toggle for local dev (if approved).
- [ ] Re-run profile and compare results.

## Verification Gates
- [ ] `./gradlew :composeApp:assembleDebug --profile`
- [ ] `./gradlew :composeApp:assembleDebug`

## Review Notes
- Pending baseline build profile and decision on which optimizations to apply.

# Cedula Pattern Expansion (User-Confirmed Valid Cases) TODO

## Plan
- [x] Expand Panama cédula regex rules to accept all user-confirmed valid examples for regular/prefix variants.
- [x] Move the nine user-confirmed examples from invalid to valid in cédula unit tests and keep a reduced truly-invalid set.
- [x] Make validator case-insensitive via normalization inside `isValidPanamaCedula`.
- [x] Update inline validation copy examples to match expanded accepted inputs.
- [x] Run focused verification gates and document outcomes.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests '*Cedula*'`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Expanded regex coverage in `PanamaCedulaUtils` to accept the user-confirmed valid numeric and prefix variants, including shorter numeric groups where required.
- `isValidPanamaCedula` now validates normalized input (`trim + uppercase`), making lowercase prefixed values (for example `pe-...`) valid.
- Updated `PanamaCedulaUtilsTest` so the nine user-provided samples are asserted as valid and kept only truly-invalid shapes in the invalid list.
- Updated inline cédula validation examples in add/edit/customer-form viewmodels to match accepted inputs.
- Verification passed: `./gradlew :composeApp:testDebugUnitTest --tests '*Cedula*'` and `./gradlew :composeApp:compileDebugKotlinAndroid`.

# Cedula Regex Follow-up (3-digit Suffix) TODO

## Plan
- [x] Extend regular cédula regex to accept `8-888-846` while preserving previously accepted regular variants.
- [x] Add regression coverage for `8-888-846` as valid and near-shape invalid cases.
- [x] Update inline validation copy examples to include the new valid shape.
- [x] Run focused verification gates and document outcomes.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests '*Cedula*'`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Updated regular numeric cédula regex to accept `...-...` suffix for the regular branch (`8-888-846`) while keeping prior accepted regular variants.
- Added test coverage for `8-888-846` as valid and `8-888-84` as invalid, preserving the previous invalid-shape checks.
- Updated validation help copy in all three customer form/viewmodel flows to include `8-888-846`.

# Customer Duplicate Dialog UX TODO

## Plan
- [x] Replace inline duplicate-customer error rendering with modal dialog UX in customer create screens.
- [x] Add ViewModel clear-error actions so dialog dismissal closes the error state cleanly.
- [x] Reuse localized `understood` action text and add a specific duplicate-customer dialog title string.
- [x] Run Android compile verification and document results.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `CustomerFormScreen` and POS `AddCustomerScreen` now present API error messages inside an `AlertDialog` instead of inline red text.
- Dialog confirm action uses localized `Res.string.understood` and dismiss/confirm both clear ViewModel error state via `clearErrorMessage()`.
- Added localized title `customers_duplicate_dialog_title` in `values/strings.xml` and `values-es/strings.xml`.
- For duplicate-customer errors, ViewModels now stop loading state (`hideLoading()`) and rely on the dialog as the primary feedback UX.
- Verification: `./gradlew :composeApp:compileDebugKotlinAndroid` passed.

# Customer Duplicate Error Mapping TODO

## Plan
- [x] Add typed duplicate-customer error mapping for customer create responses (`CU_004` / 409 path) in shared API/repository flow.
- [x] Handle duplicate-customer exception in customer create viewmodels (`CustomerFormViewModel` and POS `AddCustomerViewModel`) with explicit message including customer name and RUC.
- [x] Surface duplicate message in customer create UI flows and remove existing no-op error handling.
- [x] Add focused test coverage for repository duplicate mapping and run verification gate(s).

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests com.teco.ventago.features.customers.CustomerModelsAndOrdersRequestTest`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Added `ApiError.CUSTOMER_ALREADY_EXISTS` (`CU_004`) mapping and `DuplicateCustomerException` so the repository can distinguish duplicate-customer create failures from generic bad requests.
- `CustomerRepository.createCustomer` now throws `DuplicateCustomerException` for `CU_004` before fallback `BadRequestException` handling.
- `CustomerFormViewModel` and POS `AddCustomerViewModel` now catch duplicate errors and publish a clear message: `Ya existe un cliente con nombre "<name>" y RUC "<ruc>".`
- POS add-customer UI now renders `errorMessage` in both reduced/full forms and removed the previous no-op TODO branch for invalid RUC feedback.
- Added repository regression coverage: `repositoryThrowsDuplicateCustomerExceptionForCu004`.
- Verification: focused customer unit test and `compileDebugKotlinAndroid` both pass.

# Cedula Regex Follow-up (Regular Format) TODO

## Plan
- [x] Adjust regular Panama cédula regex to accept the user-reported valid format `8-888-8456` while keeping existing accepted formats.
- [x] Update cédula validator tests to cover the new valid format and nearby invalid variants.
- [x] Update inline validation copy to include the new valid regular example.
- [x] Run focused verification gates and document outcomes.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests '*Cedula*'`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Updated regular regex to accept both regular numeric shapes: `1-1234-12345` and `8-888-8456`.
- Added `8-888-8456` as valid test input and added nearby invalid variants (`8-88-8456`, `8-8888-8456`) to prevent over-broad matches.
- Updated all inline validation messages to include the new valid regular example.

# Final Consumer Cedula Validation (POS + Customer Form) TODO

## Plan
- [x] Add shared Panama cédula validator utility (`normalizePanamaCedula`, `isValidPanamaCedula`) with the six accepted regex patterns.
- [x] Wire cédula normalization + inline validation state into POS add flow (`AddCustomerState`, `AddCustomerViewModel`, `AddCustomerScreen`) and block submit on invalid non-empty final-consumer cédula.
- [x] Wire cédula state + inline validation rendering into POS legacy edit flow (`EditCustomerState`, `EditCustomerViewModel`, `EditCustomerScreen`) without backend payload changes.
- [x] Wire same validator into active customers form flow (`CustomerFormState`, `CustomerFormViewModel`, `CustomerFormScreen`) and block create save on invalid non-empty final-consumer cédula.
- [x] Add focused validator unit tests and run verification gates.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests '*Cedula*'`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Added shared validator (`PanamaCedulaUtils`) with exact accepted regex patterns and explicit normalization via `trim + uppercase`.
- POS add flow now normalizes cédula on input, surfaces inline error in cédula field, and blocks `createCustomer()` when `FINAL_CONSUMER` cédula is non-empty and invalid.
- POS legacy edit flow now has `cfCedula` + `cfCedulaError` state and renders a final-consumer cédula field with the same inline validation behavior (no backend update payload changes).
- `CustomerForm` create flow now normalizes cédula, displays inline cédula error state, and blocks save when final-consumer cédula is non-empty and invalid.
- Added `PanamaCedulaUtilsTest` coverage for all accepted patterns, invalid samples (`00-00-0000` etc.), and normalization behavior.

# Login Must-Change-Password Hard Block TODO

## Plan
- [x] Trace login token-claim path and ensure `must_change_password` can be read before any persistence/sign-in side effects.
- [x] Implement auth-service guard so `must_change_password=true` aborts login before Firebase/token/cache writes.
- [x] Add blocked login UI state in `LoginScreen` with English copy, password-related vector icon, and a "Back to Login" action.
- [x] Run focused verification gate and document outcomes.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests com.teco.ventago.core.authz.AuthzJwtDecoderTest`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `AuthzJwtDecoder` now extracts `must_change_password` (top-level or nested in `data`) and exposes it via `UserAuthzClaims.mustChangePassword`.
- `AuthService` now enforces `must_change_password` before Firebase sign-in and before token/cache writes for login/register flows; when true it signs out and throws `MustChangePasswordException`.
- `LoginViewModel` now handles `MustChangePasswordException` with a dedicated blocked UI state instead of generic auth error handling.
- `LoginScreen` now shows a blocking English message with a lock vector and a `Back to login` button when the claim requires password change.
- Added decoder regression coverage for the new claim in `AuthzJwtDecoderTest`.
- Follow-up copy update: blocked screen text was changed to friendly Spanish wording and removed technical claim/key wording.

# Fresh Login Redirect + Orders Loader Trap TODO

# Products View Category Drill-down Fix TODO

## Plan
- [x] Trace why `products:view` users cannot open category detail from `CategoriesManageScreen`.
- [x] Allow category row click to navigate into category items in read-only mode.
- [x] Keep create-only routes/actions protected while enabling view-only drill-down.
- [x] Run focused verification gate and document results.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `CategoriesManageScreen` now navigates on category click for all authorized product users; edit options remain guarded by `canManageCategories`.
- `CategoriesManageViewModel.selectCategory` now stores selected category for both read-only and manage users.
- `AuthzNavigation` now maps `EditCategoryScreen` to `RouteKey.PRODUCT_DETAILS` (view/create), while add/modify category screens remain `RouteKey.CATEGORY_MANAGE` (create-only).

# Scopes UI Audit (Quotes/Expenses/Products) TODO

## Plan
- [x] Audit listed Quotes screens and gate mutation CTAs using quote create/update permissions (create includes modify).
- [x] Add explicit expenses action permissions in details state/viewmodel and hide detail mutation CTAs (update vs delete).
- [x] Add runtime guards in expenses viewmodel mutation methods to prevent unauthorized API actions.
- [x] Gate products app-bar action icons for add category/add item using manage-categories permission.
- [x] Run focused verification gate and document outcomes.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `QuoteSuccessScreen` now hides `new_quote` CTA unless `canCreateQuote` is true.
- Expense details now split mutation permissions in state (`canUpdateExpenseAction`, `canDeleteExpenseAction`) and hide mutation CTAs accordingly (concept edit, payment create/edit/mark, expense edit/duplicate/delete).
- `ExpenseDetailsViewModel` now enforces runtime guards for update/delete expense actions (payments + categorization + delete) to prevent unauthorized mutations even if UI entry points are bypassed.
- Product top app-bar add actions are now permission-aware: `CategoriesManageActions` and `EditCategoryActions` render add icons only when `canManageCategories` is true.
- Product category/item create-edit screens now hide primary save/add buttons when `products:create` is missing, and item save paths are runtime-guarded (`AddItemViewModel` / `EditItemViewModel`) via `canManageItems`.
- Quote settings editability is now scoped by quote permissions (`ActionKey.QUOTES_UPDATE`) instead of owner settings-modify permission, and quote-settings save is blocked at runtime if that permission is missing.

# Customer Action Scopes Audit TODO

## Plan
- [x] Hide delete-customer CTA in `CustomerDetailsScreen` when `customer:delete` permission is missing.
- [x] Audit customer action surfaces mapped by `ScopeKey` and patch missing UI gates in customer views/action bars.
- [x] Add runtime guards in customer details ViewModel for delete/address mutations to mirror UI gating.
- [x] Run focused verification gate and document results.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `CustomerDetailsState/ViewModel` now split action permissions into `canEditCustomerAction` (`customer:create`) and `canDeleteCustomerAction` (`customer:delete`).
- `CustomerDetailsScreen` now hides the delete customer CTA when delete scope is missing and hides billing-address mutation actions when create scope is missing.
- `ClientListActions` top app bar now hides the add-customer icon unless the user has customer action access (`create` or `delete`), aligned with customer list/search behavior.
- Customer details mutation methods (`deleteCustomer`, `createAddress`, `updateAddress`, `deleteAddress`) now have runtime permission guards.

# Customer Details Edit CTA Scope Guard TODO

## Plan
- [x] Locate the edit customer CTA rendering path in customer details flow.
- [x] Add a customer-edit permission flag to details state sourced from authz in `CustomerDetailsViewModel`.
- [x] Hide the "Editar cliente" button in `CustomerDetailsScreen` when create permission is missing.
- [x] Run focused verification gate and document results.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `CustomerDetailsState` now includes `canEditCustomerAction`.
- `CustomerDetailsViewModel` now evaluates `ActionKey.CUSTOMERS_CREATE` from the authenticated user and publishes the flag to UI state.
- `CustomerDetailsScreen` now conditionally renders the edit customer button only when `uiState.canEditCustomerAction` is true.

# Customer Add Actions Scope Guard TODO

## Plan
- [x] Confirm where add-customer entry points exist in POS customer search and customers list screens.
- [x] Wire authz-derived `canAddCustomerAction` state in both customer search/list viewmodels based on `customers:create` or `customers:delete`.
- [x] Gate add-customer navigation/FAB rendering in UI using that state.
- [x] Run focused verification gate and document results.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `SearchCustomerViewModel` and `CustomersListViewModel` now compute `canAddCustomerAction` from shared authz evaluator (`CUSTOMERS_CREATE || CUSTOMERS_DELETE`).
- `CustomersListScreen` now renders the add FAB only when `uiState.canAddCustomerAction` is true.
- `SearchCustomerView` now routes `CustomerNotFound` to add-customer only when `canAddCustomerAction` is true; otherwise it opens customers list.
- `SearchCustomerView` event handling now reads the latest ViewModel state (`viewModel.uiState.value`) to avoid stale permission capture from initial composition.

## Plan
- [x] Trace fresh-login bootstrap redirect path and confirm why sub-users land on `Orders` instead of `Home`.
- [x] Change bootstrap redirect behavior so authenticated users always start at `HomeScreen`.
- [x] Guard route-authz redirect execution until `currentUser` is hydrated to avoid transient wrong-route redirects during login startup.
- [x] Run focused verification gates and document outcomes.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests com.teco.ventago.core.authz.AuthzEvaluatorTest`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Post-login startup redirect in `App.kt` no longer uses `fallbackScreenFor(...)`; for authenticated users with business + invoicing configured it now always targets `HomeScreen`.
- Route-level authz guard in `App.kt` now waits for non-null `currentUser` before enforcing route permissions, preventing startup redirects based on partial auth state.
- `OrdersViewModel` now defers its initial `loadOrders()` until a valid `businessId` is available, and reloads when business context changes, avoiding startup fetches with `-1` that could leave the list in a loading trap.
- Removed temporary `ASDADS` debug prints from `App.kt` that were used to trace redirect behavior.
- A transient KSP generated-source race occurred when running test + compile in parallel; clean sequential rerun passed both gates.

# Settings Logout Access Fallback TODO

## Plan
- [x] Confirm root cause: users without `settings:view` cannot reach `SettingsScreen`, which currently contains the only sign-out entry point.
- [x] Implement minimal-impact authz fix so authenticated users can access settings page/menu while preserving owner-only edit controls.
- [x] Run focused verification gates and document outcomes.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests com.teco.ventago.core.authz.AuthzEvaluatorTest`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `RouteKey.SETTINGS_PAGE` and `MenuKey.SETTINGS` now use `allowAll`, so sub-users without `settings:view` can still open `SettingsScreen` and use sign out.
- Owner-only mutation controls remain protected by `ActionKey.SETTINGS_MODIFY` / `uiState.canModifySettings`, so this change only restores access for logout/basic read-only options.
- Updated `AuthzEvaluatorTest` expectations to the current authz model and added explicit coverage for settings route access without `settings:view`.

# Folios Card Owner-Only Visibility TODO

## Plan
- [x] Restrict folios/invoicing plan card rendering to owner-main users only.
- [x] Apply the same owner-only gating in both `HomeScreen` and `HomeSummaryScreen` to keep behavior consistent.
- [x] Run focused verification gate and document results.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Folios/invoicing plan card now renders only when `uiState.showFolioPurchase` is true (owner-main), so sub-accounts do not see it.
- The owner-only folios card behavior is aligned in both `HomeScreen` and `HomeSummaryScreen`.

# Home Access + Home Buttons Authz Fix TODO

## Plan
- [x] Update authz policies so `HomeScreen` is accessible to all authenticated users and `HomeSummaryScreen` is restricted to owner main or `home:dashboard`.
- [x] Extend `HomeState/HomeViewModel` with explicit UI permissions for customers/expenses/support-card visibility.
- [x] Refactor Home action cards section (`HomeScreen` quick actions block) to show cards only when corresponding permissions are allowed.
- [x] Run focused verification gate and document results.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `RouteKey.HOME` and `MenuKey.HOME` are now allow-all (authenticated), so sub-users no longer lose Home access due missing `home:dashboard`.
- `RouteKey.HOME_SUMMARY` now allows owner-main or users with `home:dashboard`.
- Home quick-action cards now render independently by permission: quotes/POS remained scoped as before, and customers/expenses now use dedicated route access flags.
- `SupportCard` now renders only for owner main accounts.

# POS Cart Edit Product Scope Guard TODO

## Plan
- [x] Inspect cart item modification entry points (name/price/discount) and current authz state fields.
- [x] Add `invoice:edit_product` authz state in POS ViewModel/State and gate cart item edit interactions in UI.
- [x] Add ViewModel runtime guards for price/discount mutation methods used by cart edit flow.
- [x] Run focused verification gate and document results.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- POS authz state now includes `canEditProduct` sourced from `ActionKey.ORDERS_EDIT_PRODUCT`.
- Cart item rows no longer open the modify-item sheet when `canEditProduct` is false, preventing product price/discount edits from the cart UI.
- `PosViewModel` now blocks `updateCartLine`, `setLineOverridePrice`, and `setLineDiscount` when the user lacks `invoice:edit_product`, so price/discount writes are guarded at runtime too.

# POS Payment Scope UI Guard TODO

## Plan
- [x] Inspect current PaymentScreen action visibility for `invoice:create_draft` vs `invoice:create`.
- [x] Restrict PaymentScreen UI so invoice-confirm controls require `canCreateInvoice`; draft action requires `canCreateDraft`.
- [x] Run focused verification gate and document results.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `PaymentScreen` no longer renders manual payment selectors, installment controls, allocation summary, or "Confirmar cobro" when `canCreateInvoice` is false.
- "Guardar sin cobrar" now renders only when `canCreateDraft` is true (and still excluded for credit/debit notes), matching scope behavior for draft-only users.

# Customer Tax Presets + CRUD Parity TODO

# KMP Authz Scope Sync TODO

## Plan
- [x] Add shared authz types/evaluator and extend auth state with JWT-derived scopes + owner/sub-user flags.
- [x] Wire central route fallback and shell/navigation gating, including a dedicated unauthorized screen.
- [x] Apply screen/action guards for Home, Orders, Quotes, Expenses, Products category management, and owner-only Settings controls.
- [x] Add focused unit coverage for JWT authz decoding, evaluator semantics, fallback resolution, and authz-driven UI/viewmodel behavior.
- [x] Run verification gates and document results.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Shared authz now lives in common code (`AuthzJwtDecoder`, `AuthzEvaluator`, route/menu/action policies) and `AuthService` enriches the cached/current `User` from JWT scopes plus owner/sub-user claims on login, refresh, restore, and token rotation.
- Shell/navigation now resolve allowed destinations before rendering, use beta cache snapshots for gating, hide unauthorized bottom-nav entries, and send fully blocked users to a dedicated unauthorized screen instead of an invalid fallback.
- Home, Orders, Expenses, Quote details/summary, POS quote/order/custom-product flows, product category management, and owner-only Settings controls now all read from the shared authz model, with runtime guards added to mutation handlers instead of relying on visibility alone.
- Added common authz regression tests for JWT claim decoding, evaluator semantics/fallback ordering, and authz-driven bottom-nav visibility; both `testDebugUnitTest` and `compileDebugKotlinAndroid` passed.

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

# POS/Quote Decimal Quantity 4dp TODO

## Plan
- [x] Add quantity utility helpers (sanitize/normalize/display/request + cents multiplication) in `NumberUtils`.
- [x] Migrate cart quantity model and math from `Int` to decimal and update viewmodel qty APIs.
- [x] Update POS quantity UI (`ModifyCartItemSheet` and cart row actions/displays) to support 4dp input and 1.0 step buttons.
- [x] Update order/quote request contracts to send `items[].quantity` as fixed-4 decimal string and keep `totals.quantity_items` as line count.
- [x] Harden order/quote quantity parsing/display compatibility for decimal quantities.
- [x] Add focused unit tests for quantity helpers, cart math with fractional qty, and request builder quantity serialization.
- [x] Run verification gates and record outcomes.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests com.teco.ventago.utils.NumberUtilsTest`
- [x] `./gradlew :composeApp:testDebugUnitTest --tests com.teco.ventago.features.pos.domain.models.CartLineTest`
- [x] `./gradlew :composeApp:testDebugUnitTest --tests com.teco.ventago.features.quotes.domain.QuoteRequestBuilderTest`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Quantity now supports decimal values up to 4dp across POS cart state and edit flow (`CartLine.quantity` moved to `Double`, with normalization and decimal-safe cents math).
- Added quantity helpers in `NumberUtils` for sanitize/normalize/display/request formatting and cents-by-quantity half-up rounding.
- `ModifyCartItemSheet` now accepts decimal keyboard input (`.`/`,`), enforces max 4dp, keeps min `0.0001`, and applies `+/- 1.0` stepping.
- Order/quote request payloads now send `items[].quantity` as fixed-4 decimal string (`"x.xxxx"`), and `totals.quantity_items` now uses `cart.size`.
- Quote edit hydration no longer truncates quantity to int (`toInt()` removed), preserving decimal quantities.
- Order-line compatibility hardening added by switching `OrderLineDto.quantity` to a flexible `Double` serializer and rendering quantity with decimal-aware UI formatting in order/POS screens.
- Added tests:
  - `NumberUtilsTest` (sanitize/normalize/format/multiply behavior),
  - `CartLineTest` (fractional quantity totals/discount/tax rounding),
  - `QuoteRequestBuilderTest` (quote quantity serialization + order DTO quantity string contract).

# CxC Operativa en Order Details (KMP) TODO

## Plan
- [x] Extender contrato de órdenes (`Order`, DTOs y requests/responses) con `receivable_terms`, `order_payments` extendido y payloads de pago auto/manual, reprogramación y void.
- [x] Implementar endpoints nuevos/modificados en `IOrdersProvider`/`OrdersProvider` + `IOrdersRepository`/`OrdersRepository` + `OrderService` con logging y rethrow.
- [x] Reemplazar estado/lógica legacy de `OrderDetailsState` y `OrdersDetailsViewModel` por flujos CxC (registrar pago, reprogramar, anular) con validaciones en centavos y fechas Panamá.
- [x] Adaptar `OrderDetailsScreen` a card de cuotas operativas, lista de pagos registrados (regla count >=2), y nuevos modales.
- [x] Aplicar regla de método `Mixto`/único/`N/A` en listado de órdenes.
- [x] Agregar pruebas unitarias (serialización requests + parseo modelos + validaciones VM) y ejecutar gates.

## Verification Gates
- [x] `./gradlew :composeApp:testDebugUnitTest --tests com.teco.ventago.features.orders.CxcOrderDetailsContractTest --tests com.teco.ventago.features.orders.ui.order_details.viewModel.OrdersDetailsViewModelCxcTest`
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [ ] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` (bloqueado por errores preexistentes fuera de este alcance: `LoginViewModel` con `Dispatchers.IO` no accesible en iOS y `PdfPreview.ios.kt` con referencias UIKit no resueltas)

## Review Notes
- Se extendió `Order` para parsear `receivable_terms` y `order_payments` con `id/payment_date/voided_at/void_reason`, manteniendo compatibilidad con payloads previos.
- Se agregaron contratos tipados para `find/order-id`, reprogramación de receivables y void de pagos, además de soporte `applications[]` en `payments/manual`.
- `OrdersProvider`, `OrdersRepository` y `OrderService` incorporan los nuevos endpoints y respuestas, con el patrón de manejo de errores del proyecto (try/catch + logging + rethrow).
- `OrderDetailsState` y `OrdersDetailsViewModel` ahora incluyen estados/acciones CxC para registrar pago (auto/manual con aplicaciones), reprogramar cuotas y anular pagos, con validación en centavos y mapping de error `O_RP_002`.
- `OrderDetailsScreen` fue adaptado a:
  - card `Cuotas de pago` con resumen `Pendiente/Vencido`,
  - tabla `Número/Vence/Monto/Adeudado/Pagado/Estado`,
  - exclusión de términos cancelados (`status=4`),
  - lista `Pagos registrados` solo cuando hay 2+ pagos, con fecha sin hora y badge de estado,
  - modales de registrar/reprogramar/anular.
- Se aplicó la regla `Mixto/único/N/A` en `OrderListItem` para la lista de órdenes.
- Tests nuevos:
  - `CxcOrderDetailsContractTest` (parseo/serialización/envelope error code),
  - `OrdersDetailsViewModelCxcTest` (validaciones CxC).

# OrdersScreen Crash (NotImplementedError) TODO

## Plan
- [x] Identificar el origen exacto del `NotImplementedError` reportado en `OrdersScreen.kt`.
- [x] Reemplazar ramas `TODO()` en el `collect` de eventos por manejo seguro sin crash.
- [x] Ejecutar gate de compilación Android para validar el fix.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- El crash venía de `LaunchedEffect` en `OrdersScreen` que consumía `viewModel.events` con tres ramas `TODO()` (`LoadingOrdersConnectionError`, `LoadingOrdersError`, `LaunchSettings`).
- Se reemplazaron esas ramas por `Unit` para evitar `NotImplementedError` en tiempo de ejecución.
- `LaunchSettings` sigue siendo atendido por `OrdersScreenActions`, que es donde se ejecuta `permissionsManager.launchSettings()`.

# Orders Parse Log Fix (missing fields in nested orders payload) TODO

## Plan
- [x] Confirmar raíz del log de parseo al cargar órdenes (`ReceivableTermDto.status` ausente).
- [x] Hacer robusta la deserialización de `ReceivableTermDto` para tolerar `status` faltante.
- [x] Agregar test de regresión para payload de orden con `receivable_terms` sin `status`.
- [x] Hacer robusta la deserialización de `OrderHistoryDto` para tolerar `status_id` faltante.
- [x] Agregar test de regresión para payload de orden con `order_histories` sin `status_id`.
- [ ] Ejecutar gates de verificación enfocados.

## Verification Gates
- [ ] `./gradlew :composeApp:testDebugUnitTest --tests com.teco.ventago.features.orders.CxcOrderDetailsContractTest`
- [ ] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- El backend en algunos listados está enviando `receivable_terms` sin `status`, lo que rompía la deserialización porque el campo era obligatorio.
- Se añadió default `status = PaymentStatus.UNPAID.id` en `ReceivableTermDto`.
- Se agregó test para validar que, si falta `status`, el parseo de `Order` no falla y se aplica valor por defecto.
- También se observó payload con `order_histories` sin `status_id`; `OrderHistoryDto` ahora tiene defaults defensivos (`statusId/note/changedBy/createdAt`) para evitar fallas de decode por campos faltantes.

# Order Details Currency Symbol Fix ($$ -> $) TODO

## Plan
- [x] Identificar por qué en `Cuotas de pago` y `Pagos registrados` se renderiza `$$`.
- [x] Corregir el helper de formato monetario para garantizar símbolo `$` único en este flujo.
- [x] Validar el cambio por inspección de código (sin ejecutar Gradle por solicitud del usuario).

## Verification Gates
- [ ] No ejecutados por solicitud explícita del usuario: "Do not run tests".

## Review Notes
- `formatDollarFromCents` concatenaba manualmente `"$"` sobre `formatNumberToMoney(...)`, que en Android ya devuelve símbolo de moneda, provocando doble símbolo.
- Se reemplazó por formateo manual desde centavos (`$<units>.<decimals>`) para garantizar salida estable como `$100.00`.

# Cuotas Table Compact Layout TODO

## Plan
- [x] Reemplazar label de columna `Número` por `#` para reducir ancho.
- [x] Compactar visual de `Estado` a indicador de color y mover explicación a ayuda contextual (`?`).
- [x] Ajustar pesos de columnas para mejorar render en pantallas pequeñas.
- [x] Verificar por inspección de código (sin Gradle por preferencia del usuario).

## Verification Gates
- [ ] No ejecutados por preferencia del usuario (sin correr Gradle/tests).

## Review Notes
- En `TermsTableHeader` se cambió `Número` por `#`.
- La columna `Estado` ahora usa icono `?` que abre un diálogo de ayuda con leyenda de colores.
- En cada fila se reemplazó el chip de texto de estado por un punto de color (verde/azul/naranja), reduciendo significativamente el ancho usado por esa columna.

# Reprogramar Cuotas Bottom Sheet Scroll TODO

## Plan
- [x] Hacer scrolleable el contenido del bottom sheet de `Reprogramar cuotas de pago`.
- [x] Mantener layout y validaciones existentes sin cambios de comportamiento.
- [x] Verificar por inspección de código (sin Gradle/tests por preferencia del usuario).

## Verification Gates
- [ ] No ejecutados por preferencia del usuario.

## Review Notes
- Se agregó `.verticalScroll(rememberScrollState())` al `Column` raíz de `RescheduleTermsBottomSheet`, permitiendo ver todo el contenido al agregar más de 2 cuotas.

# KMP Epson Thermal Printing TODO

## Plan
- [ ] Vendor Epson Android/iOS SDK assets into the repo and wire platform build configuration/DI.
- [ ] Add shared printer contracts, repository/service/cache-sync, and ticket layout parsing/command mapping.
- [ ] Extend order/POS contracts for `TICKET`, auto-print after confirmed sales, and ticket fallback fetch.
- [ ] Add order-details reprint flow with visibility guards, loading feedback, and 0/1/many-printer selection.
- [ ] Add printer settings/onboarding/config UI flows and navigation entry points from settings and billing points.
- [ ] Run verification gates and document outcomes.

## Verification Gates
- [ ] `./gradlew :composeApp:testDebugUnitTest`
- [ ] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [ ] `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`

## Review Notes
- Pending implementation.

# POS Branch/Billing Point Prefill Cache TODO

## Plan
- [x] Persist last-used POS branch + billing point combination in local cache (business-scoped key).
- [x] Prefill branch + billing point on POS first step when cached combination is available and valid.
- [x] Preserve quote-flow defaults and existing fallback behavior when cache is missing/stale.
- [x] Run Android compile verification gate.

## Verification Gates
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Added a business-scoped local cache key in `PosViewModel` to persist the last selected `branchCode|billingPoint` combination.
- POS sale flow now attempts to restore that combination during initial branch hydration (`currentState.branches.isEmpty()`), and applies it only when both branch and billing point still exist.
- Existing quote defaults are preserved: quote flow keeps using its default branch logic and does not write/read sale prefill cache.
- User-driven selection updates (`onBranchSelected`, `onBillingPointSelected`) now persist the current combination, so returning to the first POS step prefills automatically.
- Verification passed on April 3, 2026: `./gradlew :composeApp:compileDebugKotlinAndroid`.
- Follow-up fix (April 3, 2026): `resetForNewSale()` no longer forces billing point index `0`; it now resolves cached branch/billing-point (or a safe current fallback) and reapplies it so the next invoice keeps the last used selection.
- Follow-up fix #2 (April 3, 2026): when entering POS from Home, business context could become available after branch list hydration; now `PosViewModel` reapplies persisted branch/billing-point once business is loaded, so prefill works even on fresh Home -> POS entry.

# Payments Navigation Split (Titles + Back Stack) TODO

## Plan
- [x] Separar navegación de pagos en rutas dedicadas: métodos, Yappy, ACH con comprobante y PayPal.
- [x] Hacer que el título del app bar sea por ruta (`Métodos de pago`, `Yappy`, `ACH con comprobante`, `PayPal`).
- [x] Ajustar `OnboardingPaymentScreen` para modo home vs modo detalle por método sin mezclar vistas en una sola ruta.
- [x] Corregir back behavior: desde detalle de método regresar a lista de métodos (no salir a Settings).
- [x] Ejecutar gate de compilación Android/KMP.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Se añadió `Res.string.ach_with_proof` para título de la ruta ACH.
- `PaymentsHomeScreen` ahora usa `Res.string.payment_methods` para título de página principal de métodos.
- `addPaymentsNavigation(...)` ahora reutiliza `PaymentMethodsViewModel` en las rutas de método y usa `OnboardingPaymentScreen` en modo detalle por método.
- Se removió la lógica legacy del app bar que inyectaba `YappyViewModel` para interceptar back; el back vuelve al comportamiento de `navigateUp()`, y el manejo de flujo se hace por rutas.

## Follow-up Bugfix (method click showed shimmer in home)
- [x] Al abrir método desde `ConfiguredList`, inicializar estado de método (`viewModel.onOpenMethod(method)`) antes de navegar para evitar estado intermedio inválido.
- [x] Al salir de rutas de método (botón interno o back top bar), restaurar estado home con `onEnterHomeRoute()` antes de `navigateUp()`.
- [x] Eliminar fallback de shimmer en home para `MethodDetail*` y mostrar lista configurada como fallback seguro.
- [x] Re-ejecutar gate de compilación.

### Verification Gate
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Crash Fix (Yappy onboarding "Atrás")
- [x] Reemplazar `getBackStackEntry(PosScreens.Payments.name)` por owner seguro en rutas de pagos.
- [x] Usar `rememberSafeGraphOwner(...)` con fallback al `backStackEntry` actual para evitar crash cuando el grafo `Payments` ya fue removido del back stack durante transición.
- [x] Verificar compilación Android.
- [x] Corregir doble navegación al salir de onboarding de método (evitar pop extra a Settings al tocar "Atrás").

### Verification Gate
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileDebugKotlinAndroid`

# Notifications Pagination Follow-up TODO

## Plan
- [x] Cambiar batch de notificaciones a `10` para carga inicial y `load more`.
- [x] Hacer top-up automático en primera carga cuando el primer payload deja `<= 1` notificación visible.
- [x] Cubrir el caso con test unitario del `NotificationsViewModel`.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests '*Notification*'`

## Review Notes
- El `NotificationsViewModel` ahora usa `DEFAULT_PAGE_SIZE = 10`.
- En `refreshFirstPage`, si la primera página deja muy pocos ítems visibles por filtros (`dismissed/removed`), solicita páginas adicionales (offset incremental) hasta completar visibilidad razonable o agotar `total`.

# Notifications UI Interaction Follow-up TODO

## Plan
- [x] Reemplazar CTA de descarte (`X`) por gesto swipe en cada notificación.
- [x] Remover acción de engranaje del app bar en `NotificationsScreen`.
- [x] Mostrar solo hora dentro de cada card (la fecha ya vive en los headers de sección).
- [x] Ejecutar verificación de compilación + tests de notificaciones.
- [x] Endurecer swipe para requerir gesto casi completo antes de descartar (evitar falsos positivos durante scroll).

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests '*Notification*'`

## Review Notes
- Swipe habilitado `EndToStart` con fondo de acción de borrado en color `secondary`; el ícono de cierre inline fue eliminado.
- `PosScreens.NotificationsScreen` quedó sin acciones de top bar (sin engranaje).
- El timestamp en card se normalizó a formato de hora `HH:mm`.
- Ajuste de compatibilidad Material3: el estado visual de swipe usa `targetValue == SwipeToDismissBoxValue.EndToStart` para esta versión de Compose.
- Se configuró `positionalThreshold` al `95%` del ancho (`FULL_SWIPE_DISMISS_THRESHOLD_FRACTION = 0.95f`) para exigir full swipe práctico antes de disparar `dismiss`.

# Notifications UI Tweaks Follow-up TODO

## Plan
- [x] Cambiar `Load more` de botón primario a `TextButtonS` con color `secondary`.
- [x] Remover tarjeta informativa "Tus notificaciones se actualizan en tiempo real".
- [x] Agregar pull-to-refresh sobre la lista para recargar notificaciones.
- [x] Exponer estado `isRefreshing` y acción `refreshNotifications()` en ViewModel/State.
- [x] Ejecutar compilación Android y tests de notificaciones.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests '*Notification*'`

## Review Notes
- `NotificationsContent` ahora usa `TextButtonS` para `notifications_load_more` con `MaterialTheme.colorScheme.secondary`.
- Se eliminó `NotificationsInfoCard` y sus strings asociadas del flujo de render.
- Se añadió `rememberPullRefreshState` + `PullRefreshIndicator` y `Modifier.pullRefresh(...)` siguiendo patrón de listas existentes (Orders/Expenses/Quotes).
- Se añadió `isRefreshing` en `NotificationsState` y `refreshNotifications()` en `NotificationsViewModel` para recarga manual por swipe.

# Customer Final Consumer Create Contract TODO

## Plan
- [x] Verificar el contrato real de `POST /api/v1/customers/create` en provider/DTO y normalización de payload para cliente consumidor final.
- [x] Corregir serialización y normalización del body para que el endpoint reciba los campos esperados (`null` explícitos cuando corresponda, sin strings vacíos espurios).
- [x] Exigir `address_line`, `province`, `district` y `corregimiento` en creación de clientes desde Home y POS cuando el tipo no es extranjero.
- [x] Cubrir el contrato con tests enfocados y correr compilar/tests relevantes.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests '*Customer*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause de contrato: `CustomerProvider.createCustomer(...)` serializaba con `explicitNulls = false`, por lo que el body omitía claves nulas; además, el flujo POS enviaba `""` en varios opcionales. Ahora el payload se normaliza y se envian `null` explícitos para respetar el contrato esperado por `/api/v1/customers/create`.
- `buildCreateCustomerRequestBody(...)` quedó cubierto con test para `fe_customer_type = "02"` y los campos `email/phone/tax_id/tags/tax_retention_*` en `null`.
- Root cause adicional confirmado por log de app: `CustomerCreatedDto` tenía campos nullable (`email/phone/tax_id/tags`) sin default, así que Kotlinx los trataba como required cuando el backend omitía `phone`, `tax_id` y `tags` en la respuesta de consumidor final. El DTO ahora acepta esos campos omitidos y también mapea `tax_exempt` / `tax_retention_*` de la respuesta esperada.
- Home (`CustomerFormViewModel`) y POS (`AddCustomerViewModel`) comparten la regla `CustomerCreateValidation.requiredLocationMessage(...)` para bloquear creación sin `address_line`, `province`, `district` o `corregimiento` cuando el cliente no es extranjero.
- El flujo POS reducido ahora incluye los campos de provincia/distrito/corregimiento/direccion y `createCustomer()` muestra `LoadingSheet` desde el inicio de la mutación.

# Customer Create Field Alerts TODO

## Plan
- [x] Reemplazar la validación genérica al guardar por errores inline sobre los campos requeridos en Home create customer.
- [x] Aplicar el mismo patrón de errores inline en POS create customer (full y reduced).
- [x] Ejecutar compile gate de Android/KMP.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Los formularios de creación ahora conservan estado de error por campo requerido (`name`, `province`, `district`, `corregimiento`, `addressLine`) en sus `UiState`.
- Al tocar `Crear cliente` con campos faltantes, los `OutlinedTextField` muestran `isError + supportingText` y los `DMDropDownField` muestran borde de error con texto inline debajo, en lugar de depender de un alert genérico.
- Los errores se limpian al corregir el campo o al volver a seleccionar provincia/distrito/corregimiento, para que la recuperación sea inmediata.

# Customer Details Cedula TODO

## Plan
- [x] Confirm the customer details contract/model already exposes `cedula_cf`.
- [x] Update `CustomerDetailsScreen` general information card to show `Cédula` when `cedula_cf` is present.
- [x] Hide `Número RUC` and `Dígito Verificador` when rendering that `cedula_cf` branch.
- [x] Run compile verification for the affected KMP module.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `CustomerDetails` already mapped `cedula_cf`, so the change stayed in the details UI only.
- `GeneralInfoCard` now prioritizes `Cédula` when `cedula_cf` is present on a non-foreign customer, and suppresses the `Número RUC` / `Dígito Verificador` rows for that branch.
- Compile gate passed with existing project warnings only (`ksp`/KMP beta/deprecation warnings unrelated to this change).

# POS Success Notification Permission TODO

## Plan
- [x] Remove notification permission request from `HomeScreen` first-entry flow.
- [x] Request notification permission from POS `SuccessScreen` for non-failed order success flows.
- [x] Preserve existing Android/iOS platform permission implementations.
- [x] Run compile and static verification gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`
- [x] Static check: `HomeScreen` no longer calls `requestNotificationPermission()`.
- [x] Static check: failed order UI path does not request notification permission.

## Review Notes
- `HomeScreen` still creates `platformState` for the existing support email action, but no longer requests notification permission when Home becomes visible.
- `SuccessScreen` now collects POS state and requests notification permission only when there is a non-failed successful order/payment-link result.
- Android/iOS platform permission code was not changed.
- Compile gate passed with existing project warnings (`ksp` version, cinterop commonization, manifest/provider, expect/actual beta, and deprecations).

# Notification Order Details Deep Link TODO

## Plan
- [x] Resolve in-app notification order detail URLs to internal order navigation instead of browser.
- [x] Reuse existing `OrdersScreenRoute(orderNumber=...)` flow so order details load by order number.
- [x] Add focused resolver tests for `ventago.tecodigi.com/orders/order-details.html?orderNumber=...`.
- [x] Run notification tests and KMP/Android compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.notifications.NotificationActionResolverTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Notification resolver now maps `/orders/order-details.html?orderNumber=...` URLs to an internal order-details action.
- `NotificationsViewModel` emits `NavigateToOrderDetails`, and `NotificationsScreen` navigates through `OrdersScreenRoute(orderNumber=...)` so the existing order lookup endpoint and details flow are reused.
- Unknown absolute URLs still open externally, and ACH notification links keep their existing internal ACH route.
- Targeted resolver test and compile gate passed with existing project warnings (`ksp` version, cinterop commonization, expect/actual beta, and deprecations).

# Order Cancel API Error Mapping TODO

## Plan
- [x] Extend shared order/API error parsing so cancel-order backend codes `INV_003` and `INV_004` are treated as real errors.
- [x] Map cancel-order failures to the backend-provided message when available, with durable fallbacks for known invoice-cancellation codes.
- [x] Surface the mapped cancel-order failure message from `OrdersDetailsViewModel` into the order details cancel flow.
- [x] Add focused regression tests and run targeted verification gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.orders.ui.order_details.viewModel.OrderMutationErrorMapperTest --tests com.teco.ventago.features.orders.ui.order_details.viewModel.OrdersDetailsViewModelCxcTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `INV_003` and `INV_004` are now recognized by shared API parsing, and order cancel now treats any `success=false/errorCode!=null` response as a failure instead of silently returning `false`.
- Cancel-order error messages now prefer the backend `data.message`, so the saas-e-invoice responses render the exact Spanish copy returned by Ventago.
- `OrderDetailsScreen` keeps the cancel sheet open on backend failure, shows the mapped message inline in the same visual danger treatment, and also emits the snackbar/error loading feedback already used by the screen.
- Focused test and compile gates passed after stopping Gradle and clearing the affected Kotlin/KSP cache directories to avoid the existing incremental-cache file-lock issue when the initial verification was run in parallel.
