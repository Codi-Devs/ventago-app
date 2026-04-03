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
