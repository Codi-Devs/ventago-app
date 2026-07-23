# Crashlytics Mapping Upload Automation TODO

## Plan
- [x] Explicitly wire public/POS release bundle tasks to their Crashlytics mapping upload tasks.
- [x] Verify Gradle task graph includes mapping upload when building both release AABs.
- [x] Record the Crashlytics vs Firestore distinction.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:bundlePublicRelease :androidApp:bundlePosRelease --dry-run`
- [x] `git diff --check -- androidApp/build.gradle.kts tasks/todo.md`

## Review Notes
- R8 mappings are uploaded to Firebase Crashlytics, not Firestore. Firestore does not deobfuscate crash reports.
- `bundlePublicRelease` now explicitly finalizes with `uploadCrashlyticsMappingFilePublicRelease`.
- `bundlePosRelease` now explicitly finalizes with `uploadCrashlyticsMappingFilePosRelease`.
- Dry-run verification showed both upload tasks in the task graph after their matching AAB bundle tasks.

# Epson R8 Preservation Fix TODO

## Plan
- [x] Inspect Epson SDK sample ProGuard rules and package contents.
- [x] Broaden app keep rules to match Epson's official sample guidance.
- [x] Build optimized release bundles to verify R8 accepts the updated rules.
- [x] Record the runtime validation requirement for Epson discovery/printing.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:bundlePublicRelease :androidApp:bundlePosRelease`
- [x] `git diff --check -- androidApp/proguard-rules.pro tasks/todo.md tasks/lessons.md`

## Review Notes
- Epson SDK sample projects all use `-keep class com.epson.** { *; }` and `-dontwarn com.epson.**`.
- The SDK jar includes native/JNI-facing packages outside `com.epson.epos2`, including `com.epson.epsonio`, `com.epson.eposdevice`, and `com.epson.eposprint`; preserving only `com.epson.epos2.**` can allow R8 to remove classes used from `libepos2.so`.
- Updated app rules to preserve the full Epson namespace while leaving R8 enabled for the rest of the release app.
- `:androidApp:bundlePublicRelease` and `:androidApp:bundlePosRelease` pass with the broadened Epson rules. Real-device validation still needs Epson printer discovery and a print job from the optimized public release build.

# Android R8 Release Optimization TODO

## Plan
- [x] Confirm current Android release optimization settings and AGP behavior.
- [x] Enable R8 optimization with conservative keep rules for external printer integration boundaries.
- [x] Build public and POS release bundles to catch R8/resource shrink failures.
- [x] Record verification result and remaining runtime risks.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:bundlePublicRelease :androidApp:bundlePosRelease`
- [x] `git diff --check -- androidApp/build.gradle.kts androidApp/proguard-rules.pro tasks/todo.md`

## Review Notes
- Enabled R8 for Android release builds with `isMinifyEnabled = true`, `isShrinkResources = true`, Android's default `proguard-android-optimize.txt`, and app-specific keep rules.
- Added conservative keep rules for `com.epson.epos2.**` and `recieptservice.com.recieptservice.**` because those packages cross native SDK and external Binder/service boundaries.
- `:androidApp:bundlePublicRelease` and `:androidApp:bundlePosRelease` passed with R8 enabled. Generated release AABs are in `androidApp/build/outputs/bundle/publicRelease` and `androidApp/build/outputs/bundle/posRelease`.
- Remaining risk is runtime-only: install a release build and smoke-test login, navigation, API serialization, camera scanning, Firebase messaging/deep links, file sharing, Epson printing, and H10P POS printing before rollout.

# Android Compile SDK 37 Resolution TODO

## Plan
- [x] Confirm where the Android compile SDK value is defined and which SDK platforms are installed locally.
- [x] Change only the compile SDK catalog value to a Gradle-resolvable installed platform.
- [x] Run Android/KMP compile verification and diff checks.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileAndroidMain`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:processPublicDebugManifest`
- [x] `git diff --check -- gradle/libs.versions.toml tasks/todo.md`

## Review Notes
- `android-compileSdk` now uses API 36, which is installed as a standard Gradle-resolvable platform at `/Users/oscar/Library/Android/sdk/platforms/android-36`.
- The previous API 37 SDK is installed locally as `android-37.0`, while Gradle was resolving integer compile SDK 37 to `android-37`, causing `Could not find compile target android-37`.
- `android-targetSdk` remains 37, and the generated app manifest still contains `android:targetSdkVersion="37"`.
- `composeApp:compileAndroidMain` and `androidApp:processPublicDebugManifest` passed with existing warnings only.

# POS Invoice Branch Selection Public App TODO

## Plan
- [x] Restore branch and billing-point selectors in POS invoice configuration.
- [x] Keep selectors disabled only while POS provisioning is active.
- [x] Guard provisioned branch/billing selection so it applies only when POS provisioning is active.
- [x] Run common metadata compile and diff verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] `git diff --check`

## Review Notes
- POS invoice configuration shows `Sucursal` and `Punto de facturación` selectors again.
- Those selectors are disabled only when `uiState.posProvisioningActive` is true, so public/non-POS builds can choose branch and billing point.
- Provisioned branch/billing selection now returns early unless provisioning is required and provisioned, and application also requires `posProvisioningActive`.
- Common metadata compilation passed with existing project warnings, and `git diff --check` passed.

# POS Devices Module Card Rollback TODO

## Plan
- [x] Move `Dispositivos POS` back from quick actions to module cards for non-POS builds.
- [x] Restore URL-image support on module cards and remove it from quick-action icons.
- [x] Run common metadata compile and diff verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] `git diff --check`

## Review Notes
- `Dispositivos POS` is back in the modules grid for non-POS builds and remains guarded by `RouteKey.SETTINGS_POS_DEVICES`.
- Module cards again support URL-backed icons, and the H10 POS image uses `https://ventago.b-cdn.net/app/h10pos.png`.
- Quick actions are back to vector/drawable icons only.
- Common metadata compilation passed with existing project warnings, and `git diff --check` passed.

# POS Payment Preview Navigation Crash TODO

## Plan
- [x] Switch Payment preview app-bar action from generic route navigation to PosScreens enum navigation.
- [x] Verify metadata compile and diff check.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] `git diff --check`

## Review Notes
- The Payment app-bar preview action now uses the `PosScreens` navigation callback, matching enum-route navigation through `destination.name`.
- This prevents Compose Navigation from trying to resolve the generic route type `PosScreens`.
- Common metadata compilation passed with existing project warnings, and `git diff --check` passed.

# POS Devices Quick Action TODO

## Plan
- [x] Move `Dispositivos POS` from modules into quick actions for non-POS builds.
- [x] Render the H10 POS URL image in the quick action left-icon style with a larger size.
- [x] Run common metadata compile and diff verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] `git diff --check`

## Review Notes
- `Dispositivos POS` now appears in quick actions for non-POS builds, still guarded by `RouteKey.SETTINGS_POS_DEVICES`.
- The H10 POS image is loaded from `https://ventago.b-cdn.net/app/h10pos.png` in the left-icon shortcut layout at a larger 52dp size.
- Module cards are back to vector-only icons.
- Common metadata compilation passed with existing project warnings, and `git diff --check` passed.

# POS Payment Preview Action TODO

## Plan
- [x] Move invoice preview from payment option body buttons into the Payment screen top app bar.
- [x] Remove duplicate manual-payment draft action when the standalone draft option is already available.
- [x] Remove repeated preview outlined buttons from payment sections.
- [x] Run common metadata compile and diff verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] `git diff --check`

## Review Notes
- The Payment route now exposes invoice preview through a secondary-colored eye icon in the existing global top app bar.
- Manual payment content no longer shows the extra "Guardar sin facturar" action; users use the standalone draft payment mode instead.
- Payment link, Yappy onsite, and draft sections no longer render duplicate "Vista previa" outlined buttons.
- Common metadata compilation passed with existing project warnings, and `git diff --check` passed.

# POS Devices Menu Image Icon TODO

## Plan
- [x] Let menu module cards render URL image icons.
- [x] Use the H10 POS image URL for the non-POS `Dispositivos POS` menu item.
- [x] Run common metadata compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Menu module items now support vector and URL-backed icons.
- The non-POS `Dispositivos POS` menu card now uses `https://ventago.b-cdn.net/app/h10pos.png` instead of the generic print icon.
- Common metadata compilation passed with existing project warnings.

# POS Devices Menu Entry TODO

## Plan
- [x] Add POS devices module item on menu for non-POS builds.
- [x] Ensure POS device route can make the app menu visible for scoped users.
- [x] Run common metadata compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Non-POS builds now show a `Dispositivos POS` module card in the menu when the user is authorized for `RouteKey.SETTINGS_POS_DEVICES`.
- The app menu visibility check now includes the POS devices settings route, so scoped users can reach the menu entry.
- Common metadata compilation passed with existing project warnings.

# POS Devices Landing WhatsApp CTA TODO

## Plan
- [x] Replace no-device landing refresh CTA with WhatsApp availability contact.
- [x] Run common metadata compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- The no-device POS landing CTA now opens WhatsApp to Ventago support with a prefilled H10 POS availability message.
- Common metadata compilation passed with existing project warnings.

# POS Devices UI Adjustments TODO

## Plan
- [x] Map branch and billing point codes to display names in POS device list/detail/edit UI.
- [x] Move location access into the first detail card as a map icon and remove the standalone location card.
- [x] Apply secondary color to POS device card titles/icons, rename telemetry to device data, and remove last reboot.
- [x] Normalize active permission switch colors.
- [x] Run common metadata compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- POS device list/detail/edit UI now displays branch and billing point names from active branch options instead of raw codes.
- The standalone location card was removed; the detail header shows only a secondary-colored map/location icon when coordinates are available.
- Section titles and POS device action icons now use the secondary color, telemetry was renamed to "Datos del dispositivo", and last reboot was removed.
- Permission switches now use secondary/on-secondary active colors with neutral inactive colors.
- Common metadata compilation passed with existing project warnings.

# POS Device H10 Image TODO

## Plan
- [x] Replace generic POS device visuals with the H10 POS CDN image.
- [x] Run common metadata compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- POS device landing, list rows, and details header now use the H10 POS CDN image at `https://ventago.b-cdn.net/app/h10pos.png`.
- Common metadata compilation passed with existing project warnings.

# POS Device Config Permissions TODO

## Plan
- [x] Add POS config contract/provider/repository/service flow for `GET /api/v1/devices/{deviceId}/pos-config`.
- [x] Load POS config when a device details screen opens and merge branch, billing point, status, and permissions into selected device state.
- [x] Show skeleton loading on the details screen while POS config is loading.
- [x] Update focused contract tests and run verification gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] Focused Android host test for `PosDevicesContractsTest`

## Review Notes
- Device permissions now load from `GET /api/v1/devices/{deviceId}/pos-config` when the POS device details route opens.
- The returned POS config is merged into the selected device for branch code, billing point code, status, and permissions while preserving list telemetry.
- The detail screen shows shimmer skeleton cards while config permissions are loading; permission editing remains disabled when config loading fails or omits permissions.
- Common metadata compilation and focused POS contract tests passed with existing project warnings.

# POS Devices Settings TODO

## Plan
- [x] Add POS device admin contracts, provider, repository, service, and DI wiring.
- [x] Add `settings:modify_pos_devices` authz scope, route/action policies, settings entry, and navigation routes.
- [x] Build POS devices active-list, no-device landing, detail, branch/billing edit sheet, map action, and permissions editor UI.
- [x] Add focused validation/serialization/parsing tests where the existing test setup supports it.
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileAndroidMain`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosSimulatorArm64`
- [x] Focused Android host tests: `PosDevicesContractsTest`, `AuthzEvaluatorTest`, and `AuthzNavigationTest`
- [ ] Full `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest`

## Review Notes
- Added POS devices settings administration for active business devices, including no-device landing, active list, detail telemetry, Google Maps action, branch/billing bottom sheet, and permission switches.
- Added API contracts for device listing, branch/billing update, and permission update with selected-business `X-Business-ID` and token-refresh retry through existing provider patterns.
- Added `settings:modify_pos_devices` authorization across scope constants, route/action policy, settings visibility, and navigation route mapping.
- Common metadata, Android main, iOS simulator compilation, focused POS contract tests, and focused authz tests passed with existing project warnings.
- Full Android host tests still fail on two unrelated existing assertions: `CustomerModelsAndOrdersRequestTest.customerFormStateUsesFullForeignCountryCatalog` and `OrdersDetailsViewModelCxcTest.shouldShowRetryInvoiceButtonRequiresPaidPendingOrFailedInvoiceAndNotCancelled`.

# POS Scanner Low Quality Camera TODO

## Plan
- [x] Add optional camera-preview scan controls for product barcode optimization without changing default scanner behavior.
- [x] Restrict POS scanner detection to retail/product barcode formats.
- [x] Tune POS scanner stability thresholds for faster low-quality-camera reads.
- [x] Add Android tap-to-focus, center auto-focus, torch control, and mild default zoom.
- [x] Add POS scanner torch UI.
- [x] Run common, Android, and iOS compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata :composeApp:compileAndroidMain :composeApp:compileKotlinIosSimulatorArm64`

## Review Notes
- Added optional scanner controls to `CameraPreview` with defaults that preserve existing scanner behavior, including opt-in tap-to-focus.
- POS scanner mode now opts into retail/product barcode formats, faster stability thresholds, center auto-focus, tap-to-focus, torch control, and a 1.4x default zoom on Android.
- Android CameraX applies torch updates without remounting the preview and keeps continuous scanning active.
- iOS actual was updated only to match the KMP signature; the requested POS optimizations remain Android-focused.
- Common metadata, Android main, and iOS simulator compilation passed. Existing project warnings remain unrelated.

# POS Scanner Android Preview Clip TODO

## Plan
- [x] Identify why the camera image can expand below the half-screen scanner region while Compose overlays stay fixed.
- [x] Force Android CameraX preview into a clip-friendly implementation.
- [x] Add explicit Compose clipping to the scanner camera half.
- [x] Run Android-focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata :composeApp:compileAndroidMain`

## Review Notes
- Android CameraX `PreviewView` now uses `ImplementationMode.COMPATIBLE` so it respects embedded Compose layout clipping instead of drawing below the scanner half after the surface initializes.
- The POS scanner camera container and preview modifier now use explicit `clipToBounds()` while preserving the 50/50 camera/products layout.
- Common metadata and Android main compilation passed. Existing project warnings remain unrelated.

# POS Flavor Settings And Success Reprint TODO

## Plan
- [x] Hide the printer settings entry in POS builds while preserving it for public builds.
- [x] Add POS success-screen ticket reprint state/action using the existing printer service.
- [x] Add the success-screen reprint button only when a POS issued order can be reprinted.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:assemblePosDebug`

## Review Notes
- POS builds hide the "Impresoras térmicas" settings entry; public builds keep the existing settings/onboarding navigation.
- The POS success screen now shows "Reimprimir ticket" only for POS builds with an issued order id/order number.
- Reprint fetches the order ticket through `PrinterService`, resolves the selected branch/billing point printer, shows loading/disabled state, and reports success/failure through the existing snackbar pattern.
- Common metadata compilation and POS debug assembly passed. Existing project warnings remain unrelated.

# POS Scanner Half Layout TODO

## Plan
- [x] Replace scanner camera/status weights with explicit 50/50 heights from the available viewport.
- [x] Keep the existing route bottom cart button as the only cart CTA.
- [x] Show scanner status and products being added in the lower half.
- [x] Run Kotlin metadata compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Scanner mode now uses `BoxWithConstraints` and explicit `maxHeight / 2` heights for camera and lower activity panel.
- The lower half shows scanner status/current scanned product plus a scrollable list of products already added to cart.
- The scanner screen still does not render an extra cart CTA; it relies on the existing POS bottom cart button.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Scanner Multi Scan TODO

## Plan
- [x] Add a camera preview continuous-scanning option without changing existing one-shot scanner screens.
- [x] Use continuous mode from POS scanner mode so different products and repeat same-barcode products can scan in one camera session.
- [x] Keep duplicate protection scoped to POS scanner UI.
- [x] Run Kotlin metadata, Android main, and iOS simulator compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileAndroidMain`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosSimulatorArm64`

## Review Notes
- `CameraPreview` now supports `singleShot`, defaulting to `true` so existing scanner screens keep one-shot behavior.
- POS scanner mode passes `singleShot = false`, so the camera analyzer keeps emitting scans without remounting the preview.
- Android `StableBarcodeAnalyzer` no longer sets `handled = true` in continuous mode and uses a short same-value cooldown instead.
- iOS `CameraCoordinator` uses the same continuous-mode cooldown and no longer stops the session for POS scans.
- POS UI same-barcode cooldown was lowered to 500 ms so a second unit with the same barcode is accepted promptly.
- Common metadata, Android main, and iOS simulator Kotlin compilation passed. Existing project warnings remain unrelated.

# POS Order Recovery Same Flow TODO

## Plan
- [x] Trace POS checkpoint restore prompt and product-back navigation behavior.
- [x] Prevent restore prompt when the current POS ViewModel already has active invoice/order data.
- [x] Run Kotlin metadata compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- `checkOrderCreationCheckpointForRestore()` now skips recovery when current in-memory POS state already has meaningful user data.
- Back navigation from products to invoice/customer information stays inside the same invoice session and no longer shows the restore dialog.
- Fresh entries with no in-memory order data can still restore a persisted checkpoint after an interruption.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Product Search Barcode SKU TODO

## Plan
- [x] Extend POS product query matching to include product barcode.
- [x] Extend POS product query matching to include product SKU.
- [x] Run Kotlin metadata compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- POS product search now matches name, description, barcode, and SKU.
- Barcode and SKU checks are nullable-safe and use the same case-insensitive partial matching as name search.
- Existing product category filtering remains unchanged.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Barcode Scanner Continuous Add TODO

## Plan
- [x] Remove duplicated scanner-mode cart CTA and keep only the bottom cart button.
- [x] Remove the bottom close scanner action and keep the top X close control.
- [x] Add scanned products to the cart immediately and make the product card cancel action undo one scanned unit.
- [x] Keep the camera mounted continuously after successful scans instead of restarting scanner mode.
- [x] Run Kotlin metadata compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Scanner mode no longer renders its own cart CTA; the existing POS bottom cart button remains the only cart action.
- Scanner mode no longer renders the bottom "Cerrar scanner" button; the camera overlay X is the only close action.
- A valid barcode now beeps and immediately adds one unit to cart through the existing `addItemToCart` path.
- The scanned-product card now acts as an undo/cancel surface and removes one scanned unit through the matching cart path.
- Successful scans no longer remount `CameraPreview`; a short same-barcode cooldown prevents duplicate callbacks while keeping different barcode scans immediate.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# Guided POS Customer Creation TODO

## Plan
- [x] Add guided-flow state and ViewModel transitions for invoicing-enabled POS customer creation.
- [x] Replace the full POS add-customer form with type, main info, and optional info steps.
- [x] Include tax exempt/retention payload fields and domestic default address behavior.
- [x] Apply guided create flow to clients-list customer creation.
- [x] Remove initial customer type preselection and use secondary colors for selected type cards.
- [x] Apply foreign/local customer UI follow-ups across creation, POS customer info, and details.
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Replaced only the invoicing-enabled POS add-customer path with a guided three-step flow.
- Customer type cards advance into type-specific required fields; returning to step 1 preserves and highlights the selected type.
- Domestic customer type selection defaults address to `PANAMA / PANAMA / BELLA VISTA` with address line `Panama`; foreign customers hide address UI and send address/location fields as `null`.
- Creation now includes ITBMS exempt and tax retention fields using `CustomerTaxRetentionCatalog`.
- Clients-list customer creation now uses the same guided create flow; edit mode keeps the existing edit form.
- Customer type cards no longer preselect Final Consumer on first open and use secondary/secondaryContainer for the selected state.
- Customer type cards now use `vanishedBackgroundColor()` as the selected background in both POS and clients-list add flows.
- Foreign customer country dropdowns now exclude Panama and default to Colombia in guided creation and POS final-customer information.
- Customer creation RUC inputs now uppercase typed letters before validation/fetching.
- Customer details now hides billing addresses for foreign customers, removes the always-active status badge/status row, and suppresses taxpayer type for foreign/final-consumer FE types.
- Common metadata compilation passed. Existing project warnings remain unrelated.
- No focused `AddCustomerViewModel` tests were added because the repo has only validator coverage for this flow and no existing ViewModel fake harness for the required services.

# Customer Details Billing Address Selector TODO

## Plan
- [x] Replace raw billing-address location code entry with province/district/corregimiento selectors.
- [x] Prefill selectors when editing an existing billing address and derive `location_code` from `PanamaLocations`.
- [x] Align the default billing-address badge with the trailing action area.
- [x] Run common metadata compile verification.
- [x] Record review notes and correction lesson.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Billing address add/edit no longer asks the user to type `location_code`.
- The sheet now uses province, district, and corregimiento dropdowns backed by `PanamaLocations`.
- New addresses default to `PANAMA / PANAMA / BELLA VISTA`; edited addresses prefill from address province/district/corregimiento, with `location_code` fallback when names are missing.
- Submit derives `location_code` via `PanamaLocations.codeFor(...)` before calling the existing create/update address ViewModel methods.
- Billing-address default badges now render in the right-side action column above edit/delete icons.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Provisioning Login TODO

## Plan
- [x] Add POS device provisioning models, provider, repository, and service.
- [x] Add Android ordered-broadcast agent reader with no-op behavior outside POS builds.
- [x] Enforce POS provisioning during email/Google login and app bootstrap refreshes.
- [x] Refresh POS device config after financial profile refreshes without publishing extra invalidations.
- [x] Apply fixed branch/billing point and restrictive POS device permissions in POS flows.
- [x] Filter POS order list requests by provisioned branch and billing point.
- [x] Prevent POS provisioning bootstrap from publishing a transient invalid state before backend config validation finishes.
- [x] Refresh expired cached access tokens before POS provisioning revalidation.
- [x] Add focused tests and run verification gates.
- [x] Pass the login JWT to the authenticated POS config endpoint.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:assemblePosDebug`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:assemblePublicDebug`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosSimulatorArm64`
- [ ] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.pos.provisioning.PosDeviceProvisioningTest --tests com.teco.ventago.core.authz.AuthzEvaluatorTest --tests com.teco.ventago.features.financialProfile.FinancialProfileServiceTest` blocked by pre-existing unrelated `AuthzNavigationTest.kt:45` unresolved `PRODUCTS`.
- [ ] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.orders.ListOrdersRequestTest --tests com.teco.ventago.features.orders.OrderServiceProvisioningTest` blocked by pre-existing unrelated `AuthzNavigationTest.kt:45` unresolved `PRODUCTS`.

## Review Notes
- POS provisioning now reads the VentaGo Pos Agent ordered broadcast on Android and keeps public/iOS behavior as no-op via DI.
- POS login validates the agent config, authenticated business, authenticated `/api/v1/devices/{deviceId}/pos-config` response, active status, and branch/billing match before persisting Firebase/tokens/cache.
- POS config fetch now sends `Authorization: Bearer <login access token>` because the backend returns `AUTH_001` without the JWT; cached-session validation also forwards the stored JWT.
- POS login now shows a support-contact message when no usable provisioning data is found, without opening an external app.
- Cached POS sessions are revalidated on startup; provisioning invalidation after a financial profile refresh forces sign-out and clears in-memory business/product/profile/customer/branch state.
- POS sale flow now uses the provisioned branch and billing point over the persisted local selection, reapplies it after reset, and disables branch/billing dropdown changes while active.
- POS invoice configuration no longer renders branch or billing point dropdown selectors; fixed provisioned values remain applied by the ViewModel.
- POS device payment permissions now decode and restrict `payment_yappy_onsite`, `payment_link`, and `payment_manual_methods`, hiding the corresponding POS payment actions and guarding ViewModel command paths.
- POS order list requests now send `branch_code` and `billing_point_code` from active provisioning; POS builds without valid provisioning fail closed by returning no orders instead of making an unscoped list request.
- POS provisioning validation no longer emits `agentConfig` with `valid=false` while the backend config request is still pending, preventing the app bootstrap observer from signing out a remembered user during cold start.
- Cached-session POS revalidation now refreshes an expired access token before calling `/pos-config`, so normal JWT expiry does not force logout when a refresh token is still valid.
- Device permissions now restrict existing JWT/authz permissions for products, customers, quotes, expenses, reports, and payment configuration without granting access by themselves.
- Focused tests were added for provisioning parsing/validation, financial-profile-triggered POS refresh, POS authz restriction gate, order request serialization, and POS order-service filtering; Android host tests cannot run until the unrelated existing `AuthzNavigationTest.kt` compile error is fixed.

# POS Barcode Scanner Mode TODO

## Plan
- [x] Trace current POS product search, scanner, cart, toast, and sound patterns.
- [x] Add scanner entry point to the product search bar.
- [x] Build POS scanner mode with half-screen camera, product pending panel, cancel/auto-add behavior, cart CTA, and close CTA.
- [x] Add successful-scan beep feedback and product-not-found feedback.
- [x] Verify common metadata compilation and update review notes.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileAndroidMain`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosSimulatorArm64`

## Review Notes
- Added a QR/barcode scanner icon to the POS product search field.
- Scanner mode requests camera permission, then shows a 50/50 camera/product-action layout.
- Successful barcode matches play a beep and show the scanned product with a cancel action; if not canceled within 3 seconds, the item is added through the existing POS cart path.
- Missing barcodes do not beep and show `Producto no encontrado`.
- Scanner mode includes the existing-style cart CTA with line count and legal/tax-aware total, plus a close scanner action.
- Common metadata, Android main, and iOS simulator Kotlin compilation passed. Existing project warnings remain unrelated.

# POS Home Dashboard Cleanup TODO

## Plan
- [x] Gate Home invoice plan card behind non-POS distribution.
- [x] Gate Home sales graph behind non-POS distribution.
- [x] Run common compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- POS builds now hide the invoice folio/plan card on Home.
- POS builds now hide the Home sales range selector and line chart.
- Public/non-POS builds keep the existing invoice plan and sales chart behavior.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Flavor H10P Printer Integration TODO

## Plan
- [x] Add Android `public` and `pos` product flavors with distinct app ids and build flags.
- [x] Add POS-only H10P printer module and SDK AIDL wiring.
- [x] Move Android printer DI to flavor-specific app modules.
- [x] Implement H10P internal printer engine behind existing `PrinterEngine`.
- [x] Update shared printer resolution so POS builds can use an internal printer without network endpoint config.
- [x] Run public/POS build verification and dependency checks.
- [x] Record review notes.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:assemblePublicDebug`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:bundlePublicRelease`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:assemblePosDebug`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:bundlePosRelease`

## Review Notes
- Added `public` and `pos` Android flavors. Generated BuildConfig confirms public uses `com.teco.ventago` with `IS_POS_BUILD=false`; POS uses `com.teco.ventago.pos` with `IS_POS_BUILD=true`.
- Added `:printer-h10p` as a POS-only Android library with SDK AIDL stubs and `H10pPrinterEngine`.
- Public Android DI now binds the existing Epson engine/discovery; POS DI binds the H10P internal printer engine and unsupported discovery.
- `PrinterService` now synthesizes an active `h10p_internal` printer only for POS builds when no backend printer exists for the selected branch/billing point.
- Printer onboarding in POS builds skips network discovery/IP entry and saves/tests the internal printer config.
- Runtime dependency checks confirmed public release includes `:composeApp` but not `:printer-h10p`; POS release includes `:printer-h10p`.
- AABs were generated at `androidApp/build/outputs/bundle/publicRelease/androidApp-public-release.aab` and `androidApp/build/outputs/bundle/posRelease/androidApp-pos-release.aab`.

# Order Detail Paid Payment Link Invoice Action TODO

## Plan
- [x] Separate never-attempted manual invoice action from retry invoice action.
- [x] Show `Facturar` for paid payment-link orders with no invoice generated.
- [x] Keep `Reintentar facturación` for invoice statuses that represent a failed/pending invoice attempt.
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Paid payment-link orders with `invoice_status` absent/none and no CUFE now show a green `Facturar` button.
- That `Facturar` action calls the existing invoice endpoint directly; it does not open the manual-payment sheet, because the order is already paid.
- `Reintentar facturación` no longer appears for `InvoiceStatus.NONE`; it remains for paid orders in pending/failed invoice states.
- Common metadata compilation passed. Existing project warnings remain unrelated.


# Payment Link Manual Invoice And ACH Detail TODO

## Plan
- [x] Trace payment-link success invoice messaging and ACH detail loading conditions.
- [x] Add financial-profile auto-invoice state to POS success decisions.
- [x] Show manual-invoice-required copy when payment-link payment is detected and auto invoice is disabled.
- [x] Stop classifying credit card automatic payments as ACH payments.
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Payment-link success now reads `auto_invoice_on_payment_success` from the financial profile.
- When a payment-link payment is detected and auto invoice is disabled, the success screen shows `Pago recibido` with manual-invoice-required copy instead of the invoice generation loader.
- Payment-link polling exits after paid detection when auto invoice is disabled, instead of waiting for an invoice status that will not be produced automatically.
- Automatic credit card payments no longer trigger ACH detail loading; ACH classification now requires ACH in the payment method name or description.
- Common metadata compilation passed. Existing project warnings remain unrelated.


# iOS Export Compliance Encryption Review TODO

## Plan
- [x] Review current iOS/KMP source for app-implemented encryption or hashing algorithms.
- [x] Verify iOS networking and secure storage implementation paths.
- [x] Check iOS project configuration and linked dependencies for explicit crypto libraries.
- [x] Add `ITSAppUsesNonExemptEncryption=false` to the iOS app `Info.plist`.

## Review Notes
- iOS HTTP traffic uses Ktor's Darwin engine, so HTTPS/TLS encryption is delegated to Apple's networking stack.
- iOS sensitive storage uses the Keychain/Security framework through `SecureStorage`.
- The only direct `HmacSHA256` implementation is in `androidMain`; the iOS actual returns an empty string.
- No proprietary, non-standard, or app-owned standard encryption implementation was found in iOS app source.
- `iosApp/iosApp/Info.plist` now declares `ITSAppUsesNonExemptEncryption` as `false` for App Store Connect export-compliance handling.

# Android 16 Target SDK Verification TODO

## Plan
- [x] Inspect Android/KMP Gradle target SDK configuration.
- [x] Verify the generated release manifest target SDK.
- [x] Build the release AAB for artifact-level validation.
- [x] Record result.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:processReleaseManifest --info`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:bundleRelease`

## Review Notes
- The app already targets API 37 through `android-targetSdk = "37"` in `gradle/libs.versions.toml`, which satisfies Google Play's Android 16/API 36+ requirement.
- The generated release bundle manifest contains `<uses-sdk android:minSdkVersion="24" android:targetSdkVersion="37" />`.
- Release bundle generation succeeded and produced `androidApp/build/outputs/bundle/release/androidApp-release.aab`.
- The Play Console warning is likely from an older uploaded artifact; upload a new release bundle built from this codebase/version.

# Yappy Onsite Payment Description TODO

- [x] Trace the Yappy onsite payload field that sends the payment description.
- [x] Replace `Factura POS` with the selected business name, falling back to `Pago Yappy`.
- [x] Apply the same fallback to Yappy onsite replacement intents.
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Yappy onsite order creation now sends `links.note` as the current business name when available, with `Pago Yappy` as fallback.
- Yappy onsite replacement pending intents from POS and Order Details pass the same business-name-first note.
- The service default for replacement Yappy onsite intents is now `Pago Yappy` instead of `POS payment`.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# Quotes List Search And Filters TODO

## Correction
- [x] Merge nested `customer` fields from quote list API responses into the `Quote` model.
- [x] Add a regression test using the backend list response shape.
- [x] Run common metadata compile.
- [ ] Run focused quote repository test; currently blocked by unrelated `AuthzNavigationTest.kt:45` unresolved `PRODUCTS`.

## Plan
- [x] Add shared quote-number display formatter and use it in quote list/details.
- [x] Update quote list subtitle to prefer customer/business name over item count.
- [x] Move quote status chips into the filter bottom sheet and expose search/filter icons in the app bar.
- [x] Add quote-number search bottom sheet that builds full backend quote numbers from prefix, branch, year, and padded sequence.
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Quote list customer names were missing because the backend list response sends nested `customer.name`, while the decoded `Quote` UI field is the flattened `customerName`.
- Quote list and detail repository mapping now merge nested `customer` fields into `customerName`, `customerEmail`, `customerPhone`, `customerRuc`, and `customerId` when the flat fields are blank.
- Added a regression test for the backend list shape with nested customer data.
- Common metadata compilation passed. Focused quote repository test did not run because common tests currently fail to compile on unrelated `AuthzNavigationTest.kt:45:30 Unresolved reference 'PRODUCTS'`.
- Quote numbers now render compactly as `2026-000031` for full values like `TEC-0000-2026-000031` in list and detail.
- Quote list rows show customer/business name under the number when present; item count remains the fallback.
- Quote status chips now live in the filter bottom sheet, and quote list top app bar exposes search and filter icons with active-filter badge.
- Quote-number search now has branch selection, year selection from 2023 through 2026 with 2026 selected by default, numeric sequence input, quote-prefix fallback `COT`, and six-digit sequence padding.
- Common metadata compilation passed. Existing project warnings remain unrelated.


# Orders Search QR Action TODO

## Correction
- [x] Translate the new QR search helper/action labels to Spanish.
- [x] Use QR scanner framing for invoice CUFE scans from Orders.
- [x] Re-run compile verification.

## Plan
- [x] Move QR scanner launch from the Orders top app bar into the order search bottom sheet.
- [x] Add a scan QR action with muted CUFE helper text in the search sheet.
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Removed the top-app-bar QR scanner icon from Orders.
- The order search bottom sheet now includes muted CUFE guidance and a full-width `Scan QR` action.
- The scanner permission/settings handling now lives in `OrdersScreen`, so both permission dialog and scanner launch continue to work from the sheet.
- Common metadata compilation passed. Existing project warnings remain unrelated.

## Correction Review Notes
- Changed the search sheet copy to `Escanea el QR de la factura para encontrarla por CUFE.` and `Escanear QR`.
- Orders CUFE scanning now calls `BarcodeScannerScreen(format = KmpBarcodeFormat.QR_CODE)`, so the scanner overlay uses the square QR frame instead of the default barcode frame.
- Common metadata compilation passed after the correction. Existing project warnings remain unrelated.

# POS Recover Invoice After First Step TODO

## Plan
- [x] Stop persisting recoverable checkpoints while the user is only on the customer/configuration step.
- [x] Save the checkpoint as the products step when the user taps "Siguiente" and enters product selection.
- [x] Ignore legacy customer-step checkpoints so they do not show a restore prompt.
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- POS now saves the order creation checkpoint as `PRODUCTS` when "Siguiente" successfully moves the user past customer/configuration.
- Customer/configuration-only saves are ignored by the ViewModel, so selecting a customer or changing invoice config does not create a recoverable invoice.
- Legacy checkpoints saved at `CUSTOMER` are cleared instead of showing the recovery dialog.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Government Customer Detection TODO

## Plan
- [x] Add POS state for the selected registered customer's `fe_customer_type` from customer details.
- [x] Populate that value from `GET /api/v1/customers/{ID}` when a registered customer is selected or restored.
- [x] Stop using RUC text to infer government customers; require `fe_customer_type == "03"`.
- [x] Clear the cached FE type when customer/final-customer state is cleared.
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- POS now stores the selected registered customer's `fe_customer_type` from `GET /api/v1/customers/{ID}`.
- Government product warnings now require `fe_customer_type == "03"` and no longer infer government status from RUC contents like `NT`.
- Customer selection, quote restore, checkpoint restore, credit/debit-note initialization, and final-customer toggles clear or hydrate the cached FE type appropriately.
- The government warning copy now says the selected customer is registered as government instead of saying the RUC looks government-like.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Success Compact Order Number TODO

## Plan
- [x] Update success order-number formatter to remove left zeroes.
- [x] Preserve current fallback behavior for blank order numbers.
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Success order numbers now strip left zeroes after any dash-delimited prefix, so `#0000000868` renders as `#868`.
- Blank order numbers still render as `#-`; all-zero values render as `#0`.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Draft Success Screen TODO

## Plan
- [x] Detect draft order success separately from invoice/payment success.
- [x] Replace draft hero copy with saved-draft messaging.
- [x] Suppress payment summary for drafts so it does not show "Pago registrado".
- [x] Keep new-order action and add a visible order-details action for drafts.
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Draft order success now shows "Orden guardada en borrador" with copy explaining the invoice and payment can be completed later.
- Draft success no longer renders the payment summary, so it does not claim "Pago registrado".
- Draft success keeps the new-order CTA and adds a visible "Ver detalle de la orden" action.
- Invoice warning/download/share actions stay disabled for draft success.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Payment Screen Default Selection TODO

## Plan
- [x] Add local PaymentScreen state so normal sales start with no selected payment option.
- [x] Keep replacement and credit/debit-note flows on existing behavior where a method is already implied.
- [x] Hide payment method content until the user selects an option.
- [x] Rename the payment-link option to "Crear enlace de pago con QR".
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Normal POS payment entry now starts with no selected payment option and no payment content rendered below the selector.
- Selecting manual, payment link, Yappy onsite, or draft stores a local selection and then renders that method's content.
- Replacement mode and credit/debit-note flows continue using the existing ViewModel payment mode so implied payment handling is unchanged.
- The payment-link selector title now reads "Crear enlace de pago con QR".
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Payment Link Success Screen TODO

## Plan
- [x] Rework the payment-link card so the QR gets full-width vertical space and share actions sit below it.
- [x] Add a small scan/status badge explaining automatic payment detection.
- [x] Require confirmation before cancelling/releasing a payment link to choose another payment method.
- [x] Change payment-link order refresh polling to wait 4 seconds after each response.
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Payment-link QR now renders full-width with a larger vertical range, and copy/WhatsApp actions sit below it.
- Added a compact scan badge explaining that the customer can scan the QR and the app will detect completed payment automatically.
- Selecting another payment method now opens a confirmation dialog before the payment link is cancelled/released.
- Payment-link status polling now waits 4 seconds between completed order refresh attempts.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# Yappy Onsite Cancel Order TODO

## Plan
- [x] Trace current QR cancel, change-payment, and order cancellation paths.
- [x] Make explicit QR cancellation cancel the order with reason `yappy qr code cancelled` and show the cancelled/failed state.
- [x] Style the cancel order button as destructive/red while preserving the change-payment action.
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- The active Yappy onsite QR destructive action now uses the Yappy onsite transaction cancel endpoint with reason `yappy qr code cancelled`.
- The change-payment action still uses the existing QR release/replacement flow and is not routed through order cancellation.
- After successful cancellation, the screen renders the cancelled order state and offers a new sale instead of returning to the cancelled order checkout.
- The cancel order button now uses `MaterialTheme.colorScheme.error` for text/icon/border.
- Common metadata compilation passed. Existing project warnings remain unrelated.

## Correction
- [x] Route destructive Yappy onsite cancellation through the Yappy transaction cancel endpoint so the provider transaction is cancelled before backend order cancellation.
- [x] Treat returned/cancelled transaction response as the cancelled-order success state.
- [x] Run compile verification after the correction.

## Correction Review Notes
- `cancelYappyOnsiteOrder()` now calls `paymentService.cancelYappyOnsiteTransaction(...)`, which maps to `PUT /api/v1/payments/yappy/onsite/transactions/:transaction_id/cancel`.
- The app accepts `cancelled`, `canceled`, or `returned` transaction responses as terminal order-cancelled success, then normalizes local display status to cancelled so the order-cancelled screen is shown.
- Common metadata compilation passed after the correction. Existing project warnings remain unrelated.

# Home Config Summary Re-entry Refresh TODO

## Plan
- [x] Confirm why bottom-bar Home re-entry calls `/business/config-summary`.
- [x] Change Home route entry to use financial profile cache/listener context without forcing refresh.
- [x] Add focused regression coverage for `setBusiness(refresh = false)` cache behavior.
- [x] Run focused tests and common compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.financialProfile.FinancialProfileServiceTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Home re-entry from bottom-bar navigation recreated/ran `HomeViewModel` entry logic, which called `financialProfileService.setBusiness(businessId, refresh = true)` and forced `/api/v1/business/config-summary`.
- Home now calls `setBusiness(businessId, refresh = false)`, so it uses the cached financial profile and the service's realtime financial listener instead of refreshing on each route entry.
- Added coverage that `setBusiness(refresh = false)` uses a matching cached profile and does not call the financial profile repository.
- Focused Android host tests and common metadata compilation passed. Existing project warnings remain unrelated.


# Config Summary And Branch Refresh Storm TODO

## Plan
- [x] Confirm the repeated backend calls from the provided GIN log sample.
- [x] Trace `config-summary` and `invoicing/branches` refresh paths to their cache/change listeners.
- [x] Stop backend refreshes from publishing their own realtime invalidations.
- [x] Add focused regression coverage for silent backend refresh caching.
- [x] Run focused tests and compile verification, then record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.branches.BranchServiceRefreshTest --tests com.teco.ventago.features.financialProfile.FinancialProfileServiceTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Provided log sample contained 34 `GET /api/v1/invoicing/branches` calls and 5 `GET /api/v1/business/config-summary` calls between `09:49:20` and `09:49:59`.
- `BranchService.refresh()` now caches backend branch refreshes with `ignoreChange = true`, so its own realtime branch listener does not immediately trigger another `/invoicing/branches` request.
- `FinancialProfileService.refresh()` now writes backend profile data through a synchronous silent cache path, and `RoomCache` no longer emits `financialChanged()` just because a `BusinessFinancialProfile` was cached.
- Added regression coverage for silent backend branch/profile refreshes while keeping branch local mutations able to publish invalidations.
- Focused Android host tests and common metadata compilation passed. Existing project warnings remain unrelated.

# Home Payment Banner Profile Resolution TODO

## Plan
- [x] Add a Home state flag for payment profile/config-summary resolution.
- [x] Gate the payment promotional banner until the backend financial profile has resolved.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Home now tracks when the payment profile/config summary has resolved.
- The payment promotional banner only evaluates after that backend-backed state is available, preventing a false post-login flash for businesses that already have payments configured.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Yappy Onsite Configuration Routing TODO

## Plan
- [x] Add explicit POS state for whether payments onboarding/account creation is completed.
- [x] Route Yappy onsite configuration to Payments home when onboarding is not completed, otherwise route directly to Yappy onsite setup.
- [x] Update the configuration dialog copy/confirm navigation to match the resolved destination.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- POS now tracks `paymentsOnboardingCompleted` from the financial profile instead of inferring initial setup from configured payment methods.
- Yappy onsite configuration routes to Payments home when payments onboarding/account creation is not completed, and routes directly to Yappy onsite setup once onboarding is completed.
- The configuration dialog copy now tells first-time payment users to accept terms/create the payments account before Yappy onsite setup.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# Home Empty Sales Chart TODO

## Plan
- [x] Hide the Home sales chart section when chart data is empty.
- [x] Keep the chart visible when any sale exists in the selected range data.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Home now skips the sales title, range selector, and line chart when the sales chart data is empty.
- The section returns automatically when `salesChart` contains non-zero sales data.

# Bottom Bar App Menu TODO

## Plan
- [x] Add a neutral app menu route and menu screen using the Home quick-card visual style.
- [x] Retarget the current products bottom-bar entry so it opens the menu instead of categories.
- [x] Wire menu cards to existing destinations for products/services, clients, quotes, sales, add product, expenses, branches, and payment methods.
- [x] Keep menu cards permission-aware so restricted routes are not exposed.
- [x] Run compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- The bottom bar now shows `Menú` with a menu icon instead of opening products/categories directly.
- The new menu screen renders two-column Home-style white cards in the requested order.
- Menu cards route to categories, clients, quotes, sales/orders, direct add product, expenses, branches, and payment methods using existing graph destinations.
- Cards are hidden when the current user lacks the matching route permission; `AddItemScreen` is now guarded by `RouteKey.PRODUCT_ADD`.
- Common metadata compilation passed. Existing project warnings remain unrelated.

## Correction Plan
- [x] Remove the menu title from the menu screen.
- [x] Reorder menu cards into paired view/action rows.
- [x] Style left-column cards as white with primary text/icons.
- [x] Style action cards like the Home new-order card and payment methods as green filled.
- [x] Keep quotes and new quote beta/permission gated.
- [x] Run compile verification and record results.

## Correction Review Notes
- Removed the visible menu title so the screen starts directly with cards.
- Reordered the cards into `[view][create/action]` rows: products/add product, clients/add client, orders/new order, quotes/new quote, expenses/new expense, branches/payment methods.
- Left-column cards use the white card container with primary content; action cards use the Home new-order secondary background with `onSecondary`; payment methods uses a green filled card.
- Quote cards remain gated by `RouteKey.QUOTES_LIST` and `RouteKey.QUOTE_NEW`, preserving the beta-feature check.
- Common metadata compilation passed. Existing project warnings remain unrelated.

## Color Correction
- [x] Replace secondary action-card background with `vanishedBackgroundColor()`.
- [x] Keep readable icon/text contrast after the background change.
- [x] Run compile verification and record results.

## Color Correction Review Notes
- Menu action cards now use `vanishedBackgroundColor()` instead of `MaterialTheme.colorScheme.secondary`.
- Because the new background is light, action-card icon/text color now uses primary for contrast.
- Payment methods remains green filled.
- Common metadata compilation passed. Existing project warnings remain unrelated.

## Layout Revert
- [x] Return the menu to the four-row layout: products/clients, quotes/sales, add product/expenses, branches/payment methods.
- [x] Remove the extra create cards for client, order, quote, and expense.
- [x] Run compile verification and record results.

## Layout Revert Review Notes
- Menu cards are back to the original white-card grid order requested for the menu.
- Quotes remain beta-gated by `RouteKey.QUOTES_LIST`.
- Common metadata compilation passed. Existing project warnings remain unrelated.

## Primary Color Correction
- [x] Set every menu card icon and label to primary.
- [x] Run compile verification and record results.

## Primary Color Correction Review Notes
- All menu cards now use `MaterialTheme.colorScheme.primary` for icons and labels.
- Common metadata compilation passed. Existing project warnings remain unrelated.

## Quick Actions Redesign Plan
- [x] Inspect Home quick-card style, quote beta gating, and create routes.
- [x] Split the menu into quick actions and module sections.
- [x] Add the quote quick action with add-customer fallback when quote beta access is unavailable.
- [x] Keep the module grid white with primary text and icons.
- [x] Include create-only permissions in bottom menu visibility.
- [x] Run compile verification and record results.

## Quick Actions Redesign Review Notes
- Menu now renders an `Acciones rápidas` section with Nueva venta, Nueva cotización or Agregar cliente fallback, Registrar gasto, and Agregar producto.
- Quote quick action stays beta/permission-gated through `RouteKey.QUOTE_NEW`; when unavailable, the second quick action uses `RouteKey.CUSTOMER_FORM`.
- Quick actions use secondary-colored text/icons and a selected treatment for Nueva venta.
- The `Módulos` grid uses white cards with primary-colored icons and labels.
- Bottom menu visibility now considers create-only route permissions so users with direct actions can still open the menu.
- Common metadata compilation passed. Existing project warnings remain unrelated.

## Quick Actions Compact Correction
- [x] Remove subtitles from quick action cards.
- [x] Remove selected-state styling from Nueva venta.
- [x] Reduce quick action icon size.
- [x] Run compile verification and record results.

## Quick Actions Compact Correction Review Notes
- Quick actions now render title-only cards with secondary-colored 24dp icons.
- All quick action cards use the same white card container; Nueva venta no longer has selected background or border.
- Common metadata compilation passed. Existing project warnings remain unrelated.

## Quick Action Icon Correction
- [x] Use the same POS drawable icon as Home for Nueva venta.
- [x] Change Agregar producto to an inventory/box icon.
- [x] Run compile verification and record results.

## Quick Action Icon Correction Review Notes
- Nueva venta now renders `Res.drawable.pos` with the same tinting approach used by Home.
- Agregar producto now uses the inventory/box Material icon instead of the cart icon.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# Home Payment Setup Banner TODO

## Plan
- [x] Add a Home state flag derived from the financial profile for whether payment methods are configured.
- [x] Render a dismissible payment setup banner below the Home action cards and above support.
- [x] Persist the dismissal locally per business.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Added a Home payment setup banner below the action cards and above support.
- The banner uses the real-time reports dark gradient style, includes the Yappy logo, opens payment settings from the CTA, and persists dismissal in `LocalStorage` per business.
- Banner visibility is driven by the observed financial profile and hides once the business has configured payment methods.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# Yappy QR Exit Cancellation Guard TODO

## Plan
- [x] Trace Yappy onsite QR cancel and back-navigation paths.
- [x] Add shared POS state for active QR exit confirmation so app-bar back can trigger the screen dialog.
- [x] Show loading feedback while cancelling a Yappy onsite charge.
- [x] Route hardware back and top-app-bar back through the same cancel-before-exit flow.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Yappy onsite cancellation now calls `showLoading()` before the cancel endpoint and returns success/error feedback through the existing `LoadingSheet`.
- Active QR exits are now represented in shared POS state so both the QR screen and the app shell can request the same confirmation dialog.
- Hardware back and top-app-bar back on an active Yappy QR now prompt to cancel the QR before leaving; confirming cancels the transaction, hides loading, then returns to checkout.
- The QR exit dialog is cleared on reset/new QR/replacement paths to avoid stale dialogs on later sales.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Manual Replacement False Invoice Warning TODO

## Plan
- [x] Trace the Yappy onsite -> payment link -> manual replacement flow from UI state to backend contracts.
- [x] Make manual-payment registration resolve issued invoice status from the registration payload and refreshed order.
- [x] Clear stale post-create invoice warning state after a successful manual replacement completion.
- [x] Add focused regression coverage for nested manual-registration invoice/ticket payloads.
- [x] Run focused tests/compile and record verification results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.orders.RegisterManualPaymentsRequestTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Manual replacement now resolves invoice status from the manual registration response plus refreshed order evidence, including nested `invoice.status`, `invoice.cufe`, and fresh-order CUFE.
- Successful manual replacement clears stale `postCreateInvoiceWarning`, so the POS success screen does not keep an old "factura manual" warning after the invoice is already issued.
- Added regression coverage for stale refreshed-order status with issued manual-registration payload.
- Focused manual-registration Android host tests and common metadata compilation passed. Existing project warnings remain unrelated.

# POS Payment Change Ticket Print Cancellation TODO

## Plan
- [x] Confirm which automatic ticket print paths still run from cancellable UI/polling coroutines.
- [x] Route payment-link issued-ticket printing through the existing `AppScope` non-cancellable print queue.
- [x] Route Yappy onsite issued-ticket printing through the same protected print queue.
- [x] Run focused compile/tests and record verification results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.printers.PrinterRepositoryTest --tests com.teco.ventago.features.orders.RegisterManualPaymentsRequestTest`

## Review Notes
- Payment-link issued-invoice ticket printing now uses the same `AppScope` + `NonCancellable` queue as manual replacement instead of running inside the polling/UI coroutine.
- Manual replacement queues the ticket before PDF loading, so a cancelled UI-only PDF fetch cannot prevent ticket printing.
- Manual replacement passes the issued-status decision derived from the registration response/refreshed order through the print queue, avoiding false skips when the refreshed order has a missing invoice status.
- Yappy onsite ticket printing now enqueues its provided ticket payload on the protected background queue instead of printing inline from polling.
- Common metadata compilation and focused Android host tests passed. Existing project warnings remain unrelated.


# POS Manual Replacement Ticket Print TODO

## Plan
- [x] Confirm why ticket printing fails after payment-link/manual replacement success.
- [x] Move replacement ticket printing out of the UI-bound confirmation coroutine.
- [x] Keep printer repository cancellation from being logged as a normal printer error.
- [x] Run focused compile/tests and record verification results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.printers.PrinterRepositoryTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Manual replacement success now captures the selected branch/billing point before closing the replacement UI and enqueues issued-invoice ticket printing on `AppScope`, so the print fetch/command is not cancelled by the success screen transition.
- Queued manual-replacement ticket printing now runs inside `NonCancellable`, because the previous `AppScope` launch could still receive cancellation from suspend boundaries during the post-payment transition. Ticket failures now include the failing stage (`ticket_payload` or `print_ticket`) and throwable type in the log.
- Manual registration ticket resolution now accepts `ticket` from either the top-level response field or nested `invoice.ticket`, avoiding the fallback docs fetch when the backend includes the ticket under the invoice object.
- The fallback `/orders/{id}/invoices/docs/ticket` fetch now retries three times on `CancellationException` before failing the print attempt, with per-attempt cancellation logs.
- Android Epson printer retries now rethrow `CancellationException` instead of treating it as a normal printer attempt failure.
- `PosViewModel` DI now explicitly passes the named `AppScope` instead of relying on `viewModelOf` constructor resolution.
- Printer repository calls rethrow `CancellationException` without logging it as a printer repository error.
- Added focused repository and manual-registration DTO regression tests. Targeted Android host tests and common metadata compilation passed.

# POS Pending Payment Method Change UI TODO

## Plan
- [x] Refactor POS payment UI so the payment selector/allocation surface can run in normal create mode and post-order replacement mode.
- [x] Replace separate payment-link switch buttons with one “Seleccionar otro método de pago” action that opens replacement mode and hides the source method plus draft option.
- [x] Implement release-first replacement actions for manual, payment link, and Yappy onsite, including manual success PDF loading and ticket printing.
- [x] Guard exits during replacement selection with a cancel-order confirmation dialog.
- [x] Extend manual payment DTO parsing/serialization for due dates and optional invoice/order/ticket response fields.
- [x] Add focused tests and run verification commands.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Payment-link success now shows one “Seleccionar otro método de pago” entry while the link is pending; opening it renders the shared payment screen content with payment link and draft hidden.
- Active Yappy QR screens also expose the shared replacement selector with Yappy hidden, plus loading feedback for replacement/cancel mutations.
- Replacement actions release the current pending intent only after the cashier confirms the new method, then create/register the chosen replacement.
- Manual replacement registration now supports credit `due_date`, refreshes the order, loads the invoice PDF for the existing success actions, and prints the ticket once when a printer is configured.
- Leaving an open replacement selector through back/home/new-sale prompts that the order will be cancelled and calls the existing order cancellation service on confirmation.
- Added DTO and replacement option policy tests. Android host tests and common metadata compilation passed; existing project warnings remain unrelated.

# Payment Success Order Deeplink TODO

## Plan
- [x] Route in-app notification order actions through `OrdersScreenRoute(orderNumber)`.
- [x] Add external deep link support for `https://ventago.tecodigi.com/orders/order-details.html?orderNumber=...`.
- [x] Add Android and iOS app/universal link host declarations for `ventago.tecodigi.com`.
- [x] Add focused regression coverage for the payment-success order-details URL.
- [x] Run focused resolver tests and common compile verification.

## Verification Gates
- [x] Attempted `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.notifications.NotificationActionResolverTest` but this migrated KMP module no longer has `testDebugUnitTest`.
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.notifications.NotificationActionResolverTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- In-app notification taps that resolve to order details now propagate `OrdersScreenRoute(orderNumber)` from the notifications route into the orders graph.
- `OrdersScreenRoute` now accepts `https://ventago.tecodigi.com/orders/order-details.html?orderNumber=...` as an external deep link while preserving the existing `tecodigi.com/orders` patterns.
- Android App Links and iOS associated domains now include `ventago.tecodigi.com`; backend-hosted assetlinks/AASA files are still required for verified OS delivery.
- Added regression coverage for the sample payment-success order number URL.
- Focused Android host notification resolver tests and common metadata compilation passed. Existing project warnings remain unrelated.

# Order Pending Payment Method Change TODO

## Plan
- [x] Audit pre-create POS payment selection against MANUAL, LINK, and YAPPY_ONSITE contracts.
- [x] Audit post-create order detail/confirmation replacement flows for active Yappy QR and payment link release behavior.
- [x] Align request/response DTOs, provider/repository/service calls, and ViewModel state transitions with the documented API wrapper contracts.
- [x] Keep UI changes minimal and consistent with existing order detail/POS payment patterns.
- [x] Add or update focused tests where contracts are parsed or request payloads are built.
- [x] Run focused compile/tests and record verification results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.orders.CxcOrderDetailsContractTest --tests com.teco.ventago.features.orders.ui.order_details.viewmodel.OrderMutationErrorMapperTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- POS payment-link success now shows a `Cambiar método de pago` card while the link is still pending, with `Cobro manual` and conditional `Yappy en caja` actions.
- Switching payment link to manual releases the active hosted pending intent with `customer_selected_cash`, hides the link, opens a manual registration panel, and submits payments through `POST /orders/{id}/payments/manual`.
- Switching payment link to Yappy releases the active hosted pending intent with `customer_selected_yappy_onsite`, creates a replacement Yappy onsite pending intent, and navigates to the Yappy QR confirmation screen.
- Pending-intent release parsing now supports `next_actions`, replacement link parsing accepts nested snake/camel URL shapes, and `manual_refund_required` maps to a blocking reconciliation message.
- Focused Android host tests and common metadata compilation passed. Existing project warnings remain unrelated.

# Credit Notes Fixes TODO

## Plan
- [x] Remove referenced credit/debit note and generic debit note types from the normal POS invoice-type dropdown.
- [x] Preserve order-details preset creation for referenced credit/debit notes.
- [x] Add generic credit-note original invoice fields, inline validation, and request references serialization.
- [x] Add focused regression tests for dropdown mapping, validation, and request reference serialization.
- [x] Run focused tests and common compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.pos.ui.viewmodel.PosNoteValidatorsTest --tests com.teco.ventago.features.orders.CreateOrderRequestTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Normal POS invoice-type dropdown now excludes `04`, `05`, and `07`; `06 - Nota de Crédito Genérica` remains selectable.
- Order-details referenced note creation still presets `04`/`05`, disables the selector, and displays the preset note label even though those values are hidden from the normal dropdown.
- Generic credit notes now collect original invoice number/date on the first step, validate required/max-length fields inline, and send a `paper` reference with midnight issue datetime.
- POS payment actions now treat `06` as a note for CTA copy and draft/payment-link gating.
- Focused tests and common metadata compilation passed. Existing project warnings remain unrelated.

# POS Payment Link Success Polling TODO

## Plan
- [x] Add POS payment-link polling state and reset handling.
- [x] Poll created payment-link orders from the success screen until payment/invoice terminal state.
- [x] Hide link QR/share/copy after payment, show invoice-generation state, then show invoice actions.
- [x] Print the ticket once after payment-link invoice generation when an active printer is configured.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Added `createdOrderId`, payment-link polling/payment-detected state, and one-shot payment-link invoice print tracking to POS state.
- `SuccessScreen` starts payment-link polling while mounted and stops it on dispose; unpaid links keep QR/copy/share, paid links show invoice generation, and issued invoices show invoice actions.
- Payment-link polling refreshes the created order, detects paid status, fetches invoice PDF after issued status, stops on issued/failed, and attempts ticket printing once when an active printer is configured.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Yappy Onsite QR Conditional Padding TODO

## Plan
- [x] Restore the normal 72dp top content padding for non-QR Yappy onsite states.
- [x] Keep the active QR view at zero top content padding and preserve 8dp QR inner padding on all sides.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Added `isPendingQrView` so only the active QR display uses `top = 0.dp`; success, invoice-processing, failed, expired, cancelled, returned, and missing-QR states keep the standard `72.dp` top padding.
- QR image keeps `8.dp` padding on all sides inside the white QR card.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Yappy Onsite QR Size Polish TODO

## Plan
- [x] Remove the extra top content padding from the Yappy onsite QR screen.
- [x] Make the QR panel fill the available card width and reduce generated QR quiet-zone margin.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Removed the 72dp top inset from the Yappy onsite QR content column.
- QR generation now uses a larger 1200px bitmap with zero quiet-zone margin, and the QR card fills the available success card width with only 8dp inner padding.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Yappy Availability Warmup TODO

## Plan
- [x] Cache Yappy onsite device availability per business.
- [x] Warm payment/Yappy availability at POS order start instead of waiting for payment screen entry.
- [x] Keep payment-screen refresh as a fallback without causing late option churn when cache exists.
- [x] Run common compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- POS now warms Yappy onsite availability at order entry and after `resetForNewSale()`, so the payment screen can use already-resolved availability instead of doing the first device check there.
- Yappy onsite devices are cached per business in `LocalStorage` and loaded immediately before the background refresh.
- Background device refresh saves the cache on success and keeps cached devices visible while refreshing, avoiding a loading-state removal/reinsert of the Yappy option.
- The payment screen still calls the refresh path as a fallback, but it no longer clears cached availability while checking.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# Yappy Portrait Payment Options TODO

## Plan
- [x] Use the provided portrait Yappy logo asset for POS payment options.
- [x] Keep payment-method configuration Yappy options on their existing horizontal logo.
- [x] Run common compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- The provided `/Users/oscar/Downloads/logo-yappy-color/yappy-color-portrait.png` matches the existing `composeResources/drawable/yappy_logo_portrait.png` asset by SHA-256, so no duplicate binary asset was added.
- POS `Yappy en caja` payment option now uses `yappy_logo_portrait` instead of the horizontal `yappy_logo`.
- Rolled back the payment-method configuration options for `Yappy` and `Yappy en caja` to their existing horizontal `yappy_logo`.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Yappy Onsite QR Success UI TODO

## Plan
- [x] Restyle `YappyOnsitePaymentScreen` to match POS success screen background, cards, button widths, and final actions.
- [x] Rebuild the QR pending state around the Yappy brand style, larger QR, ticking timer, Spanish labels, and `$XX.XX` amount format.
- [x] Show a clear paid/invoice-processing state with green check and loader copy while the invoice is being generated.
- [x] Reuse generated invoice PDF handling for download/share actions and expose order detail navigation.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Yappy onsite QR page now uses the POS success background/card language, the Yappy logo, a larger rounded QR panel, `$XX.XX` amount formatting, Spanish status labels, and a ticking timer pill.
- Payment success before invoice completion now shows a green animated check, loader, and `Pago recibido` / invoice-generation copy.
- Final invoice success now mirrors the POS success actions: download PDF, share invoice, make another order, and open order details.
- Added a narrow `shareYappyOnsiteInvoicePdf()` helper that reuses the existing invoice PDF fetch/cache path.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Yappy Logo Icon TODO

## Plan
- [x] Reuse the existing Yappy brand asset in the POS payment selector.
- [x] Keep generic vector icon rendering for the other payment methods.
- [x] Run common compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- `Yappy en caja` now renders the existing `yappy_logo` Compose resource as an untinted painter in the payment method icon slot.
- `PaymentMethodOptionCard` still supports regular Material vector icons for the other payment options.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Yappy Selector Stability TODO

## Plan
- [x] Add explicit Yappy onsite availability resolution state to POS UI state.
- [x] Render a stable Yappy onsite selector row while configured-device availability is loading.
- [x] Keep final visibility rules unchanged once availability is resolved.
- [x] Run common compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Payment screen now distinguishes payment profile resolution, Yappy onsite configuration, and selected point device-availability resolution.
- When the user can use or configure Yappy onsite, the selector renders a stable disabled row with "Verificando disponibilidad para este punto" while the profile or units load instead of inserting the option milliseconds later.
- Once availability resolves, the existing business rules remain: enabled only for selected branch/billing point with Yappy onsite enabled; hidden if configured but unavailable for the selected point; configuration CTA still appears only when the user can configure Yappy onsite.
- Common metadata compilation passed. Existing KMP cinterop warning remains unrelated.

# POS Restore False Positive TODO

## Plan
- [x] Confirm why entering POS without edits can create a meaningful checkpoint.
- [x] Exclude default invoice-setting state from meaningful user-data detection.
- [x] Add focused regression coverage for default bottom-note configuration.
- [x] Run focused tests and common compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.pos.OrderCreationCheckpointTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Root cause: `includeBottomNote` is seeded from business invoicing settings and was counted as meaningful checkpoint data whenever non-null, even if the user had not edited the POS order.
- `OrderCreationCheckpointData.hasMeaningfulUserData()` no longer treats bottom-note inclusion alone as a user modification.
- Added regression coverage proving `includeBottomNote = true` and `includeBottomNote = false` are not meaningful by themselves.
- Focused checkpoint tests and common metadata compilation passed. Existing KMP cinterop/deprecation warnings remain unrelated.

# POS Payment Channel Visibility TODO

## Plan
- [x] Split POS payment configuration state into payment-link readiness and Yappy onsite readiness.
- [x] Update PaymentScreen selector behavior for configured, unconfigured, and hidden channels.
- [x] Preserve permission guards for invoice payment links, Yappy onsite QR, and payments configuration/view access.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- POS now stores payment-link readiness separately from overall payment-method readiness, so Yappy onsite or manual transfer no longer make the link option look ready.
- Payment link remains visible for users who can create payment links; if no compatible link channel is configured, the card shows a `Configurar links de pago` CTA before use.
- Configured Yappy en caja only appears when the selected branch/billing point has an onsite unit and the user can generate Yappy onsite QRs.
- Unconfigured Yappy en caja appears only for owners or sub-users with `invoice:yappy_onsite`, `payments:configure`, and `payments:view`, and its CTA opens the Yappy en caja configuration route.
- Common metadata compilation passed. Existing KMP cinterop/deprecation warnings remain unrelated.

# Local Unsaved POS Order Checkpoint TODO

## Plan
- [x] Add serializable POS order checkpoint models and meaningful-data detection.
- [x] Persist/restore normal sale checkpoints through `PosViewModel`.
- [x] Wire the restore dialog into the POS entry screen and payment-settings redirect.
- [x] Clear checkpoints on successful order/draft/quote completion.
- [x] Add focused checkpoint tests and run verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest`
- [x] Attempted `./gradlew --no-build-cache --no-configuration-cache :composeApp:allTests`

## Review Notes
- Added business-scoped `pos.order_creation_checkpoint:<businessId>` persistence for normal POS sale creation, with restore prompt, meaningful-data filtering, product snapshots, and payment reset-on-restore.
- Checkpoint writes are skipped for quote/edit, quote-to-order, credit/debit note, and disabled flow contexts, and are cleared after successful order/draft/quote completion or explicit start over.
- Added focused checkpoint serialization, meaningful-data, and cart-line restore tests.
- Common metadata compilation and Android host tests passed. `allTests` reached iOS simulator test linking but failed because the local Xcode link step could not find the `FirebaseCore` framework; this is an environment/linkage blocker outside the checkpoint code.

# Payments Fees Tab Chip Polish TODO

## Plan
- [x] Replace nested text-button tab chips with balanced clipped text chips.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Replaced the tab selector implementation with balanced clipped text chips so selected and unselected labels align cleanly.
- Common metadata compilation passed. Existing KMP cinterop/deprecation warnings remain unrelated.

# Payments Fees Filters TODO

## Plan
- [x] Limit transaction status filters to all, pending, and paid.
- [x] Limit transaction method filters to all, Yappy, ACH, and card.
- [x] Limit batch status filters to all, issued, due, and paid.
- [x] Render batch period labels and simplified batch card copy/actions.
- [x] Ensure batch detail action resets transaction status/method filters and filters by `batch_id`.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Transaction filters now expose `Todos`, `Pendiente`, `Pagado`, and method filters expose `Todos`, `Yappy`, `ACH`, `Tarjeta`.
- Batch filters now expose `Todos`, `Emitido`, `Por pagar`, `Pagado`.
- Batch cards now show a month/year period label, `Total fees`, no line-count row, and a `Ver detalles` action.
- `Ver detalles` opens transactions filtered by `batch_id` with all transaction status/method filters cleared and page size 10; `Ver todo de nuevo` clears the batch/status/method filters.
- Common metadata compilation passed. Existing KMP cinterop/deprecation warnings remain unrelated.

# Separate Comisiones Fees Screen TODO

## Plan
- [x] Split general payment configuration from fee summary/detail UI.
- [x] Add a dedicated `PaymentsFeesScreen` route sharing the payments graph ViewModel.
- [x] Move fee summary, transaction list, and batch list into the new fees screen.
- [x] Preserve page-based load-more behavior for fee transactions and batches.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Added `PaymentsFeesScreen` under the payments graph, sharing the existing `PaymentMethodsViewModel`.
- Payment methods home now renders general configuration, channel cards, and a compact entry into `Comisiones fees`; it no longer renders fee transactions or batches inline.
- The new fees screen renders the fee summary only when any summary amount is non-zero, then shows the existing transactions/batches tabs, filters, refresh, pay, and load-more behavior.
- Common metadata compilation passed. Existing KMP cinterop/deprecation warnings remain unrelated.

# PayPal Deeplink Return Crash TODO

## Plan
- [x] Trace the PayPal callback URI from platform entry point to Compose navigation.
- [x] Add a supported PayPal deeplink route and guard external URI navigation against unmatched destinations.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Android delivers `tecodigi://paypal` into `ExternalUriHandler`, and the prior listener passed it directly to `navController.navigate(NavUri(uri))`, which throws when no destination matches the URI.
- `PaymentsPaypalScreen` now declares `tecodigi://paypal` as a Navigation deep link.
- External URI handling now routes PayPal callbacks directly to `PaymentsPaypalScreen` and wraps unknown URI navigation with `runCatching`, preventing unmatched external links from crashing composition.
- Common metadata compilation passed; existing KMP cinterop/deprecation warnings remain unrelated.

# ACH Onboarding Disclosure TODO

## Plan
- [x] Add the requested ACH `Validación no automática` badge to the first onboarding step.
- [x] Add TecoDigi terms text with link to every channel onboarding first-step footer.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- ACH Step 1 now shows a secondary-colored `Validación no automática` badge explaining that VentaGo reviews uploaded receipts and risk signals but does not directly query the customer's bank.
- ACH intro copy now says `señales de riesgo` instead of `validaciones automáticas` to avoid contradictory language.
- Yappy, Yappy en caja, ACH, PayPal, TiloPay, and the generic onboarding footer now render the TecoDigi payment terms text on Step 1.
- Common metadata compilation passed; existing KMP cinterop/deprecation warnings remain unrelated.


# Yappy En Caja Last Group Delete Navigation TODO

## Plan
- [x] Trace Yappy onsite group delete success flow and method-route exit callback.
- [x] Add a payment-methods-home navigation event for last-group deletion.
- [x] Reset payment screen state when the final Yappy onsite group is removed.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Added `NavigateToPaymentMethodsHome` and handled it in `PaymentMethodsScreen`.
- After successful deletion of the last Yappy en caja group, the ViewModel resets payment method state and emits navigation back to payment methods.
- If other groups remain, the existing configured-page refresh behavior is preserved.
- Common metadata compilation passed; existing KMP cinterop/deprecation warnings remain unrelated.


# Yappy En Caja Config Sheet Navigation TODO

## Plan
- [x] Trace why configured edit/add unit opens the onboarding Step 3 form.
- [x] Add explicit configured-mode sheet visibility state for group/unit forms.
- [x] Keep configured add/edit group and unit flows on the configuration page.
- [x] Update unit add sheet to include group selection before billing point.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Added explicit `showYappyOnsiteGroupSheet` and `showYappyOnsiteDeviceSheet` state so configured add/edit forms no longer depend on onboarding step state.
- Configured add/edit group and unidad de cobro now open bottom sheets and stay on the Yappy en caja configuration page.
- Add unidad de cobro sheet includes group selection, billing point selection, and Device ID; edit keeps the existing group and allows Device ID plus billing point.
- Unit save now derives the backend name from the selected billing point, so hidden form fields no longer block sheet saves.
- Common metadata compilation passed; existing KMP cinterop/deprecation warnings remain unrelated.


# Payments Permissions TODO

## Plan
- [x] Add payments and Yappy onsite authz scope/action keys.
- [x] Update Payments route/action policies and focused authz tests.
- [x] Publish payments permission flags from PaymentMethodsViewModel and guard mutations.
- [x] Hide configure/pay controls in PaymentMethodsScreen by permission.
- [x] Gate POS Yappy en caja QR generation and payment-method configuration navigation.
- [x] Run focused verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Added `payments:configure`, `payments:view`, `payments:pay`, and `invoice:yappy_onsite` scope/action coverage, including focused authorization tests.
- Payments settings now require owner access or one payments scope; view-only users can inspect configured channels/fees without configure controls, and commission payment is limited to `payments:pay`.
- POS Yappy en caja QR generation now requires `invoice:yappy_onsite`; missing payment-channel setup only navigates to Payments settings when the user can configure payments.
- Verification passed. Android host tests and common metadata compilation completed successfully; existing KMP cinterop/SDK/deprecation warnings remain unrelated.

# Yappy En Caja Configuration View TODO

## Plan
- [x] Group configured unidades de cobro under their Yappy group cards.
- [x] Replace device wording with `unidades de cobro` and use branch/billing-point names instead of raw codes.
- [x] Reduce horizontal content padding for Yappy en caja detail/configuration content.
- [x] Move configured group/device edits into bottom sheets.
- [x] Hide add actions when no branch or billing-point capacity remains.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Configured Yappy en caja now renders one card per group, with its unidades de cobro listed inside the corresponding group card.
- Configured group/device edits now open bottom sheets. Group edit supports group ID, branch, API key, and secret key; unit edit supports Device ID and billing point.
- Add group/unit actions are hidden when all branches or billing points are already used.
- Branch and billing point dropdown/display labels now use names such as `Casa Matriz - Punto 1` instead of raw code pairs.
- Replaced user-facing Yappy onsite `dispositivo(s)` copy with `unidad(es) de cobro`.
- Common metadata compilation passed; existing KMP cinterop/deprecation warnings remain unrelated.


# Yappy En Caja Finalize Navigation TODO

## Plan
- [x] Trace the Yappy en caja success-step primary button and method-route exit callback.
- [x] Route Step 4 `Finalizar` through the method-route exit callback instead of the generic stepper.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- `Finalizar` on Yappy en caja Step 4 now calls the method-route exit callback, which resets the payment home route and navigates back to `PaymentMethodsScreen`.
- This avoids clearing `activeMethod` while remaining on the method-detail route, which produced the blank white screen under the `Yappy en caja` app bar.
- Common metadata compilation passed; existing KMP cinterop/deprecation warnings remain unrelated.

# Yappy En Caja Onboarding Resume Polish TODO

## Plan
- [x] Remove the duplicated Step 2 title/subtitle from the group card.
- [x] Add a tutorial text action next to the Yappy Comercial action.
- [x] Show saved groups on Step 2 with delete access for users returning to edit registration.
- [x] Resume interrupted onboarding directly at device registration when saved groups exist and no devices are registered.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes
- Removed the duplicate Step 2 title/subtitle so the group card starts at `Grupos de Yappy`.
- Added a secondary text action for `Ver tutorial` next to `Abrir Yappy Comercial`.
- Step 2 now lists saved groups with delete access, and saved groups suppress the automatic blank group draft.
- Returning to onboarding with saved groups and no devices now resumes at Step 3 with the first available device card seeded.
- Metadata compilation completed successfully. `compileKotlinMetadata` was skipped as up-to-date, then `compileCommonMainKotlinMetadata` compiled the edited common source successfully; existing KMP cinterop/deprecation warnings remain unrelated.

# Yappy En Caja Onboarding Badge Polish TODO

## Plan
- [x] Convert first-step payment confirmation text into a shield badge.
- [x] Use secondary color for the terms link, Ventago fee title, and fee question mark icon.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes
- First-step payment confirmation now renders as a compact secondary-colored shield badge.
- Terms link, Ventago fee title, and fee question mark icon now use the secondary color.
- Metadata compilation completed successfully and was skipped as up-to-date; existing KMP cinterop commonization warning remains unrelated.

# Yappy En Caja Onboarding Refresh TODO

## Plan
- [x] Add multi-card draft state for Yappy onsite groups and devices.
- [x] Add ViewModel handlers for draft add/remove/collapse/update and batch save.
- [x] Refresh Yappy onsite onboarding UI copy, fee dialog, terms link, and success page.
- [x] Add focused tests for limits, duplicate prevention, derived names, and one-by-one saves.
- [x] Run verification gates and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes
- Reworked Yappy en caja onboarding into the requested four-step flow with new copy, fee explanation dialog, commercial dashboard link, multi-card groups/devices, and a success confirmation page.
- Added draft state plus ViewModel handlers for collapsible group/device cards, duplicate prevention, branch/billing-point limits, derived backend names, and batch saves that call the existing endpoints one item at a time.
- Added focused policy tests for group/device capacity, duplicate prevention, and branch/billing-point derived names.
- Verification passed. `compileKotlinMetadata` completed successfully with the task skipped as up-to-date; existing KMP cinterop commonization warning remains unrelated.

# Payment Method Detail Polish TODO

## Plan

- [x] Show masked TiloPay credential placeholders for configured accounts without submitting mask text.
- [x] Hide the internal PayPal connect button during onboarding so only the footer connect button remains.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes

- Configured TiloPay credential fields now display `*********` while the local form value is blank, without saving/submitting the mask as credential text.
- PayPal onboarding step 4 now relies on the footer `Conectar PayPal` button only; the internal card connect button remains available outside onboarding.
- Metadata compilation completed successfully and was skipped as up-to-date; existing KMP cinterop commonization warning remains unrelated.

# Channel Configured Fee Badge TODO

## Plan

- [x] Replace separate configured and fee badges on channel rows with one secondary badge.
- [x] Show only the VentaGo fee on the channel badge.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes

- Channel rows now render one secondary-colored badge for configured methods in the format `Configurado: <fee>`.
- TiloPay's channel badge now shows only the VentaGo fee: `Configurado: 0.50%`.
- Metadata compilation completed successfully and was skipped as up-to-date; existing KMP cinterop commonization warning remains unrelated.

# TiloPay Configured State TODO

## Plan

- [x] Fix TiloPay configured detection after credential save so the method opens as configured instead of onboarding again.
- [x] Restore configured/fee badges on payment method rows.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes

- TiloPay credential save now updates the in-memory payment summary immediately with a TiloPay card provider, so returning to the payment page sees the method as configured without waiting on a later profile emission.
- TiloPay configured detection now uses credential configuration state (`configured && enabled`) from either the summary/provider or the latest TiloPay status, instead of requiring full `readyForPayments()` platform/business flags.
- Payment channel rows now show `Configurado` and a fee badge again when `methodConfigured(method)` is true; TiloPay's fee badge includes `3.75% + $0.50 + 0.50%`.
- Metadata compilation completed successfully and was skipped as up-to-date; existing KMP cinterop commonization warning remains unrelated.

# TiloPay Fee Row Match TODO

## Plan

- [x] Update the TiloPay fee row to match the provided two-column image.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes

- TiloPay fee rows now render as two side-by-side columns with a centered plus: `TILOPAY / 3.75% + $0.50` and `VENTAGO / 0.50%`, each with its own transaction subtitle.
- Metadata compilation completed successfully and was skipped as up-to-date; existing KMP cinterop commonization warning remains unrelated.

# TiloPay Onboarding Polish TODO

## Plan

- [x] Change the TiloPay account request CTA to an outlined primary button.
- [x] Compact the card fee layout into a single-line TiloPay + VentaGo fee presentation.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes

- The TiloPay account request CTA now uses `OutlinedButtonM` with `MaterialTheme.colorScheme.primary`.
- Each brand fee section now shows `TILOPAY` and `VENTAGO` side by side, followed by one combined fee line: `3.75% + $0.50 + 0.50%`.
- Metadata compilation completed successfully and was skipped as up-to-date; existing KMP cinterop commonization warning remains unrelated.

# TiloPay Cards Onboarding Refresh TODO

## Plan

- [x] Split TiloPay card onboarding into four guide/configuration steps plus success.
- [x] Replace TiloPay step copy and fee presentation with the requested Visa, Mastercard, and American Express layout.
- [x] Add the TiloPay affiliation CTA and keep only the credential save button on the credentials step.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes

- TiloPay onboarding now has four visible steps: method overview, transaction commissions, account affiliation, and credentials.
- Added the requested Visa, Mastercard, and American Express fee breakdown with TiloPay `3.75% + $0.50` and VentaGo `0.50%` messaging.
- Added the TiloPay account affiliation CTA opening `https://web.tilopay.com/start/affiliation-pty`.
- The credentials step now labels fields as `Llave API`, `Usuario API`, and `Contraseña API`, includes the saved-credential security note for configured accounts, and avoids duplicate save buttons during onboarding.
- Metadata compilation completed successfully and was skipped as up-to-date; existing KMP cinterop commonization warning remains unrelated.

# Payment Channels Visual Refresh TODO

## Plan

- [x] Rework `PaymentMethodsScreen` channels into the provided grouped visual structure.
- [x] Keep channel availability/navigation logic unchanged while updating labels, logos, spacing, and dark-mode-aware colors.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes

- Rebuilt the payment channels section as two groups: `Cobros físicos` and `QR y links de pago`, matching the provided hierarchy with icon headers and rounded method-list containers.
- Preserved method visibility and navigation behavior while changing row labels, logo treatment, spacing, dividers, and chevron styling.
- Used theme-aware surface, outline, text, and primary colors so the section remains compatible with dark mode.
- Metadata compilation completed successfully and was skipped as up-to-date; existing KMP cinterop commonization warning remains unrelated.

# Payments Onboarding Copy TODO

## Plan
- [x] Update first-time payments onboarding hero title/body and checklist copy.
- [x] Add linked TecoDigi payments terms text below the primary button.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes
- First-time payments onboarding now uses the requested VentaGo payment-channel headline, body, three checklist items, and bold closing line.
- Removed the stale secondary explainer card that still referenced the old channel list.
- Added the TecoDigi payments terms sentence below the primary button with the terms phrase linked to `https://tecodigi.com/paas-terminos-condiciones/`.
- Metadata compilation completed successfully and was skipped as up-to-date; existing KMP cinterop warning remains unrelated.

# Remove Payments Beta Gate TODO

## Plan
- [x] Confirm payment route/action authorization is already scope-only in production code.
- [x] Remove POS/payment settings beta visibility state so payment links and Yappy en caja are available to all eligible businesses.
- [x] Update focused authz tests and task lessons for the new product rule.
- [x] Run focused verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.core.authz.AuthzEvaluatorTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes
- Removed the `payments` beta enum and stale POS `hasPaymentsBeta` state.
- POS now shows payment-link and Yappy en caja options without beta membership; existing permission/configuration guards still control whether a user can select and complete those flows.
- Updated the blocked payments copy to remove beta language.
- Updated authz tests so payment routes/actions require scopes only, while users without scopes remain blocked.
- Added missing payments screen mode/event definitions required by the current payments UI code to compile.
- Focused authz test passed. Metadata compilation completed successfully and was skipped as up-to-date; existing KMP cinterop warning remains unrelated.

# Payments Web Parity TODO

## Plan
- [x] Audit the current KMP payment implementation against `payments-kmp-replication.md`.
- [x] Fill missing shared API contracts, provider/repository/service methods, and normalization helpers.
- [x] Complete settings payment overview, channel setup pages, fee billing detail/checkout, and home fee prompt parity.
- [x] Complete POS payment-link/Yappy onsite creation, pending-conflict handling, confirmation runtime, and payment-method replacement parity.
- [x] Complete order-detail link generation, payment replacement, cancellation/payment maintenance, and ACH review parity.
- [x] Add or update focused contract/ViewModel tests for newly implemented flows.
- [x] Run verification gates and record results.

## Verification Gates
- [x] Focused source audit against all documented endpoints and UI flows.
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes
- Implemented payment configuration parity across PayPal, Yappy links, Yappy en caja groups/devices, ACH, TiloPay/card, manual/onsite visibility, and financial-profile configured-state helpers.
- Added missing payment API contracts and service paths for TiloPay status/config/disconnect, direct fee checkout, Yappy onsite group/device CRUD, pending transaction cancellation, fee batch filtering, and replacement payment intents.
- Completed app flows for payment settings, fee billing, home fee prompts, POS draft/payment-link/Yappy onsite creation, pending-conflict recovery, QR polling/cancellation, and order-detail payment replacement/manual-payment transitions.
- Verification passed: common metadata compile, Android host tests, and metadata compile. Existing unrelated Gradle warnings remain.

# Get Orders Related Documents TODO

## Plan
- [x] Confirm current `get-orders` parsing, order detail rendering, and related-order navigation.
- [x] Show the related document type as the backend document type code plus user-friendly label.
- [x] Add focused contract coverage for `related_documents` inside the paginated `get-orders` response envelope.
- [ ] Run focused verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.orders.CxcOrderDetailsContractTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes
- Confirmed `Order.relatedDocuments` is already decoded from `related_documents` in `/api/v1/orders/get-orders` list items and rendered by `OrderDetailsScreen` when present.
- The related documents card now shows the backend document type code with the readable label, for example `04 - Nota de crédito`, alongside the clickable order number and amount.
- Clicking a related order number continues to route through `OrdersScreenRoute(orderNumber = ...)`, which finds/selects the target order and opens `OrderDetailsScreen`.
- Added contract coverage for the paginated `get-orders` envelope containing `related_documents`.
- Focused Android host test passed. Metadata compilation succeeded and was skipped as up-to-date; existing KMP cinterop warning remains unrelated.

# Order Details Credit Notes TODO

## Plan
- [x] Add a reusable order helper for credit-note document types `04` and `06`.
- [x] Hide credit/plazos, rescheduling, and payment collection actions on credit-note order details.
- [x] Guard ViewModel payment/reschedule/payment-link entry points against credit notes.
- [x] Add focused tests for the new credit-note behavior.
- [x] Run verification gates and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.orders.CxcOrderDetailsContractTest --tests com.teco.ventago.features.orders.ui.order_details.viewmodel.OrdersDetailsViewModelCxcTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes
- Added `Order.isCreditNoteDocument()` and `Order.supportsReceivableActions()` so document types `04` and `06` are centrally treated as credit notes for receivable/payment UI policy.
- `OrderDetailsScreen` now hides the `Cuotas de pago` card, `Reprogramar cuotas`, `Registrar pago`, payment-link collection actions, and related sheets for credit-note orders while preserving read-only invoice/order information.
- `OrdersDetailsViewModel` now blocks payment link generation/sharing, draft invoice payment collection, register-payment open/submit, and receivable reschedule open/confirm paths for credit notes.
- Added focused coverage that normal invoices and debit notes still support receivable actions, while referenced and generic credit notes do not.
- Verification passed. `compileKotlinMetadata` completed successfully with the task skipped as up-to-date; existing KMP cinterop warning remains unrelated.

# Yappy En Caja Onsite Payments TODO

## Plan
- [x] Add onsite Yappy summary, group/device, transaction, and order response contracts.
- [x] Add `Yappy en caja` settings channel with onboarding steps for groups and devices.
- [x] Add POS onsite payment mode with QR display, polling, cancellation, invoice wait states, and ticket printing.
- [x] Add focused serialization/parsing/polling tests.
- [x] Run verification gates and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes
- Added `Yappy en caja` to payment methods, config summary parsing, financial-profile configured-state checks, settings navigation, and localized screen titles.
- Added typed Yappy onsite group/device/transaction/cancel contracts through provider, repository, and service layers; secrets are only sent in configure requests and are not rendered in saved summaries.
- Added a four-step onboarding/configuration flow using observed branches and billing points for dropdowns, with saved group/device summaries and aggregate open-session counts from config summary.
- Added POS onsite mode, current branch/billing-point device eligibility checks, `payment_flow_type = "in_place"`, `PaymentLinksBlock.method = "YAPPY_ONSITE"`, QR rendering, transaction polling, pending invoice handling, cancel/expired states, PDF download, invoice retry, and ticket auto-print when a configured printer and ticket payload exist.
- Added serialization/parsing coverage for onsite order creation, `onsite_payment`, config summary, group/device requests, transaction polling states, invoice issued with/without ticket, invoice failed, expired, cancelled, and returned.
- Verification passed. `compileKotlinMetadata` is invoked successfully but Gradle currently skips the task after resource checks.

# Related Credit/Debit Notes TODO

## Plan
- [x] Add `related_documents` parsing and credit/debit helper logic to orders.
- [x] Add contract coverage for related documents and credit-note capacity calculations.
- [x] Show related credit/debit notes on order details with clickable order-number navigation.
- [x] Pass remaining credit-note capacity into referenced POS note creation.
- [x] Block referenced credit-note submission when the new note exceeds the remaining allowed amount.
- [x] Hydrate stale selected order details once on detail open so `related_documents` appears for cached orders.
- [x] Run focused tests/compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.orders.CxcOrderDetailsContractTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.pos.ui.viewmodel.PosNoteValidatorsTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes
- Added `related_documents` to the shared `Order` model with helper methods for credit/debit note classification and active credit-note capacity.
- `OrderDetailsScreen` now shows a related documents card with clickable order numbers routed through `OrdersScreenRoute(orderNumber = ...)`.
- Referenced credit-note creation now receives the remaining credit capacity and blocks submit before API calls when the note total exceeds that amount.
- Follow-up: order details now refreshes the selected order once from `/find/order-id` when the cached selected order has no related documents, fixing stale cache cases like order 715.
- Focused tests and metadata compile passed. Metadata compilation was skipped as up-to-date after the Android host test compiled common code.

# iOS App Encryption Export Compliance TODO

## Plan
- [x] Review Apple export-compliance documentation for the App Store Connect encryption algorithm question.
- [x] Inspect iOS target dependencies and source for proprietary, non-standard, or app-implemented standard crypto.
- [x] Verify iOS networking/storage paths and identify whether encryption is limited to Apple OS APIs.
- [x] Record the recommended App Store Connect answer.

## Verification Gates
- [x] Apple docs reviewed: encryption limited to Apple's operating system requires no App Store Connect encryption documentation; non-Apple standard algorithms or proprietary algorithms require documentation.
- [x] Repo scan covered iOS/KMP source, Xcode package/product dependencies, Info.plist, Ktor engine configuration, SecureStorage, Firebase, Epson, and HMAC/security keywords.
- [x] Confirmed iOS HTTP client uses Ktor Darwin engine and iOS secure storage uses Keychain/Security APIs.
- [x] Confirmed app-owned HMAC implementation is Android-only; iOS actual implementation returns an empty string.

## Review Notes
- Recommended answer for the current iOS app: `None of the algorithms mentioned above`.
- The app does use/access encryption through Apple OS facilities: HTTPS/TLS through the Darwin engine and secure storage through Keychain/Security.
- No app-owned proprietary or non-standard encryption implementation was found.
- No app-owned iOS implementation of standard algorithms such as AES/RSA/HMAC/SHA was found. The only direct HMAC code is in `androidMain`; iOS stubs it out.
- iOS links Firebase products and a transitive `grpc-binary` package; no app source directly implements or configures cryptographic algorithms through those dependencies.
- If future iOS changes add app-level crypto or a third-party TLS/crypto library used directly for encryption, re-run this review before answering App Store Connect.

# Delete Account Login Reset TODO

## Plan

- [x] Trace current account-deleted event handling and sign-out cleanup.
- [x] Add a dedicated account-deleted navigation callback that clears the back stack to login.
- [x] Verify deletion cleanup still clears cache/tokens/session and no Firebase deletion exists.
- [x] Run focused iOS compile verification and record results.

## Verification Gates

- [x] Focused source scan for account deletion navigation and Firebase deletion patterns.
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosArm64`

## Review Notes

- `SettingsScreen` now handles `AccountDeleted` through a dedicated `onAccountDeleted` callback instead of generic route navigation.
- Settings navigation now sends the user to `LoginScreen` with `popUpTo(PosScreens.LoginRegister.name) { inclusive = true }` and `launchSingleTop`, clearing authenticated screens from the stack.
- Successful backend deletion now performs sign-out-style cleanup without Firebase account deletion: cancels user listeners, removes change listeners, signs out of Firebase, clears cache/user state, deletes JWT/refresh JWT, and clears the session id.
- Focused scan confirmed no Firebase account-delete patterns remain; the only Firebase auth operation in deletion cleanup is `firebase.signOut()`.
- iOS ARM64 Kotlin compilation passed. Existing warnings remain from unrelated iOS printer/PDF/expect-actual code.

# Internal-Only Account Deletion TODO

## Plan

- [x] Inspect the delete-account success path and identify the Firebase failure source.
- [x] Remove Firebase account deletion from successful internal account deletion.
- [x] Verify source no longer calls `firebase.deleteAccount()` from `AuthService.deleteAccount`.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] Focused source scan for `firebase.deleteAccount`.
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosArm64`

## Review Notes

- `AuthService.deleteAccount` now treats a successful backend deletion as the source of truth and no longer calls Firebase account deletion.
- Removed the unused `deleteAccount()` API from `IFirebaseService`/`FirebaseService` so common auth code no longer exposes `FirebaseAuth.currentUser.delete()`.
- Successful deletion now cancels user-change collection, removes change listeners, clears local cache/user state, deletes stored JWT/refresh JWT, and clears the session id.
- Focused source scan confirmed no Firebase account-delete patterns remain in the common/iOS app source; remaining `deleteAccount` references are the internal endpoint flow and Settings trigger.
- iOS ARM64 Kotlin compilation passed. Existing warnings remain: cinterop commonization disabled, Skiko version mismatch, and unrelated iOS printer/PDF warnings.

# Account Deletion Response Contract TODO

## Plan

- [x] Inspect current delete-account provider/repository parsing.
- [x] Model the backend response contract without manual JSON payload construction.
- [x] Add focused tests for `status: true`, `status: false`, and Yii 404 handling.
- [x] Run focused tests/compile verification and record results.

## Verification Gates

- [x] Focused account-delete source scan.
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.auth.data.provider.DeleteAccountResponseParsingTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosArm64`

## Review Notes

- `AuthProvider.deleteAccount` now sends a typed `DeleteAccountRequest` instead of a raw JSON string payload.
- Added explicit delete-account response parsing: `{"status": true}` maps to success, `{"status": false}` maps to a false result without an API error, and non-2xx Yii JSON such as 404 is preserved in `ApiResponse.data` with `ApiError.UNDEFINED`, `errorCode`, and `errorMessage`.
- `UserRepository.deleteAccount` continues to return `false` for backend `status:false`, while non-2xx error responses flow through the existing exception/logging path.
- Added `DeleteAccountResponseParsingTest` covering the three expected backend examples.
- Focused Android host test and iOS ARM64 Kotlin compile passed. Existing warnings remain: cinterop commonization disabled, AGP compile SDK support warning, Skiko version mismatch, and unrelated deprecations/casts.

# Account Deletion Settings Flow TODO

## Plan
- [x] Inspect existing delete-account endpoint/service and Settings UI patterns.
- [x] Add Settings delete-account action below sign out with confirmation dialog.
- [x] Wire ViewModel deletion to backend, loading/success/error feedback, app-state clearing, and login navigation.
- [x] Add localized strings for the confirmation and failure feedback.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] Search Settings/auth delete-account wiring.
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosSimulatorArm64`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosArm64`

## Review Notes
- Re-enabled the visible delete-account action in Settings, directly below `Sign out`, using the existing red settings button style.
- Added a confirmation dialog before deletion with localized English and Spanish copy.
- Confirming deletion now reads the current JWT, calls the existing backend-backed `authService.deleteAccount(token)` flow, shows the existing `LoadingSheet` states, clears feature service state on success, and emits navigation back to `LoginScreen`.
- Failure paths now emit an error state and show a localized snackbar message.
- Focused scan confirmed delete-account UI state, events, strings, and backend service wiring are present.
- Metadata, iOS simulator ARM64, and iOS device ARM64 Kotlin compilation passed. Existing warnings remain: cinterop commonization disabled, Skiko version mismatch, and unrelated deprecations/casts.

# Disable In-App Account Creation TODO

## Plan
- [x] Trace all registration entry points from login/navigation.
- [x] Remove or disable visible account creation actions so reviewers cannot start self-service registration.
- [x] Guard registration routes in navigation in case an internal path tries to open them.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] Search auth UI/navigation for `Create account`, register routes, and register screen calls.
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosSimulatorArm64`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosArm64`

## Review Notes
- Removed the login-screen self-registration CTA (`¿No tienes cuenta? / Sign Up`) and the email-not-found dialog action that navigated to account registration.
- `ApiError.F_AUTH_002` now surfaces as a normal sign-in error instead of offering registration.
- The `RegisterScreen` nav route now redirects to `LoginScreen`, so accidental/internal navigation cannot render the account creation form.
- Existing logged-in business onboarding (`BusinessRegisterScreen`) remains unchanged for users whose account already exists but lacks a business.
- Focused scans show no remaining `navigate(PosScreens.RegisterScreen)` calls from auth UI; remaining matches are the enum value and the unused screen function.
- Metadata, iOS simulator ARM64, and iOS device ARM64 Kotlin compilation passed. Existing warnings remain: cinterop commonization disabled, Skiko version mismatch, and unrelated deprecations/casts.


# iOS App Store Tracking Rejection Review TODO

## Plan
- [x] Review Apple tracking/ATT requirements against this app's actual SDK usage.
- [x] Inspect iOS/KMP dependencies and source for IDFA, ad attribution, tracking SDKs, and privacy manifests.
- [x] Decide whether the correct fix is App Store Connect privacy label updates or ATT implementation.
- [x] Record verification evidence and recommended App Review response.

## Verification Gates
- [x] Search repo for `AppTrackingTransparency`, `NSUserTrackingUsageDescription`, `AdSupport`, `IDFA`, ad/attribution SDKs, Firebase, analytics, and privacy manifests.
- [x] Review Gradle/Xcode dependency declarations for iOS-relevant SDKs.
- [x] Cross-check against official Apple tracking definition.

## Review Notes
- Apple requires ATT only when app/user/device data is linked with third-party data for targeted advertising/advertising measurement or shared with a data broker.
- iOS repo scan found no direct `AppTrackingTransparency`, `NSUserTrackingUsageDescription`, `AdSupport`, `ASIdentifierManager`, IDFA, AdMob, AppsFlyer, Adjust, Branch, Facebook/Meta, or similar attribution SDK usage.
- iOS project links Firebase SDK 11.2.0 products: Analytics, Auth, Crashlytics, Database, Firestore, and Messaging.
- `GoogleService-Info.plist` sets `IS_ANALYTICS_ENABLED` to false and `IS_ADS_ENABLED` to false, but shared KMP code still instantiates `Firebase.analytics`, logs events, sets user ID, and sends user/email/business parameters.
- Android has FingerprintJS device fingerprinting, but iOS `FingerPrintService` currently returns the literal `"IOS"` for backend fingerprint headers, so Android-only fingerprinting should not be treated as iOS tracking.
- No app-level `PrivacyInfo.xcprivacy` was found in source; Firebase may provide SDK manifests through SPM dependencies, but the app's App Store Connect privacy answers still need to match actual behavior.
- Recommended default fix if VentaGo does not use ads attribution/data brokers/cross-app tracking on iOS: update App Store Connect privacy information so collected data is not marked as "used for tracking", then reply to App Review explaining the iOS app does not track and does not access IDFA.
- If Firebase/Google Analytics is configured for Google Ads attribution, Google signals, ads personalization, or cross-company advertising measurement, implement ATT before enabling that collection or disable those features/remove FirebaseAnalytics on iOS.

# iOS Crashlytics dSYM Upload TODO

## Plan
- [x] Confirm Firebase's current Xcode 15 Crashlytics dSYM run-script requirements.
- [x] Inspect the iOS target build phases and debug-symbol settings.
- [x] Add the Crashlytics dSYM upload script and required input files with minimal pbxproj changes.
- [x] Verify the project metadata and record results.

## Verification Gates
- [x] Xcode project file parses with `plutil -lint`.
- [x] Crashlytics run script and required input files are present in `project.pbxproj`.
- [x] Xcode Debug/Release build settings resolve `DEBUG_INFORMATION_FORMAT=dwarf-with-dsym`.

## Review Notes
- Added `Upload Crashlytics dSYMs` as the final iOS target build phase, using the Swift Package Manager script path from Firebase:
  `"${BUILD_DIR%/Build/*}/SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/run"`.
- Added the Xcode 15 input files required by Firebase: dSYM bundle, DWARF binary, dSYM `Info.plist`, built `GoogleService-Info.plist`, and app executable.
- Updated Debug `DEBUG_INFORMATION_FORMAT` from `dwarf` to `dwarf-with-dsym`; Release already produced dSYMs.
- Did not add the optional `.debug.dylib` input because Xcode resolves `ENABLE_USER_SCRIPT_SANDBOXING=NO` for both Debug and Release.
- Verification passed: `plutil -lint iosApp/iosApp.xcodeproj/project.pbxproj`, required-path scan, and `xcodebuild -showBuildSettings` for Debug and Release.

# POS Cart Item Edit Sheet TODO

## Follow-up Plan
- [x] Make `Cancelar` / `Aplicar` a flush fixed footer instead of an elevated floating bar.
- [x] Remove the summary card elevation that creates the odd shadow.
- [x] Improve outside-tap keyboard dismissal for taps inside the scrollable sheet content.
- [x] Rerun focused compile verification and record results.

## Plan
- [x] Update `ModifyCartItemSheet` to match the provided full-screen secondary-accent card style.
- [x] Add editable product name and pass it through the cart update flow.
- [x] Convert renamed saved-product lines into personalized cart lines with no catalog item ID.
- [x] Fix quantity input width so multi-digit quantities remain visible.
- [x] Clear focus/keyboard when tapping outside inputs and clarify discount copy as per-item.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- `ModifyCartItemSheet` now uses a full-screen sheet with a drag handle, large title/subtitle header, circular close action, secondary-accent cards, pill discount controls, a summary card, and a fixed bottom action row.
- Added a product-name input. When a saved cart line is renamed, `PosViewModel` converts only that line to a personalized item (`itemId = -1`) keyed by the existing line ID, preserving source tax/unit/additional metadata where available.
- Quantity editing now uses a wider centered `OutlinedTextField`, so multi-digit quantities remain visible.
- The sheet clears focus and hides the keyboard on outside taps.
- Discount labels now explicitly say `Descuento por ítem`.
- Final metadata compile succeeded but Gradle skipped `:composeApp:compileKotlinMetadata` as up-to-date. Final Android debug Kotlin compilation passed after adjusting typography helper usage.
- Follow-up: removed footer elevation/shadow while keeping it fixed outside the scrollable content, flattened the summary card elevation to avoid the odd shadow, and changed keyboard dismissal to track input bounds so outside taps in the sheet content clear focus without immediately clearing taps on inputs.
- Follow-up verification passed: `:androidApp:compileDebugKotlin`; metadata compile succeeded with `:composeApp:compileKotlinMetadata` skipped as up-to-date.

# Edit Product Collapsible Layout TODO

## Plan
- [x] Compare add-product card layout against current edit-product `ItemScreenContent`.
- [x] Move edit-product fields into the same basic/image/identification/fiscal/additional collapsible-card layout.
- [x] Preserve existing edit image-picker, barcode scanner, permission, loading, and save behavior.
- [x] Run focused KMP/Android compile verification.
- [x] Record review notes and lessons.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Edit-product `ItemScreenContent` now uses the same card layout pattern as add-product: basic info card plus collapsible cards for additional taxes, identification/control, and DGI fiscal info.
- Removed the edit-product Básico/Avanzado tab from the layout so optional groups are accessed through the same collapsible cards as add-product.
- Reused the add-product card/content helpers as module-internal composables to avoid duplicating the section layout.
- Added an edit-specific image picker wrapper so existing remote product images still render until a new local image is selected.
- Kotlin metadata and Android debug Kotlin compilation passed.


# Edit Product Image Picker TODO

## Plan
- [x] Compare add-product, edit-product, and logo image picker wiring to identify the broken callback path.
- [x] Apply the minimal fix so edit-product camera/gallery options launch the shared picker managers.
- [x] Run focused KMP/Android compile verification.
- [x] Record review notes and any prevention lesson.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Edit-product image selection is rendered by `ItemScreenContent`, not `AddItemScreen`.
- The bottom sheet buttons were only hiding the sheet; they did not set `launchCamera` or `launchGallery`, so no platform picker was launched.
- Added the missing launch triggers and reset camera/gallery/settings flags after consumption to match the working add-product/logo pattern.
- Kotlin metadata and Android debug Kotlin compilation passed.


# POS Cart And Product Total Button TODO

## Plan
- [x] Change cart-screen invoice CTA from `Nueva factura` plus amount to `Continuar factura` without amount.
- [x] Remove the amount suffix from cart-screen quote CTAs as well.
- [x] Change product-selection CTA amount to use the current tax-aware invoice total instead of subtotal-only cart total.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Cart-screen invoice CTA now displays `Continuar factura` with no amount.
- Cart-screen quote CTAs keep their existing labels but no longer append an amount.
- Product-selection bottom bar now formats `viewModel.legalInvoiceTotal()` so the visible amount includes currently known taxes and respects `taxExempt`.
- Android debug Kotlin compilation passed.

# Invoice Preview Spanish Accents TODO

## Plan
- [x] Review hardcoded Spanish UI text in `InvoicePreviewScreen.kt`.
- [x] Fix missing Spanish accents without changing invoice preview layout or behavior.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Fixed missing accents in invoice preview labels and headings: `electrónica`, `Dirección`, `Facturación`, `código`, `Identificación`, `Número`, `emisión`, and `Ítems`.
- Re-scanned for the unaccented forms in `InvoicePreviewScreen.kt`; remaining matches are only function names, not user-facing copy.
- Android debug Kotlin compilation passed.

# POS Grid Add New Product Icon TODO

## Plan
- [x] Add a plus icon to the left of the grid-mode `Agregar Nuevo` text.
- [x] Keep the existing grid card size and centered alignment.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Grid-mode manual product card now centers a plus icon and `Agregar Nuevo` label as one row.
- Existing card height, dashed border, and centered alignment are unchanged.
- Android debug Kotlin compilation passed.

# POS Registered Customer Card TODO

## Plan
- [x] Make the registered-customer option card open the existing customer search screen when selected.
- [x] Remove the duplicated select-client button from the registered-customer section while keeping selected customer details visible.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Registered-customer option card now selects registered customer and opens `PosScreens.SearchCustomerScreen` directly.
- The first POS page no longer renders the separate `Seleccionar cliente` button after registered-customer selection.
- Selected registered customer details remain visible in a compact secondary-color summary card.
- Android debug Kotlin compilation passed.

# POS Manual Product Button Label TODO

## Plan
- [x] Change list-mode manual product button text to `Agregar Nuevo`.
- [x] Simplify grid-mode manual product card to one centered `Agregar Nuevo` label while preserving size.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- List-mode manual product dotted button now says `Agregar Nuevo`.
- Grid-mode manual product dotted card keeps the same 104 dp height and now renders only centered `Agregar Nuevo` text.
- Android debug Kotlin compilation passed.

# POS Payments Beta Link Option TODO

## Plan
- [x] Make the product-added snackbar action button use secondary as the button content color.
- [x] Track whether the current business has the `payments` beta in POS state.
- [x] Hide the payment-link option in `PaymentScreen` when the `payments` beta is not enabled.
- [x] Keep existing payment-link permission/configuration guards for beta users.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Product-added snackbar action now sets `TextButton` content color to `MaterialTheme.colorScheme.secondary`.
- POS state now tracks `hasPaymentsBeta` from the existing beta snapshot.
- `PaymentScreen` only renders `Crear enlace de pago` when `hasPaymentsBeta` is true.
- Existing `canCreatePaymentLink` and payment-method configuration checks still control whether beta users can select/create a link.
- Android debug Kotlin compilation passed.

# Orders Customer Name Filter TODO

## Plan
- [x] Inspect current order filter request/state and existing customer search APIs.
- [x] Replace the RUC filter state with customer-name search plus selected customer ID.
- [x] Update the orders filter sheet to show at most four matching customers and apply the selected customer ID.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Orders filter sheet now uses `Nombre del cliente` instead of `RUC del cliente`.
- Typing at least 2 characters debounces a customer search by name through `CustomerService.searchCustomersByName(...)`.
- Customer results are requested with limit 4 and rendered with an additional `take(4)` cap.
- Selecting a customer stores `customerIdFilter`, and orders continue using the existing `customer_id` field in `ListOrdersRequest`.
- Apply is disabled when a name is typed without a selected customer, so free text is never sent as an order filter.

# POS Final Customer Email Validation TODO

## Plan
- [x] Add final-customer email error state and validate blank vs malformed values.
- [x] Show inline error on the final-customer email input.
- [x] Block continue/order creation when the final-customer email is malformed and show feedback.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Final customer email now reuses the shared `emailRegex`; blank remains valid because the field is optional.
- Invalid final customer email now shows inline `Correo inválido`.
- `validateFinalCustomerSelection()` now normalizes email, stores the email error, shows feedback, and blocks navigation/order creation when email is malformed.
- Android debug Kotlin compilation passed.

# POS Product Added Snackbar TODO

## Plan
- [x] Confirm saved and personalized product add-to-cart paths.
- [x] Show a `Producto agregado` snackbar with `Facturar` action after catalog/saved products are added from POS.
- [x] Show the same snackbar after personalized manual products are returned and added to the cart.
- [x] Make the snackbar action navigate to the same cart screen as the bottom cart button.
- [x] Set the product-added snackbar height to `56.dp`.
- [x] Use secondary color for the snackbar `Facturar` action text.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- `PosViewModel.addItemToCart` now increments a product-added snackbar token after successful positive-quantity additions.
- Saved/catalog products and personalized manual products both use `addItemToCart`, so both paths trigger the same feedback.
- `PosProductScreenBottomBar` hosts a `Producto agregado` snackbar over the cart bottom-button area with `Facturar` action.
- The snackbar action navigates to `PosScreens.CartScreen`, matching the mobile cart button destination.
- The bottom bar overlay keeps the same 72 dp footprint as the cart button area so the snackbar covers it without resizing the bottom bar.
- The snackbar content now uses a custom `Snackbar` with `Modifier.height(56.dp)`.
- The snackbar `Facturar` action text now uses `MaterialTheme.colorScheme.secondary`.
- The bottom bar tracks the last shown token locally so returning to the product screen does not replay an old snackbar.

# Product Service Selector TODO

## Plan
- [x] Confirm normal and personalized product entry share the same add-item form.
- [x] Add a product/service selector in the basic information card with product selected by default and secondary-color selected styling.
- [x] When service is selected, set product type to service, hide the unit measure input, and force `und` internally.
- [x] Preserve saved-product and unsaved personalized-product payload behavior.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Normal product and personalized product creation share `AddItemScreen`; personalized mode is driven by `NavResults.KEY_IS_PERSONALIZED_PRODUCT`.
- Added a two-option selector in the basic information card with product selected by default and selected styling driven by `MaterialTheme.colorScheme.secondary`.
- Removed the duplicate optional product-type dropdown from the identification card.
- When service is selected, the unit measure dropdown is hidden and state is forced to `unitMeasureCode = "und"` for both unsaved personalized items and saved products.

# POS Invoice Configuration Card TODO

## Plan
- [x] Locate the first invoice creation page dropdowns and the order/invoice payload path.
- [x] Add a collapsed `Invoice configuration` card that contains branch, billing point, invoice type, invoice nature, and invoice date.
- [x] Add invoice date state with default Panama current datetime and a date selector limited to six months in the past.
- [x] Serialize `issued_datetime` as current Panama datetime for today, or selected date at `T00:00:00` for non-today dates.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- The first POS invoice page now shows one collapsed `Configuración de factura` card for branch, billing point, invoice type, operation nature, and invoice date.
- Invoice date defaults to the current Panama date and uses the existing KMP date selector with a min date six months before today and max date today.
- `issued_datetime` resolves to the current Panama datetime when the selected invoice date is today, otherwise to the selected date at `T00:00:00`.

# POS Add Product Keyboard TODO

## Plan
- [x] Confirm whether normal and personalized product entry share the same add-product screen.
- [x] Make the add-product scroll container react to the keyboard so focused inputs can move above it.
- [x] Dismiss the keyboard when tapping outside add-product inputs.
- [x] Preserve existing product form behavior and unrelated worktree changes.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- POS normal-product and personalized-product entry both use `features/product/ui/item/add/AddItemScreen.kt`; personalized mode is selected from `NavResults.KEY_IS_PERSONALIZED_PRODUCT`.
- The add-product scroll container now applies navigation-bar padding, IME padding, and an extra bottom spacer so lower fields/actions can scroll above the iOS keyboard.
- Tapping outside inputs in the add-product form now clears focus, dismissing the keyboard.

# POS Product Search Controls TODO

## Plan
- [x] Make product search typed text visible in iOS dark mode.
- [x] Match search bar and layout-toggle heights.
- [x] Replace the two-button view selector with one icon button that toggles list/grid mode.
- [x] Dismiss the keyboard when tapping outside the product search input.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Product search now uses explicit `onSurface` text and cursor colors so typed text remains visible in iOS dark mode.
- Search input and layout toggle share a 56 dp control height.
- The grid/list selector is now a single square toggle button that switches to the opposite mode and updates the icon.
- Tapping outside the product search input clears focus, dismissing the keyboard.

# POS Payment Dynamic Action Spacing TODO

## Plan
- [x] Replace the fixed spacer between the distribution card and invoice actions with measured dynamic space.
- [x] Keep invoice actions in normal scroll flow so small devices can scroll instead of overlapping the card.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- The manual payment area now measures available viewport height after the payment selector and bottom padding.
- Distribution card and invoice actions are placed by a measured layout: extra height becomes dynamic space, while small devices keep a minimum gap and scroll normally.

# POS Final Customer Card And Keyboard TODO

## Plan
- [x] Inspect POS customer, product, and customer-search input layouts for keyboard behavior.
- [x] Simplify final-customer additional-information card colors so it uses the normal card surface hierarchy in light/dark mode.
- [x] Make POS form content scrollable above the iOS keyboard and dismiss keyboard on outside taps across the affected POS inputs.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Final-customer additional information now uses the add-product plain-card pattern: `CardDefaults.cardColors(containerColor = vanishedBackgroundColor())`, 12 dp shape, and a small 4 dp outer inset so no elevated tonal/content overlay appears in dark mode.
- POS final-customer details, add/edit customer, search customer, and add/edit product form containers now apply IME padding so the content can scroll above the keyboard.
- POS customer forms and product form containers dismiss the keyboard when tapping outside inputs.

# POS Payment Action Spacing TODO

## Plan
- [x] Increase the gap between the distribution card and create-invoice actions.
- [x] Add scroll-content bottom padding so actions have 16 dp breathing room at the bottom.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Superseded by dynamic spacing: distribution card and invoice actions now use measured remaining viewport space instead of a fixed large spacer.
- Payment screen content now ends with a 16 dp spacer, keeping the action group from touching the bottom and allowing small screens to scroll naturally.

# POS Payment Link Card And Invoice Actions TODO

## Plan
- [x] Restyle payment-link section as a card matching the payment distribution card.
- [x] Make the generate-link CTA use the secondary color.
- [x] Restyle invoice generation actions to match the provided primary, preview, separator, and draft-button layout.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Payment-link section now uses the same `Surface` shape, border, and elevation style as the payment distribution card.
- Generate-link and create-invoice primary CTAs use `MaterialTheme.colorScheme.secondary` with leading icons.
- Invoice actions now follow the mock structure: filled create button, outlined preview button, centered `o` divider, and centered save-draft text with helper copy.

# POS Payment Option Card Icons TODO

## Plan
- [x] Replace manual/payment-link option text markers with Material icons.
- [x] Use cash-style icon for manual payment and link icon for payment link.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Manual payment now uses the cash-style Material `Payments` icon because this Compose icon set does not expose a direct `Cash` icon.
- Payment link now uses the Material `Link` icon because this Compose icon set does not expose `Link2`.

# POS Payment Dashed CTA Secondary Accent TODO

## Plan
- [x] Update dashed select-payment-method CTA to use secondary color.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Dashed `Seleccionar método de pago` CTA now uses `MaterialTheme.colorScheme.secondary` for the dashed border, add icon, and label.

# POS Payment Selector Secondary Accent TODO

## Plan
- [x] Update selected manual/link payment option styling to use secondary color.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Selected `Pago manual` / `Crear enlace de pago` option cards now use `MaterialTheme.colorScheme.secondary` for selected border and background accents.

# POS Payment Distribution Row Subtitles TODO

## Plan
- [x] Hide distribution-row subtitles for regular payment methods.
- [x] Keep subtitles visible for credit and other payment rows.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Distribution rows now omit subtitles for regular payment methods.
- Credit rows keep due-date status subtitles.
- Other rows show the entered custom payment-method description as their subtitle.

# POS Payment Method Icons And Other Draft TODO

## Plan
- [x] Replace payment method string badges with Material icons.
- [x] Map each manual payment code to the requested icon.
- [x] Add an `Other` draft bottom sheet with amount and payment-method description before adding it.
- [x] Require at least 15 characters for the `Other` method description and do not add it on dismiss.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Replaced `paymentMethodIconLabel` with an `ImageVector` mapper and rendered icons in selected payment rows and method-picker rows.
- Mapped transfer, card, cash/Punto Pago, credit, loyalty/vale, gift card, and other to Material icons; cheque uses `FactCheck` because this Compose icon set does not expose `Checkbook`.
- Added an `Other` draft bottom sheet that asks for amount and the customer-used payment method before adding code `99`.
- Dismissing the `Other` sheet does not mutate selected methods; `Listo` is disabled until amount is positive and the description has at least 15 characters.

# POS Payment App Bar Title TODO

## Plan
- [x] Remove the duplicate payment section title/subtitle from `PaymentScreen`.
- [x] Update the payment route app bar title to `Forma de pago`.
- [x] Apply the requested shared app bar title typography.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Removed the local `Forma de pago` and `Acepta uno o varios` header row from the POS payment selector.
- Updated `pos_payment` so the payment screen app bar shows `Forma de pago` in Spanish and `Payment method` in English.
- Updated `DMTopAppBar` title rendering to use `MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.W800)` for all app bar titles.

# Date Picker Month Labels TODO

## Plan
- [x] Update the shared KMP date picker month dropdown to display month names instead of month numbers.
- [x] Keep year/day dropdowns numeric and preserve ISO date output.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Updated the shared `InstallmentDueDateFieldKmp` date picker dropdown helper to accept a display label formatter.
- Month dropdown now displays Spanish month names (`Enero`, `Febrero`, ..., `Diciembre`) while still storing numeric month values internally.
- Year and day dropdowns remain numeric, and accepted dates continue to emit ISO `YYYY-MM-DD`.

# POS Payment Method Sheet Refinement TODO

## Plan
- [x] Restyle method-selection rows as icon + method name with the right action text and no generic subtitle.
- [x] Remove the pending-assignment subtitle under the sheet title.
- [x] Reorder selectable methods: bank transfer, credit card, debit card, cash, credit, then remaining methods.
- [x] Show credit as a normal row with a due-date-required subtitle.
- [x] Stop auto-opening amount edit for non-credit methods after selection.
- [x] For credit, open a draft due-date/amount sheet and only add it on `Listo`.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Method-selection sheet now shows only the `Seleccionar método de pago` title and compact icon/name rows with the right-side action text.
- Selection order is `Transferencia bancaria`, `Tarjeta crédito`, `Tarjeta débito`, `Efectivo`, `Crédito`, then the remaining manual methods.
- Regular methods add immediately with the pending amount assigned and no amount editor popup; the amount editor still opens from the amount chip in the distribution card.
- Credit appears as a normal selectable row, then opens a draft amount/due-date sheet; dismissing that sheet does not add a credit payment.
- Credit creation uses the selected amount and due date only after tapping `Listo`, capped to the pending amount.

# POS Payment Distribution Card TODO

## Plan
- [x] Replace the empty-state icon/text with a dashed "Seleccionar metodo de pago" button that opens method selection.
- [x] Keep that dashed add button visible under selected methods only while there is pending amount to assign.
- [x] Remove the top-right add/edit method button from the distribution card.
- [x] Add a trailing remove icon for each selected payment method row.
- [x] Open an amount-edit bottom sheet when tapping a selected method amount.
- [x] Require credit due date before adding credit payments to the distribution.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Replaced the empty distribution state with a dashed `Seleccionar método de pago` CTA that opens method selection.
- Kept the dashed CTA below selected methods only while there is still a pending amount to assign.
- Removed the distribution card top-right add/edit button; selected rows now expose a trailing X for removal.
- Tapping a selected amount opens a focused amount editor bottom sheet, including `Otro` description and credit due-date editing when relevant.
- Credit payments now require selecting a due date in the method-selection sheet before the installment is created.

# iOS Associated Domains Provisioning TODO

## Plan
- [x] Extract the real failure from the attached device-build log.
- [x] Check the app entitlements and Xcode signing style for capability/profile mismatch.
- [x] Change signing configuration with minimal project impact.
- [ ] Re-run the device-oriented Xcode build command and record the result.

## Verification Gates
- [ ] `xcodebuild -workspace iosApp/iosApp.xcodeproj/project.xcworkspace -allowProvisioningUpdates -allowProvisioningDeviceRegistration -scheme iosApp -configuration Debug -IDECustomDerivedDataLocation=/Users/oscar/Library/Caches/Google/AndroidStudio2026.1.1/DerivedData SYMROOT=/Users/oscar/Library/Caches/Google/AndroidStudio2026.1.1/DerivedData/iosApp-bwcvwyzsqfleafbxrlgnsqqioczp/Build/Products OBJROOT=/Users/oscar/Library/Caches/Google/AndroidStudio2026.1.1/DerivedData/iosApp-bwcvwyzsqfleafbxrlgnsqqioczp/Build/Intermediates.noindex COCOAPODS_SKIP_KOTLIN_BUILD=YES OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES -destination id=00008020-000331E602D9002E -destination-timeout 15 TEST_AFTER_BUILD=NO build`

# iOS Bundle Identifier Configuration TODO

## Plan
- [x] Confirm where the Compose iOS framework and Xcode app identifiers are configured.
- [x] Add the explicit Kotlin/Native framework bundle ID and align the iOS app bundle/team settings.
- [x] Run focused Gradle/Xcode verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:linkDebugFrameworkIosSimulatorArm64`
- [x] `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' build`

## Review Notes
- Added Kotlin/Native `binaryOption("bundleId", "com.tecodigi.ventago.app")` to the generated `ComposeApp` iOS framework to remove the inferred bundle ID warning.
- Updated the iOS Xcode config to use `PRODUCT_BUNDLE_IDENTIFIER=com.tecodigi.ventago.app` and `TEAM_ID=2Q56QR79M2`, and set the target Debug/Release `DEVELOPMENT_TEAM` to `2Q56QR79M2`.
- Verified `composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework/Info.plist` reports `CFBundleIdentifier=com.tecodigi.ventago.app`.
- Verified the built simulator app Info.plist reports `CFBundleIdentifier=com.tecodigi.ventago.app`.

# POS Payment Selection Simplification TODO

## Plan
- [x] Remove the duplicated amount summary card from the payment step.
- [x] Remove the segmented mode switch and use only payment option cards to select manual vs payment link.
- [x] Remove the unavailable on-site payment option from the selector.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Removed the top total/subtotal summary card from the POS payment step; allocation totals remain in the distribution card below.
- Removed the segmented Manual/Enlace/En sitio switch so the payment option cards are the only payment-form selector.
- Removed the unavailable on-site payment option card; only manual and payment link remain visible.
- Preserved the existing manual, payment-link, draft, preview, and credit due-date behavior.

# POS Payment Mockup Alignment TODO

## Plan
- [x] Replace the default TabRow with a rounded segmented control matching Manual/Enlace/En sitio.
- [x] Add the `Forma de pago` method-card selector with selected/unselected card styling from the mockup.
- [x] Restyle the distribution card rows and add-method CTA to better match the mockup.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Replaced the Material TabRow with a rounded segmented control for `Manual`, `Enlace`, and disabled `En sitio`.
- Added mockup-style `Forma de pago` cards for manual, payment link, and on-device payment while keeping the existing payment-link permission/configuration flow.
- Restyled `Distribución del cobro` as a standalone rounded card with a pill `Método` CTA, divider-separated rows, icon badges, and amount pills.
- Existing charge allocation, credit due-date validation, draft, preview, and payment-link creation behavior remained on the same ViewModel state paths.

# POS Payment Selection UI TODO

## Plan
- [x] Inspect current POS payment step state, validation, due-date handling, and order creation payload.
- [x] Replace tip UI with a total-only charge summary.
- [x] Change manual payment selection into an added-charge card plus bottom sheet for choosing method and amount.
- [x] Preserve multi-payment payload behavior and require due date for credit payments.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Removed the visible tips row/button/editor from the POS payment step and made the charge amount resolve to the legal invoice total only, ignoring legacy restored tip state.
- Replaced always-visible payment method chips/inputs with a compact `Distribución del cobro` card and a scrollable bottom sheet for adding/editing manual methods and credit charges.
- Credit charges now start without an automatic due date, show inline required-date feedback, disable confirmation until all credit due dates are selected, and the ViewModel blocks programmatic submit without due dates.
- Preserved the existing `charged` and `installments` state/payload path, including cash overpayment/change behavior and multi-method payment creation.

# Order Details Yappy Payment ID TODO

## Plan
- [x] Inspect order payment model parsing and the Order Details payment card rendering.
- [x] Show the payment id for Yappy payments when a payment id is present.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- `OrderPaymentDto` already parses `payment_intent_id`, so no API/model change was needed.
- `Pagos registrados` now shows `ID de pago: <payment_intent_id>` for Yappy payments with a nonblank payment id.
- Yappy detection uses method name/description and method id `12` from the provided payload, while ACH-specific loading/actions remain unchanged.

# Financial and Operative Reports UI TODO

## Plan
- [x] Add curated KPIs, trend/breakdown charts, and details for Estado de Resultados.
- [x] Add curated KPIs, current-vs-previous charts, and details for Comparativo Financiero.
- [x] Add curated KPIs, margin trend, income/expense donut, and details for Margen Operativo.
- [x] Add curated KPIs, indicator/customer charts, and details for Resumen Ejecutivo.
- [x] Add curated KPIs, cash-flow combo chart, and payable/receivable details for Flujo de Caja.
- [x] Run focused report tests plus Android/metadata compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests 'com.teco.ventago.features.reports.*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`

## Review Notes
- Estado de Resultados now shows requested KPIs, multi-series trend lines, financial composition donut, and margin rows rendered as percent.
- Comparativo Financiero now shows current/previous/variation KPIs, grouped Actual vs Anterior chart, variation chart, and comparison detail rows.
- Margen Operativo now shows revenue/expense/profit/margin KPIs, margin line chart, income/expense/profit donut, and percent margin detail rows.
- Resumen Ejecutivo now maps `business_overview` to requested KPIs, excludes count-only rows from the indicator chart, and renders count-only rows as quantities rather than money.
- Flujo de Caja now shows receivable/payable/net/cobertura KPIs, grouped cash-flow period chart, and detail rows with Cobro/Pago labels plus business-facing action text.

# Expense Reports UI TODO

## Plan
- [x] Add curated KPIs, charts, and detail columns for Resumen de gastos.
- [x] Add curated KPIs, aging chart, and payable detail columns for Antiguedad de CxP.
- [x] Add curated KPIs, account/category charts, and detail columns for Gastos por cuenta.
- [x] Add curated KPIs, supplier charts, and detail columns for Gastos por proveedor.
- [x] Add curated KPIs and paginated detail-only table for Detalle de gastos.
- [x] Add curated KPIs, frequency/supplier/category charts, and detail columns for Gastos recurrentes.
- [x] Run focused report tests plus Android/metadata compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests 'com.teco.ventago.features.reports.*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`

## Review Notes
- Resumen de gastos now shows the requested four KPIs, grouped period bars for subtotal/ITBMS/total, a trend combo chart for total/documents, and period detail rows.
- Antiguedad de CxP now shows payable/overdue/not-due/partial-paid KPIs, grouped aging bars for balance/documents, and document-level payable rows.
- Gastos por cuenta now shows categorized/uncategorized/count/top-account KPIs, category and classification charts, and account rows with code/classification/items/expenses/totals.
- Gastos por proveedor now shows spend/supplier/top-supplier/concentration KPIs, supplier and concentration charts, and supplier rows with RUC, invoices, subtotal, ITBMS, total, and spend percent.
- Detalle de gastos now remains KPI plus paginated detail table only, matching the web UI with no graph fallback.
- Gastos recurrentes now shows monthly estimate/count/top supplier/top category KPIs, frequency/supplier/category charts, and recurring pattern rows with supplier RUC, frequency count, last expense, next due date, status, and actions.

# Tax Reports UI TODO

## Plan
- [x] Add curated KPIs for ITBMS en Ventas with row-derived fallbacks for ITBMS bruto, retenciones, credit notes, and net ITBMS.
- [x] Add curated charts for ITBMS en Ventas: net ITBMS by period and fiscal composition.
- [x] Add curated detail columns for ITBMS en Ventas with business labels and negative display for retentions.
- [x] Add curated KPIs, charts, and detail columns for ITBMS en Gastos.
- [x] Run focused report tests plus Android/metadata compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests 'com.teco.ventago.features.reports.*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`

## Review Notes
- ITBMS en Ventas now shows the requested fiscal KPIs, including negative display for retentions and credit-note ITBMS, and derives gross/net values from rows when summary fields are missing.
- ITBMS en Ventas renders `ITBMS neto por periodo` as vertical bars and `Composicion fiscal del ITBMS` as a horizontal composition bar chart.
- ITBMS en Ventas detail rows are business-facing: document, type, date, client, taxable subtotal, gross ITBMS, retention, net ITBMS, exempt, non-taxed, and total.
- ITBMS en Gastos now shows the requested KPIs, a monthly total/ITBMS combo chart, top suppliers by ITBMS, fiscal breakdown donut, and period/supplier/document/subtotal/ITBMS/total details.
- Increased the desktop detail-column cap to preserve the full tax sales table.

# Customer Statement Search Fix TODO

## Plan
- [x] Change customer report RUC detail columns so customer ids are never used as RUC fallback.
- [x] Add a lightweight customer search option model for report filters.
- [x] Add customer search API flow through reports provider/repository/service using `/api/v1/customers/?page=0&size=8&name=...`.
- [x] Add debounced customer search and selection state to `ReportDefinitionViewModel`.
- [x] Replace the `customer_id` free-text field in Estado de Cuenta with a dynamic name/RUC dropdown that sets `customer_id`.
- [x] Run focused report tests plus Android/metadata compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests 'com.teco.ventago.features.reports.*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`

## Review Notes
- User correction: Estado de Cuenta must search customers by business-facing name/RUC, not ask the owner to type a technical customer id.
- Customer report RUC columns now only read RUC/tax fields and render empty when no RUC exists; they no longer fall back to customer id.
- Added `ReportCustomerOption` parsing for `/api/v1/customers/` search results and a focused parser test using the provided response shape.
- Reports provider/repository/service now expose customer search with page `0`, size `8`, name query, bearer auth, and `X-Business-ID`.
- Estado de Cuenta filter now debounces customer name search, shows a dropdown of customer name and RUC, and writes the selected customer id to the report request before applying filters.

# Customer Reports UI TODO

## Plan
- [x] Add raw-summary KPI extraction for all six customer reports, including note suffixes where requested.
- [x] Add customer-report chart rules using preferred chart payloads, row fallbacks, and donut breakdown fallbacks.
- [x] Add curated details for Ventas por Cliente.
- [x] Add curated details for Estado de Cuenta.
- [x] Add curated details for Clientes con Saldo Pendiente.
- [x] Add curated details for Clientes Nuevos, Clientes Inactivos, and Ranking de Clientes.
- [x] Run focused report tests plus Android/metadata compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests 'com.teco.ventago.features.reports.*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`

## Review Notes
- User correction: customer reports need exact KPI/chart/detail mappings from web API response keys rather than generic summary and row rendering.
- Customer KPIs now read from raw `summary` keys, including nested `top_by_amount` and note suffixes like document count or top-customer total.
- Customer charts now prefer requested `charts.*` keys and fall back to rows, summary totals, aging buckets, or pending-balance rows depending on report.
- Ventas por Cliente, Estado de Cuenta, Clientes con Saldo Pendiente, Clientes Nuevos, Clientes Inactivos, and Ranking de Clientes now use curated business columns with defaults like `Consumidor Final`, `Cliente sin nombre`, `-`, and `Sin compra`.
- First verification caught a private helper reuse (`containsAny`) from the parser; replaced it with local list checks and reran successfully.

# Sales Reports Batch UI TODO

## Plan
- [x] Add curated indicators and detail rows for Sales by Payment Method.
- [x] Add curated indicators and detail rows for Sales by Salesman.
- [x] Add curated indicators, amount-based bars, and detail rows for Sales by Branch.
- [x] Add curated indicators, remove aging charts, and owner-facing detail rows for Cancellations and Credit Notes.
- [x] Add curated indicators, aging labels, and owner-facing document rows for Sales Pending to Charge.
- [x] Run focused report tests plus Android/metadata compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests 'com.teco.ventago.features.reports.*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`

## Review Notes
- User correction: continue report-by-report refinement for the remaining real-time sales reports, removing technical labels and preserving business-facing language.
- Sales by Payment Method now shows only total sold, total charged, main method, and digital-payment percent, with detail rows limited to method, transactions, sold, charged, pending, average ticket, and usage percent.
- Sales by Salesman now shows total sold, total charged, average ticket, and lead salesman, with user-level document/subtotal/ITBMS/sold/ticket rows.
- Sales by Branch now uses amount-based bars and shows total sold, lead branch code, total charged, and average ticket, with branch rows focused on branch, code, ITBMS, sold, and average ticket.
- Cancellations and Credit Notes now filters out aging charts, uses adjustment/canceled/credit-note/user/type indicators, and shows client, order, amount, reason, type, and executing user.
- Sales Pending to Charge now shows pending/overdue/partial/client/days indicators, friendly aging bucket labels, and document rows with client, issue date, due date, total, paid, and unpaid amount.

# Product Sales Report UI TODO

## Plan
- [x] Remove the report-detail `Volver a reportes` button because the app bar already owns back navigation.
- [x] Use green chart colors for both bar and donut chart rendering.
- [x] Add Product Sales metric rules for total sold, items sold, top item by sales amount, and top item by quantity.
- [x] Rename the Product Sales bar chart to `Top productos por ventas`.
- [x] Add Product Sales detail-column rules that hide technical IDs/category/type/duplicate labels and show product/service, sales percent, ITBMS, and total sales including ITBMS.
- [x] Run focused report tests plus Android/metadata compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests 'com.teco.ventago.features.reports.*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`

## Review Notes
- User correction: refine Product Sales report specifically and apply general chart/back-button cleanup to all reports where relevant.
- Removed the duplicate in-content back button from report detail; navigation stays in the app bar.
- All report bar charts now use the shared green chart color, and donut charts use green variants.
- Product Sales now curates four KPI cards and can derive row-based totals/top products if the summary payload omits them.
- Product Sales chart is titled `Top productos por ventas` and uses product names with total sales including ITBMS.
- Product Sales detail rows hide key/product id/category/type fields and show only product/service, items sold, % ventas, ITBMS, and total con ITBMS.

# Sales Summary Report UI TODO

## Plan
- [x] Remove real-time/export badges from report list cards and detail header.
- [x] Shift report buttons, chips, icons, and section titles to secondary theme colors.
- [x] Add Sales Summary display rules for KPI selection/order, ITBMS currency display, recent-first bars, Cobrado vs pendiente pie chart, and detail columns.
- [x] Add a Sales Summary action button that opens the existing Orders screen.
- [x] Replace visible `Anio` copy with `Año`.
- [x] Run focused report tests plus Android/metadata compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests 'com.teco.ventago.features.reports.*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`

## Review Notes
- User correction: refine reports report-by-report; Sales Summary needs curated KPIs, chart behavior, business detail fields, Orders navigation, and secondary color emphasis.
- Removed the `Tiempo real` and `Excel/PDF` badges from report cards and the report detail header.
- Report buttons, category chips, report icons, report titles, section titles, and pagination/export actions now use the secondary color family.
- Sales Summary now shows the requested six KPI cards only, formats generated ITBMS as money, uses green recent-first bars, replaces the previous pie chart with `Cobrado vs pendiente`, and limits detail rows to the business fields requested.
- Sales Summary detail includes `Ver órdenes`, wired to the existing Orders screen route.
- Replaced visible `Anio` labels with `Año`.

# Real-Time Reports Business UI TODO

## Plan
- [x] Remove developer-facing API/key/source metadata from report detail UI.
- [x] Render visible chart cards from backend chart datasets instead of textual chart metadata.
- [x] Show skeleton content during both first load and report refresh.
- [x] Add report-specific filter inputs from the web report contracts using business-facing labels and calendar pickers for dates.
- [x] Run focused report tests and Android/metadata compile verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests 'com.teco.ventago.features.reports.*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`

## Review Notes
- User correction: report detail still exposed technical metadata and did not render visible charts or full business filters.
- Removed endpoint/report-key/source metadata from the report detail UI and replaced technical fallback copy with owner-facing language.
- Chart cards now render explicit API chart points first, then derive visible charts from report rows or KPI values when the API response does not include a chart-ready payload.
- Report detail shows shimmer skeleton cards during initial load and refresh.
- Report-specific filter inputs are generated from the real-time report contract map using business labels, select/boolean pills, numeric/text fields, and the existing date picker.

# Real-Time Reports API Integration TODO

## Plan
- [x] Review the updated report guide API contracts and existing provider/repository/UI loading patterns.
- [x] Add real-time reports provider, repository, service, request/response models, and DI wiring.
- [x] Replace static report-definition detail rendering with live query state, filter application, pagination, skeleton loading, and export actions.
- [x] Add focused tests for request/query construction and response parsing.
- [x] Run focused and broad compile/test verification gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests 'com.teco.ventago.features.reports.*'`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`

## Review Notes
- User correction: the first reports pass only exposed static definitions. The detail screen must call `/api/v1/reports/real-time/<report_key>` and use backend `data` to render KPIs/rows/pagination.
- Keep async reports excluded from this scope; only the guide's real-time endpoints are implemented.
- Preserve Spanish visible copy, compact filter band semantics, skeletons on initial load, KPIs before charts/table, export button states, and paginated range controls.
- Added a generic real-time reports API layer for `/api/v1/reports/real-time/<report_key>` and export/download endpoints using the app's existing orders-host auth headers and token refresh path.
- Report detail now fetches live `summary`, `rows/items/timeline`, `charts`, and `pagination` data. First load uses skeletons; Apply/pagination/export use `LoadingSheet` feedback.
- Customer Statement intentionally waits for `customer_id` before calling the endpoint because the web contract marks it required.
- Custom date ranges are not exposed as free-text fields; date-range UI should use the existing calendar picker component in a follow-up.

# Real-Time Reports Home Section TODO

## Plan
- [x] Review architecture, code conventions, required skills, and the web report replication guide.
- [x] Add a real-time report catalog with the six web categories and only real-time report definitions.
- [x] Add report category and report definition UI screens using existing Compose/design-system patterns.
- [x] Add a Home Summary report card and navigation/authz wiring behind `reports:view` plus `real_time_reports` beta access.
- [x] Add focused catalog/authz tests and run compile/test verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.reports.RealTimeReportCatalogTest --tests com.teco.ventago.core.authz.AuthzEvaluatorTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`

## Review Notes
- Existing worktree already has unrelated modified auth/network/iOS/task files, including `HomeSummaryScreen.kt`; preserve those changes and keep this patch scoped to reports.
- The shared guide requires Spanish visible copy, six report categories, real-time-only inclusion, no invented charts, compact filter-band semantics, and export metadata aligned to web exceptions.
- Added `RealTimeReportCatalog` with 26 real-time reports across Ventas, Clientes, Impuestos, Gastos, Finanzas, and Operativos. Async reports from the guide (`1027`, `taxes`, `expense_auxiliary`, `dgi_anexos_72_94`) stay excluded.
- `HomeSummaryScreen` now renders a gated "Reportes en tiempo real" card when `AuthzEvaluator.canRoute(REPORTS_PAGE)` passes.
- Reports access now requires both `reports:view` and beta feature `real_time_reports`; `reports:execute` is also beta-gated for future execution/export wiring.
- Added `Reports` navigation graph with category browsing and report definition detail route. The current UI intentionally shows definitions, filters, KPIs, chart parity notes, table/actions, endpoint, and export metadata without inventing missing charts.
- Focused Android host tests and Android/metadata compile gates passed. The first test run caught a Compose padding overload issue, which was fixed before rerunning.


# iOS QR Scanner Crash TODO

## Plan
- [x] Review project architecture, code conventions, and existing scanner implementation.
- [x] Identify why opening the iOS scanner crashes Compose/Metal.
- [x] Patch the iOS camera session lifecycle with minimal UI impact.
- [x] Run focused iOS compile verification and record result.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosSimulatorArm64`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes
- Crash stack shows `AVCaptureSession.startRunning()` called from `PreviewHostView.attach()` during `UIKitView` insertion, while Compose is applying/drawing changes.
- Starting the capture session synchronously on the main thread can spin AVFoundation's runloop and re-enter Compose rendering, producing `Attempt to call MetalRedrawer.draw() recursively`.
- The current iOS scanner had the MLKit result path commented out, so the focused fix should avoid frame-processing startup and use native metadata scanning where possible.
- iOS `CameraCoordinator` now configures, starts, and stops `AVCaptureSession` on a private serial queue instead of the main Compose draw path.
- The iOS scanner now uses `AVCaptureMetadataOutput` for QR and common barcode formats, delivering results on the main queue and stopping after the first result.
- `PreviewHostView.attach()` no longer forces `layoutIfNeeded()` during interop insertion; it assigns the preview frame and lets UIKit layout normally.
- iOS simulator compile passed after correcting AVFoundation interop opt-in/nullability; metadata compile was successful but Gradle skipped the compile task as up-to-date.

# iOS Temporary UI Hide TODO

## Plan
- [x] Review relevant UI files and platform helper conventions.
- [x] Hide the Home invoicing folio purchase CTA on iOS without changing Android.
- [x] Hide the Home Summary invoicing folio purchase CTA on iOS without changing Android.
- [x] Hide the Settings "Pagos y cobros" link on iOS without changing Android.
- [x] Run focused compile verification and record result.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosSimulatorArm64`

## Review Notes
- Use the existing common `isIOS()` expect/actual helper for platform gating instead of adding a new abstraction.
- The folio purchase action is suppressed inside `InvoicingPlanCard`, so callers from both Home UI variants cannot leak the CTA on iOS.
- `HomeSummaryScreen` also passes `null` for `onBuyStamps` on iOS so the caller explicitly matches the temporary product requirement.
- Metadata and iOS simulator compile verification passed with existing unrelated commonization, Skiko version, expect/actual beta, and deprecation warnings; the iOS simulator compile was rerun after the explicit `HomeSummaryScreen` caller guard.

# iOS Xcode 16 Linker Compatibility TODO

## Plan
- [x] Check selected Xcode/iOS SDK used by the local build.
- [ ] Reproduce the iOS framework link failure outside Xcode.
- [ ] Apply the smallest dependency/toolchain compatibility fix.
- [ ] Verify iOS framework link and Android compile still pass.

## Verification Gates
- [ ] `./gradlew --no-build-cache --no-configuration-cache :composeApp:linkDebugFrameworkIosArm64`
- [ ] `./gradlew --no-build-cache --no-configuration-cache :composeApp:linkDebugFrameworkIosSimulatorArm64`
- [ ] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Local selected Xcode is `16.4` with iOS SDK `18.5`.
- The linker error references `UIViewLayoutRegion` from Compose `CMPLayoutRegion.o`; this symbol requires the iOS 26 SDK.
- With Compose Multiplatform `1.11.x`, the toolchain fix is to select Xcode/iOS SDK 26. For local Xcode 16.4 builds, the dependency fallback is Compose Multiplatform `1.10.x`.

# iOS Compile Recovery TODO

## Plan
- [x] Reproduce/analyze reported iOS compile errors for `HomeViewModel`, `NotificationsViewModel`, and `InvoicePreviewModel`.
- [x] Replace iOS-incompatible `Dispatchers.IO` call sites in the reported ViewModels with injected KMP-safe dispatchers.
- [x] Replace JVM-only `toSortedMap()` usage in common invoice preview code with common-safe sorted entries.
- [x] Run iOS simulator compile verification and record result.
- [x] Run iOS arm64 compile verification and Android compile regression check.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosSimulatorArm64`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosArm64`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- Kotlin/Native reported `Dispatchers.IO` as internal at the new Home summary/financial profile refresh call sites and at the default parameter in `NotificationsViewModel`.
- `HomeViewModel` and `NotificationsViewModel` now receive a `CoroutineDispatcher` from Koin instead of resolving `Dispatchers.IO` directly in those failing common call sites.
- Koin uses `Dispatchers.Default` for those ViewModels because it is available across KMP targets.
- `InvoicePreviewModel` now sorts grouped tax rows and manual payments with `entries.sortedBy { it.key }`, avoiding JVM-only `toSortedMap()`.
- iOS simulator, iOS arm64, and Android debug Kotlin compile gates all pass with existing unrelated warnings.

# Loading Bottom Sheet Dismiss Guard TODO

## Plan
- [x] Review loading component behavior and current Material3 modal dismissal API.
- [x] Patch `LoadingScreen.kt` so loading sheets cannot dismiss from outside click/back press while `LOADING`.
- [x] Preserve success/error auto-dismiss and user dismissal semantics.
- [x] Run compile verification and record result.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`

## Review Notes
- Material3 scrim clicks can invoke a hide path before `onDismissRequest` returns, so a no-op `onDismissRequest` is not enough to make the sheet non-dismissible.
- `LoadingSheet` and `LoadingBottomSheet` now pass `ModalBottomSheetProperties(shouldDismissOnClickOutside = false, shouldDismissOnBackPress = false)` while the state is `LOADING`.
- `sheetGesturesEnabled = false` is still kept for the loading state to block drag dismissal.
- `SUCCESS` and `ERROR` remain dismissible so existing completion animations and `hideLoading()` callbacks keep working.
- Compile verification passed with existing unrelated warnings about cinterop commonization, compile SDK 37 support in AGP 9.1.0, expect/actual beta, and existing deprecations.

# Financial Profile Home Config Summary TODO

## Plan
- [x] Trace `FinancialProfileService` usages from app bootstrap, Home, and POS payment flow.
- [x] Identify why Home does not reliably trigger `config-summary`.
- [x] Patch Home/business initialization and financial profile refresh semantics.
- [x] Add focused service coverage and run verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.financialProfile.FinancialProfileServiceTest :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`

## Review Notes
- POS loads `config-summary` later because `PosViewModel.onPaymentScreenVisible()` explicitly calls `financialProfileService.refresh(businessId)` when the profile has not loaded.
- Home observed `FinancialProfileService` but only initialized `HomeSummaryService`; it did not establish the financial profile business context when the active business became available.
- `FinancialProfileService.setBusiness(refresh = true)` returned early when `currentBusinessId` and `state` were already populated, so an explicit refresh could be skipped if cache/state existed.
- Home now calls `financialProfileService.setBusiness(businessId, refresh = true)` when the active business changes, alongside `HomeSummaryService`.
- `FinancialProfileService` now only publishes cached profiles matching the requested business id and honors explicit refresh requests even when state already exists.
- Focused service tests and Android compile verification passed.

# Loading Bottom Sheet Visibility TODO

## Plan
- [x] Review loading component usage and the category activation flow.
- [x] Identify why `LoadingBottomSheetState` does not render.
- [x] Patch loading sheet state creation and dismissal behavior.
- [x] Run compile verification and record result.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`

## Review Notes
- Root cause: loading sheets were using `rememberModalBottomSheetState(confirmValueChange = { false })`; with current Material3 this blocks the hidden-to-visible transition, so the bottom sheet never opens.
- `CategoriesManageScreen` is one affected example during category activation.
- Replaced the broken state factory with `rememberModalBottomSheetState(skipPartiallyExpanded = true)` for loading sheet instances.
- `LoadingSheet` and `LoadingBottomSheet` now ignore dismiss requests and disable gestures while the state is `LOADING`, but allow dismissal after `SUCCESS` or `ERROR` so completion animations can hide the sheet.
- Compile verification passed with existing unrelated warnings about cinterop commonization, AGP compile SDK 37 support, expect/actual beta, and existing deprecations.

# AGP 9.1.0 KMP Module Split TODO

## Plan
- [x] Review AGP 9/KMP migration requirements and current single-module Android application layout.
- [x] Update the Gradle wrapper to an AGP 9.1-compatible version.
- [x] Add a dedicated Android application module and move app-owned Android files into it.
- [x] Convert `composeApp` from an Android application target to an Android-KMP shared library target.
- [x] Run Gradle verification gates and fix migration breakages.
- [x] Record final verification result and lessons.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:assembleDebug`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:testAndroidHostTest :androidApp:testDebugUnitTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:validateReleaseOrdersBasePath`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:bundleRelease --dry-run`

## Review Notes
- AGP 9 requires KMP projects to use the Android-KMP library plugin for shared Android targets instead of applying the Android application plugin in the KMP module.
- `androidApp` now owns the Android application plugin, app manifest, app resources, `google-services.json`, Firebase/Crashlytics plugins, and release AAB URL guard.
- `composeApp` remains the shared KMP module and keeps Room/KSP, shared Android actuals, Epson native libraries, and common/iOS targets.
- `:composeApp:testAndroidHostTest` replaces the old Android unit-test gate for the shared Android-KMP target; `:androidApp:testDebugUnitTest` currently has no sources.
- `:androidApp:assembleDebug` passed, including manifest merge, Google services, dexing, and native library packaging.
- `:androidApp:bundleRelease --dry-run` shows the release URL guard is wired before release bundle work, and direct validation passed with the production URL.
- Remaining non-blocking warnings: cinterop commonization is disabled, AGP 9.1.0 is only tested up to compile SDK 36.1 while this project uses SDK 37, Compose dependency accessors are deprecated, and existing Kotlin/Compose API deprecations remain.

# Android Gradle Plugin 8.13.2 TODO

## Plan
- [x] Verify the highest AGP upgrade that fits the current KMP/application module structure.
- [x] Update AGP from 8.11.2 to 8.13.2.
- [x] Run Android compile verification.
- [x] Record the AGP 9 migration constraint.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest`

## Review Notes
- Kotlin Multiplatform 2.4.0 supports AGP 8.5.2 through 9.1.0, but AGP 9.0+ is not an in-place version bump for this repo because `composeApp` applies both `org.jetbrains.kotlin.multiplatform` and `com.android.application`.
- Kotlin/Android docs require splitting the Android application into a separate module before using AGP 9.0+ with KMP.
- AGP 8.13.2 is the latest stable 8.x line available in Google Maven and preserves the current single-module Android application layout.
- Compile and debug unit-test verification passed.
- The compile SDK 37 warning remains because AGP 8.13.2 is tested up to API 36.1; AGP 9.1.1+ supports API 37, but that is outside the Kotlin 2.4.0 compatibility range provided and requires the AGP 9 KMP module split.

# Android Target SDK 37 TODO

## Plan
- [x] Update the Android target SDK catalog value from 36 to 37.
- [x] Run Android compile verification.
- [x] Record verification result.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileDebugKotlinAndroid`

## Review Notes
- This task changed only `android-targetSdk`; the current catalog already has `android-compileSdk = "37"`.
- Compile verification passed.
- AGP now uses `8.13.2`; it still warns that SDK 37.0 is newer than the compile SDK level it has been tested with, but no build failure occurred.

# Kotlin 2.4.0 KMP Upgrade TODO

## Plan
- [x] Review project architecture, code conventions, current version catalog, and baseline worktree state.
- [x] Verify Kotlin 2.4.0-compatible dependency and plugin versions from primary sources.
- [x] Update the version catalog/build configuration with a minimal compatible toolchain set.
- [x] Run Gradle verification gates and fix compile/deprecation breakages caused by the upgrade.
- [x] Record verification results and any upgrade lessons.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.orders.CxcOrderDetailsContractTest --tests com.teco.ventago.navigation.AuthzNavigationTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest`

## Review Notes
- Baseline worktree was clean before the upgrade.
- Project docs reviewed: `doc/architecture.md` and `doc/code-conventions.md`.
- Confirmed Kotlin 2.4.0 is stable; AGP remains inside Kotlin KMP compatibility ranges.
- Updated Gradle wrapper from 8.14.3 to 8.14.4 to satisfy Kotlin's next-version Gradle recommendation and remove the KGP Gradle-version warning.
- Updated Ktor to 3.x and switched Coil from `coil-network-ktor2` to `coil-network-ktor3`.
- Updated compatible catalog entries for Compose Multiplatform, Ktor, Room, KSP, SQLite, GitLive Firebase, Koin, Coil, AndroidX/Google/Firebase plugins, and kotlinx libraries.
- Fixed Ktor 3/serialization compile breakages by replacing removed `instanceOf<Boolean>()` checks with `jsonPrimitive.booleanOrNull`.
- Fixed Places SDK 5.x compile breakage by replacing removed `Place.Field.ADDRESS`/`LAT_LNG` with `FORMATTED_ADDRESS`/`LOCATION`.
- Fixed test-gate regressions by aligning receiver-phone placeholder filtering and keeping the Summary bottom-nav item owner-main only while preserving route authorization separately.
- Verification passed with remaining non-blocking warnings: disabled cinterop commonization, expect/actual beta warnings, Compose dependency accessor deprecations, Places Autocomplete deprecations, Material/BackHandler/Divider deprecations, and kotlinx-datetime compat deprecations.

# POS Invoice Preview Retention TODO

## Plan
- [x] Extend the invoice preview model/builder with a retention row derived from selected retention code/rate and ITBMS amount.
- [x] Render the retention message and amount under the payment form in `InvoicePreviewScreen`.
- [x] Render the same retention amount in the totals breakdown before the final total.
- [x] Add focused regression coverage and run targeted verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.pos.InvoicePreviewBuilderTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `InvoicePreviewBuilder` now derives an optional retention row from the selected retention catalog entry and applies the configured rate over the computed ITBMS amount.
- `InvoicePreviewScreen` renders the retention block directly under `Forma de Pago`, with the retention object/message on the left and formatted retention amount on the right.
- Added a regression test for the 50% state retention case where `$0.60` ITBMS produces `$0.30` retention, and verified the default preview does not render a retention row.
- Correction: totals breakdown now also lists `Retención` before the final `Total`, and the regression test confirms the invoice total remains unchanged.

# Production AAB Orders URL Guard TODO

## Plan
- [x] Add a release-bundle sanity check that reads the active `ReleaseConfigs.ordersBasePath` value from `Platform.kt`.
- [x] Fail Android AAB build tasks when the release orders URL is anything other than `https://invoice-vg.tecodigi.com`.
- [x] Verify the guard passes with the production URL and fails with a temporary local-IP value.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:validateReleaseOrdersBasePath`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:bundleRelease --dry-run`
- [x] Temporary local-IP negative check with `./gradlew --no-build-cache --no-configuration-cache :composeApp:validateReleaseOrdersBasePath`
- [x] `./gradlew --configuration-cache :composeApp:validateReleaseOrdersBasePath`
- [x] Repeated `./gradlew --configuration-cache :composeApp:validateReleaseOrdersBasePath` to verify cache reuse.
- [x] `./gradlew --configuration-cache :composeApp:bundleRelease --dry-run`

## Review Notes
- Added `validateReleaseOrdersBasePath` in `composeApp/build.gradle.kts`; it parses only the uncommented `ReleaseConfigs.ordersBasePath` constant and requires `https://invoice-vg.tecodigi.com`.
- Hooked the guard into `preReleaseBuild` and release bundle tasks, so `:composeApp:bundleRelease` runs the check before AAB packaging work.
- Positive validation passed with the production URL. A temporary `http://192.168.0.3:5001` release URL failed with `Release AAB blocked... It looks like a local development URL`, then `Platform.kt` was restored to production and the positive validation passed again.
- Correction: converted the validation from a closure-based `DefaultTask` action to a typed `ValidateReleaseOrdersBasePathTask` with Gradle `RegularFileProperty` and `Property<String>` inputs so configuration cache can serialize the task state.
- Configuration-cache verification now stores and reuses successfully for the direct validation task; the temporary local-IP negative check also fails while reusing configuration cache. `bundleRelease --dry-run` stores configuration cache and keeps the guard before `preReleaseBuild`.

# Spanish Country Catalog TODO

## Plan
- [x] Translate the shared customer country catalog names from English to Spanish while preserving ISO codes and catalog size.
- [x] Reuse the shared country catalog from the POS add-customer flow instead of maintaining a separate English list.
- [x] Update focused country catalog assertions and run verification gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.customers.CustomerModelsAndOrdersRequestTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `CustomerCountries.options` now keeps the same 219 ISO country codes while displaying Spanish country names.
- POS add-customer now maps from `CustomerCountries.options`, removing the separate shorter English list from `AddCustomerViewModel`.
- Focused customer catalog/request tests and the KMP/Android compile gate passed with existing KSP version, cinterop commonization, expect/actual beta, and unrelated deprecation warnings.

# Orders List Customer Row TODO

## Plan
- [x] Add customer display name to order list rows using the shared `Order.displayCustomerName()` helper.
- [x] Move the payment method metadata to the trailing column below the order amount and remove the `Pago:` prefix.
- [x] Run the requested KMP/Android compile verification and document the result.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `OrderListItem` now shows the optional customer display name below the order number, keeps item count as muted metadata, and moves the raw payment method label under the amount on the trailing side.
- The compile verification passed with existing KSP version, cinterop commonization, expect/actual beta, and unrelated deprecation warnings.

# POS Invoice Warning Confirmation TODO

## Plan
- [x] Normalize invoice warning code/message fields across create-order, retry-invoice, and shared order models.
- [x] Add a shared create-confirmation invoice warning resolver and wire POS state/create flow to it without treating order creation as failed.
- [x] Update POS confirmation UIs and retry-invoice messaging/action gating to honor the new warning policy.
- [x] Add focused regression coverage and run the requested verification gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.orders.CreateOrderResponseParsingTest --tests com.teco.ventago.features.orders.InvoicePostCreateWarningPolicyTest --tests com.teco.ventago.features.orders.ui.order_details.viewmodel.OrdersDetailsViewModelCxcTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `CreateOrderResponse`, `RetryInvoiceResponse`, and `Order` now normalize `invoice_warning_code` / `invoiceWarningCode` and `invoice_warning_message` / `invoiceWarningMessage`, so both create and refresh payloads can drive the same UX logic.
- Added a shared `resolvePostCreateInvoiceWarning(...)` policy in invoicing domain, and `PosViewModel.createOrder()` now preserves order success while storing an explicit post-create warning state instead of coercing `invoiceStatus == NONE` into `FAILED`.
- Phone and legacy POS confirmation UIs now switch to an error-themed warning presentation when invoice generation is pending/manual-failure, keep the order summary/navigation available, and disable open/download/share together when policy says invoice access should be blocked.
- Retry invoice flow now keeps backend refresh as the primary truth source, falls back to the retry response warning message, and otherwise shows the generic pending-verification copy. The retry CTA is now limited to paid orders with `invoiceStatus` `NONE` or `PENDING` that are not cancelled.
- Focused parsing/policy/retry tests passed, and the compile verification passed with the existing workspace warnings about KSP version mismatch, disabled cinterop commonization, manifest replacement noise, expect/actual beta, and unrelated deprecations.

## Correction Notes
- [x] Hide the invoice download card entirely on `SuccessScreen` when invoice actions are not available instead of rendering it disabled after invoice-generation failure/warning states.

# POS Final Customer Cart Identity TODO

## Plan
- [x] Add a shared POS cart customer-display helper that derives either the saved customer or the typed final-customer identity without overloading the selected-customer domain state.
- [x] Update the cart customer section to render typed final-customer name/ID with an explicit `Consumidor final` declaration and keep edit/clear actions appropriate for that flow.
- [x] Add focused regression coverage for the display helper, run targeted verification, and document the result.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.pos.PosFinalCustomerValidationTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Root cause: `CartOrganism` only rendered `uiState.customer`, while typed final-customer identity stayed isolated in `finalName` / `finalIdNumber` / `finalEmail`, so the cart collapsed back to the generic `Consumidor Final` placeholder.
- Added a shared `cartCustomerDisplay()` projection in `PosState` so cart UI can consume one derived display model for both saved and typed final customers without mutating the actual selected-customer state.
- The cart now shows the typed final-customer name plus `Tipo: Consumidor final` and the corresponding identification label (`Cédula`, `Pasaporte`, or `Identificación extranjera`), with edit routing back to `POSScreen` and clear resetting only the typed final-customer fields.
- Focused POS validation/display tests and the KMP/Android compile gates passed with the existing workspace warnings about KSP version, cinterop commonization, expect/actual beta, and unrelated deprecations.

# Add Item Optional Image Layout TODO

## Plan
- [x] Move the optional image picker out of the first required card and into the "Identificación y control" card.
- [x] Default the personalized-product "Guardar producto" option to unchecked.
- [x] Run a targeted compile gate and document the result.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- `AddItemScreen` now keeps the required "Información básica" card focused on mandatory fields, while the optional product image picker renders inside the collapsible "Identificación y control" card.
- Personalized-product mode now starts with `Guardar producto` unchecked through the shared item state default and the personalized-mode initializer, so the checkbox does not restore to checked when entering that flow.
- The targeted KMP/Android compile gate passed with the existing workspace warnings about KSP version, cinterop commonization, FileProvider manifest replacement, expect/actual beta, and unrelated deprecations.

# Order Details Phone Placeholder TODO

## Plan
- [x] Treat `"0000"` the same as empty for order customer/receiver phone resolution.
- [x] Keep the change in the shared order phone helper so Order Details and related actions inherit it automatically.
- [x] Add focused regression coverage and run the targeted order contract test.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.orders.CxcOrderDetailsContractTest`

## Review Notes
- `displayCustomerPhone()` now treats `"0000"` as a placeholder for both `customer.phone` and `receiver_phone`, so Order Details no longer renders or reuses that value for share/call actions.
- Added a focused order contract test that covers both branches: a `"0000"` customer phone now falls back to a valid receiver phone, and a `"0000"` receiver phone is hidden entirely when no valid customer phone exists.

# POS Final Customer Cedula Validation TODO

## Plan
- [x] Reuse the existing Panama cédula validator in the POS final-customer order flow instead of accepting any cedula string.
- [x] Surface the cedula validation error inline in the additional info card and block both "continuar" and `createOrder()` when the cedula is invalid.
- [x] Add focused regression coverage for the POS validation helper, run targeted verification, and record the lesson.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.pos.PosFinalCustomerValidationTest :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- POS final-customer identification now reuses the shared Panama cédula validator and normalization path, so `cedula` values are uppercased/trimmed and rejected when the format is invalid.
- The additional info card now renders an inline error for invalid cédulas, and the same validation blocks both the customer-step "Siguiente" action and the final `createOrder()` call as a double guard.
- Added focused regression coverage for cedula rejection, cedula normalization/acceptance, and non-cedula passthrough; the targeted test plus KMP/Android compile gate passed with the existing workspace warnings about KSP version, cinterop commonization, and deprecations.

# Orders Receiver Name TODO

## Plan
- [x] Extend the shared order model to decode top-level `receiver_name` / `receiver_phone` and resolve the display customer name for final-consumer orders.
- [x] Update order detail flows to use the shared display name/phone fallback instead of only `customer.name` and `customer.phone`.
- [x] Add focused regression coverage for the `get-orders` payload shape and run targeted verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.orders.CxcOrderDetailsContractTest`

## Review Notes
- `Order` now decodes top-level `receiver_name` and `receiver_phone`, and centralizes customer display fallback in shared helpers so final-consumer orders prefer a non-blank receiver name like `EMPRESA PRUEBA`.
- `OrderDetailsScreen` now uses the shared helpers for customer name/phone display and for the note-generation prefill, so the order detail flow no longer depends exclusively on `customer.name`.
- Added focused regression coverage for both final-consumer override behavior and the non-final-customer fallback; the targeted order contract test passed with the existing workspace warnings about KSP version, cinterop commonization, and deprecations.

# POS Passport Final Customer Payload TODO

## Plan
- [x] Add a focused serialization check for passport final-customer info with `country_code = "US"`.
- [x] Run the targeted order request test.
- [x] Document the verification result.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.orders.CreateOrderRequestTest`

## Review Notes
- Added `createOrderSerializesFinalPassportCustomerCountryCode`, which asserts `John Smith`, `passport`, `US123456789`, and `country_code = "US"` in `final_customer_info`.
- Targeted order request serialization test passed with existing Gradle warnings about KSP version and cinterop commonization.

# POS Final Customer Country Selector TODO

## Plan
- [x] Reuse the customer form country catalog in the POS final-customer additional information UI.
- [x] Show a country dropdown when the final customer identification type is passport or foreign tax ID, and keep it hidden for cédula.
- [x] Send the selected country code through `final_customer_info.country_code` in the order creation payload.
- [x] Add focused regression coverage and run targeted verification.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.orders.CreateOrderRequestTest --tests com.teco.ventago.features.customers.CustomerModelsAndOrdersRequestTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- POS final-customer additional information now uses a country dropdown backed by `CustomerCountries.options` when the ID type is `passport` or `foreing_taxid`.
- Selecting a foreign/passport ID type defaults the country code to `PA` if no country is selected yet; switching back to cédula clears the country code to avoid stale payload values.
- Order creation maps the selected country to `final_customer_info.country_code`; quote-building follows the renamed state field as well.
- Focused tests cover the shared country catalog and the requested final-customer payload shape with `country_code = "AD"`.

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

# Invoice Preview Bottom Note Card TODO

## Plan
- [x] Add optional bottom-note data to `InvoicePreview`.
- [x] Build the note from configured title/body only when the POS include switch is enabled.
- [x] Render the note as a new invoice-preview card when present.
- [x] Add focused preview-builder coverage and run verification gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.features.pos.InvoicePreviewBuilderTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileDebugKotlinAndroid`

## Review Notes
- Invoice preview now includes a new card for the configured bottom-note title/body only when the POS include switch is enabled.
- The preview body is converted from simple HTML into readable plain text for the card, preserving list-like line breaks.
- Focused builder tests cover include/exclude behavior and HTML normalization.

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

# Fingerprint / Session Header Audit

## Plan
- [x] Inspect shared Ktor client configuration and DI wiring.
- [x] Verify URL matching rules for adding the request header.
- [x] Search for request paths that bypass or override the shared client behavior.
- [x] Report whether fingerprint/session id is sent on all backend requests and call out exceptions.

## Verification Gates
- [x] `rg` audit for `FingerPrintService`, `X-SESSION-ID`, `HttpClient`, backend `Configs.*BasePath`, and direct top-level `client` imports.
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroid`

## Review Notes
- Backend requests through the Koin-injected `HttpClient` get `X-SESSION-ID` appended by the shared `HttpSend` interceptor when the request URL origin matches `Configs.serverBasePath` or `Configs.ordersBasePath`.
- The actual platform `FingerPrintService.getFingerPrint()` value is not used in request headers; the active backend identifier is the generated session id.
- Direct top-level `client` usage was found only in `BunnyCDNUtils`, targeting BunnyCDN storage, which is intentionally excluded by the backend URL matcher.

# Invoice Fingerprint Header TODO

## Plan
- [x] Add shared `fingerprint` header constant.
- [x] Bind `FingerPrintService` on iOS so the shared client interceptor can resolve it on both platforms.
- [x] Append the fingerprint header to every invoice backend request through the shared Ktor client interceptor.
- [x] Add focused matcher coverage and run verification gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroid`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :composeApp:compileKotlinIosSimulatorArm64`

## Review Notes
- Added `FINGERPRINT_HEADER_NAME = "fingerprint"` in shared network constants.
- The shared Ktor `HttpSend` interceptor now appends `fingerprint: FingerPrintService.getFingerPrint()` for invoice backend requests matched by `Configs.ordersBasePath`.
- iOS now binds `FingerPrintService` in `platformModule`, matching the existing Android binding so the shared interceptor resolves on both platforms.
- Added matcher regression coverage to avoid sending the fingerprint header to business-vg, BunnyCDN, or lookalike invoice hosts.
- Verification passed with existing project warnings only (cinterop commonization, Skiko version, expect/actual beta, deprecations).

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

# Panama Cedula Prefix Short Format TODO

## Plan
- [x] Adjust the shared Panama cédula validator so prefixed `E-` and `N-` formats accept the reported short-group valid shapes (`E-8-9856`, `N-8-9856`).
- [x] Add focused regression coverage for those valid inputs plus nearby invalid variants.
- [x] Run the targeted validator verification gate and document the result.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testDebugUnitTest --tests com.teco.ventago.utils.PanamaCedulaUtilsTest`

## Review Notes
- The shared Panama cédula validator now accepts the reported prefixed short formats `E-8-9856` and `N-8-9856` without broadening the prefix rules to previously invalid mid-length variants like `E-123-12345` or `N-123-1234`.
- Regression coverage now includes both new valid examples and nearby invalid shapes (`E-12345-1234`, `N-123456-1234`, `N-8-985`) so future regex edits keep the prefix branches narrow.
- The focused validator gate passed with the existing workspace warnings only (`ksp` version mismatch, disabled cinterop commonization, manifest replacement, expect/actual beta, and unrelated deprecations).

# iOS Order Details Formatting TODO

## Plan
- [x] Review architecture/code conventions and local task instructions.
- [x] Identify why Order Details header amounts/date are blank or fallback-only on iOS.
- [x] Patch iOS actual formatting utilities with minimal platform-specific changes.
- [x] Verify iOS compile and Android/common regression gates.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosSimulatorArm64`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosArm64`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin`
- [x] `xcrun swift /tmp/ventago_date_formatter_check.swift`

## Review Notes
- Root cause found: iOS `formatNumberToMoney` returns an empty string, so all UI paths using it render blank amounts.
- Root cause found: iOS `DateFormat.getFormattedDate` only accepts `yyyy-MM-dd'T'HH:mm:ss`, while backend order dates can include fractional seconds and timezone suffixes such as `Z` or `-05:00`.
- iOS `formatNumberToMoney` now uses `NSNumberFormatter` with currency style and two fraction digits, matching Android's platform formatter instead of returning blank.
- iOS `DateFormat.getFormattedDate` now tries the base order timestamp format plus fractional-second and timezone variants, using `en_US_POSIX` for input parsing and system timezone for output.
- Foundation runtime check matched `2026-03-27T12:00:00`, `2026-06-05T20:45:10Z`, `2026-06-05T16:41:30.212815-05:00`, and `2026-01-27T01:56:04.304173Z`; currency check returned `$18.19`.
- iOS simulator, iOS device, metadata, and Android compile gates passed with existing unrelated warnings only.

# iOS POS QR Crash TODO

## Plan
- [x] Review architecture/code conventions and local task instructions.
- [x] Locate the POS/payment-link QR generation path and platform actual implementation.
- [x] Patch iOS `generateQR` so Core Image receives `NSData` for `inputMessage`.
- [x] Run focused iOS compile verification.
- [x] Record outcome and lesson.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosSimulatorArm64`

## Review Notes
- Root cause found: iOS `generateQR` passes Kotlin `ByteArray` to `CIQRCodeGenerator.inputMessage`; Core Image requires `NSData` and throws `NSException` at runtime.
- iOS QR generation now creates `NSData` via `NSString.create(string = ...).dataUsingEncoding(NSUTF8StringEncoding)` before calling `filter.setValue(..., forKey = "inputMessage")`.
- The sibling iOS Core Image barcode helpers were updated from unsafe `String as NSString` casts to the same `NSString.create(...)` bridge.
- Focused iOS simulator compile passed with existing unrelated warnings only (`cinterop` commonization, Skiko mismatch, expect/actual beta, deprecations, and existing iOS cast/redundant-conversion warnings outside this fix).

# JWT Refresh Single-Flight TODO

## Plan
- [x] Review architecture and code conventions before changing auth/network code.
- [x] Trace provider-level 401 refresh calls for `config-summary` and business-by-id bootstrap flows.
- [x] Implement a single-flight refresh gate so concurrent 401 handlers share one refresh request.
- [x] Ensure refresh failure triggers only one sign-out while all waiting requests fail consistently.
- [x] Add focused concurrency coverage for the refresh gate.
- [x] Run metadata/test verification and record result.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.auth.domain.RefreshTokenSingleFlightTest :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosSimulatorArm64`

## Review Notes
- Existing worktree already had unrelated modified UI/iOS/task files before this auth fix.
- Root cause identified: each provider that received `AUTH_001`/401 called `authService.refreshToken(client)` independently, so concurrent protected requests could create a refresh storm.
- Minimal-impact approach: guard `AuthService.refreshToken(...)` centrally and add retry-once token capture to the two reported endpoints.
- `AuthService.refreshToken(...)` now uses a shared single-flight gate; concurrent callers that saw the same failed access token wait for the active refresh and skip their own network refresh after a new token is stored.
- Refresh parse failures, missing refresh tokens, and refresh HTTP/API failures now throw through the gate and trigger `signOut()` only once for the failure burst.
- `FinancialProfileProvider.getFinancialProfile` (`config-summary`) and `BusinessProvider.getBusinessById` now capture the token used for the failed request and retry only once.
- Focused Android host test passed after updating an existing fake `IAuthService` signature; iOS simulator compile also passed. `compileKotlinMetadata` was requested in the focused gate but Gradle skipped it.

# iOS App Icon Asset TODO

## Plan
- [x] Confirm the existing iOS asset catalog app icon location.
- [x] Generate a 1024x1024 opaque PNG from the provided logo image.
- [x] Replace the catalog's `app-icon-1024.png` without changing project wiring.
- [x] Verify generated icon dimensions, alpha channel, and changed-file scope.

## Verification Gates
- [x] `sips -g pixelWidth -g pixelHeight -g hasAlpha iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png`
- [x] `git status --short -- iosApp/iosApp/Assets.xcassets/AppIcon.appiconset tasks/todo.md tasks/lessons.md`

## Review Notes
- Existing catalog uses a single universal iOS `1024x1024` app icon at `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png`.
- Source logo is a square PNG but reports an alpha channel, so the replacement should be flattened to an opaque RGB PNG.

# iOS App Icon Replacement TODO

## Plan
- [x] Inspect the new 1024x1024 icon attachment.
- [x] Replace the existing iOS app icon asset with the new artwork.
- [x] Flatten the PNG alpha channel for iOS/App Store compatibility.
- [x] Verify icon dimensions, alpha channel, JSON validity, and changed-file scope.

## Verification Gates
- [x] `sips -g pixelWidth -g pixelHeight -g hasAlpha iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png`
- [x] `jq empty iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json`
- [x] `git status --short -- iosApp/iosApp/Assets.xcassets/AppIcon.appiconset tasks/todo.md tasks/lessons.md`

## Review Notes
- New source icon is already 1024x1024 but reports `hasAlpha: yes`.
- Existing iOS catalog still points to `app-icon-1024.png`, so only the PNG asset needs replacement.

# Settings Address Configure Button Hide TODO

## Plan
- [x] Review project architecture, code conventions, and Settings address UI.
- [x] Hide the nonworking address configuration button without changing the address display.
- [x] Run focused compile verification and record result.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`

## Review Notes
- Local `$disciplined-execution` and `$frontend-design` skill files were missing, so the written AGENTS policy is being applied directly.
- Existing worktree had unrelated modified files before this change; keep this patch limited to Settings address UI and task tracking.
- Removed the Settings business address `TextButtonS` that launched the nonworking autocomplete widget; the read-only address summary still renders.
- Metadata verification completed successfully; Gradle reported `:composeApp:compileKotlinMetadata` as skipped/up-to-date.

# Real-Time Reports Keyboard And Category State TODO

## Plan
- [x] Review real-time reports list/detail UI and navigation state handling.
- [x] Make report filters content IME-aware and dismiss keyboard on outside taps.
- [x] Preserve selected report category when returning from a report detail.
- [x] Run focused compile verification.
- [x] Record review notes and any prevention lesson.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- `ReportsScreen` now stores the selected category name with `rememberSaveable`, so returning from a report detail restores the selected category instead of falling back to Ventas.
- `ReportDefinitionScreen` now fills the viewport, applies `imePadding()`, remains vertically scrollable while the keyboard is open, and clears focus when tapping outside filter inputs.
- Metadata verification succeeded but Gradle skipped `:composeApp:compileKotlinMetadata` as up-to-date; Android debug Kotlin compilation passed and exercised the common reports UI change.

# Real-Time Reports Labels And Cash Flow Tabs TODO

## Plan
- [x] Fix requested report titles, accents, filter labels, and detail column labels.
- [x] Add shared friendly labels for backend enum/concept keys used by CxP aging and financial reports.
- [x] Split cash-flow detail into summary/detail/charges/payments/aging tabs.
- [x] Add per-document cash-flow detail actions that route through `OrdersScreenRoute(orderNumber)`.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :androidApp:compileDebugKotlin`

## Review Notes
- `Antiguedad de CxP` is now `Antigüedad de CxP`; related aging filter/detail labels use accents.
- Added friendly report labels for backend keys/statuses such as `revenue`, `costs`, `gross_profit`, `operating_expenses`, `operating_profit`, `operating_margin`, `uncategorized`, `sales`, `expenses`, `profit`, `net_cash_flow`, `no_due_date`, `not_paid`, and `paid`.
- Cash-flow detail now has tabs for Resumen, Detalle, Cobros, Pagos, and Antigüedad. The user requested “4 tabs” but listed 5 sections, so the listed sections were implemented.
- Cash-flow document rows render `Ver detalles`; when a document/order number is present, the action routes through `OrdersScreenRoute(orderNumber)` so the existing orders flow opens the detail screen.
- Metadata compile succeeded but Gradle skipped `:composeApp:compileKotlinMetadata` as up-to-date. Android debug Kotlin compilation passed after adding a local `containsAny` helper.

# iOS Push Permission Timing TODO

## Plan
- [x] Confirm the startup notification permission trigger and the post-order success trigger.
- [x] Remove the bootstrap permission request so iOS does not prompt on app open.
- [x] Preserve the existing successful order creation permission request path.
- [x] Run focused compile verification and record results.

## Verification Gates
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosSimulatorArm64`

## Review Notes
- Startup root cause was `AppViewModel.init -> getToken()`, where the iOS `actual fun getToken()` requested `UNUserNotificationCenter` authorization.
- `getToken()` on iOS now only reads existing notification settings and registers for remote notifications when authorization is already granted/provisional, so app open no longer shows the permission prompt.
- The successful-order path remains `SuccessScreen -> platformState.requestNotificationPermission()`, preserving the intended permission request timing after order creation.
- Metadata compile passed; iOS simulator ARM64 Kotlin compile passed after fixing nullable `UNNotificationSettings` handling.
# Archive iOS CocoaPods Config Error

## Plan

- [x] Inspect iOS/CocoaPods project metadata for stale absolute paths mentioned by Xcode.
- [x] Identify whether the durable fix is regeneration or a source-controlled project file correction.
- [x] Apply the smallest change needed to let Xcode archive resolve `composeApp.release.xcconfig`.
- [ ] Verify no stale `/Users/oscar/Documents/CODI/VentaGo` references remain in relevant iOS project files.

## Review

- Pending.

# iOS Folio Purchase CTA App Review TODO

## Plan

- [x] Audit folio/package/subscription purchase wording and external purchase entry points.
- [x] Remove or gate iOS UI so users cannot start folio/package purchases or contact purchase support from the iOS app.
- [x] Verify no iOS-rendered purchase CTA remains through focused source searches.
- [x] Run focused iOS compile verification and record results.

## Verification Gates

- [x] Focused source search for folio/package/purchase CTAs.
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosArm64`

## Review Notes

- `HomeScreen` now passes no folio purchase action on iOS; `InvoicingPlanCard` also keeps its existing internal `isIOS()` guard, so the `Comprar folios` button cannot render on iOS.
- `HomeSummaryScreen` already gated the folio WhatsApp action on iOS; scan confirmed the same pattern remains.
- `InvoiceLandingScreen` now hides the WhatsApp lead CTA and `Ver Precios` external website button on iOS.
- `InvoicingLandingScreen` now hides the starting price, WhatsApp CTA, and external information link on iOS.
- `SettingsScreen` already hides the payment-methods settings entry on iOS.
- Remaining purchase/payment wording found by scan is either inside Android-only runtime branches, merchant/customer payment flows, commented legacy subscription code, report/customer purchase terminology, or backend/model fields.
- iOS ARM64 Kotlin compilation passed. Existing warnings remain: cinterop commonization disabled, Skiko version mismatch, and unrelated deprecations/casts.

# ACH Onboarding Four-Step Refresh TODO

## Plan

- [x] Expand ACH onboarding to four setup steps plus a final success screen.
- [x] Add the ACH knowledge phase with validation explanation and four process cards.
- [x] Update the ACH costs step copy.
- [x] Update ACH next-step, footer, and save-success routing.
- [x] Run focused Kotlin metadata compilation.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes

- ACH onboarding now keeps the existing first step, inserts a new knowledge phase as step 2, moves costs to step 3, configuration to step 4, and success confirmation to step 5.
- The new knowledge phase explains comprobante validation and the four-step review flow.
- The cost step now shows the `$0.27` accepted-transaction fee, periodic platform billing note, and links-only compatibility alert.
- Metadata compilation passed. Existing project warnings remain unrelated.

# iOS Archive Kotlin Native Heap Failure TODO

## Plan

- [x] Review the attached archive failure log.
- [x] Inspect Gradle/Kotlin Native memory configuration.
- [x] Apply the smallest build configuration change for the release link heap failure.
- [x] Run focused iOS release framework verification.
- [x] Record verification result and any residual risk.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:linkReleaseFrameworkIosArm64`

## Review Notes

- Attached archive failure is `java.lang.OutOfMemoryError: Java heap space` during `:composeApp:linkReleaseFrameworkIosArm64`, inside Kotlin/Native release framework optimization/devirtualization.
- Raised Kotlin daemon heap from 3 GB to 6 GB and Gradle daemon heap from 4 GB to 8 GB in `gradle.properties`; the iOS release linker runs under the Gradle daemon and now starts with `-Xmx8192M`.
- Verification passed: `:composeApp:linkReleaseFrameworkIosArm64` completed successfully in 12m24s, producing the iOS arm64 release `ComposeApp.framework`.
- Non-blocking warnings remain from existing configuration: cinterop commonization disabled and a Skiko dependency version mismatch warning.

# iOS Archive Version TODO

## Plan

- [x] Find the iOS archive marketing version source.
- [x] Update iOS marketing version from `1.0` to `1.6.0`.
- [x] Verify Xcode build settings resolve `MARKETING_VERSION=1.6.0`.

## Verification Gates

- [x] `xcodebuild -project iosApp/iosApp.xcodeproj -target iosApp -configuration Release -showBuildSettings`

## Review Notes

- iOS archive marketing version is sourced from `iosApp/Configuration/Config.xcconfig`.
- Updated `MARKETING_VERSION` to `1.6.0`; left `CURRENT_PROJECT_VERSION=1` unchanged because that is the build number, not the user-facing app version.
- Xcode Release build settings now resolve `MARKETING_VERSION = 1.6.0`.

# iOS Bluetooth Purpose String TODO

## Plan

- [x] Review the App Store Connect warning and current iOS Info.plist purpose strings.
- [x] Add `NSBluetoothAlwaysUsageDescription` with a printer-focused user-facing purpose string.
- [x] Verify the plist is valid and contains the new key.

## Verification Gates

- [x] `plutil -lint iosApp/iosApp/Info.plist`
- [x] `plutil -p iosApp/iosApp/Info.plist | rg "NSBluetoothAlwaysUsageDescription"`

## Review Notes

- App Store delivery succeeded, but Apple flagged `ITMS-90683` for the next delivery because the app or an SDK references Bluetooth-sensitive APIs.
- Added `NSBluetoothAlwaysUsageDescription` to `iosApp/iosApp/Info.plist`: `VentaGo necesita acceso a Bluetooth para descubrir y conectarse a impresoras térmicas compatibles.`
- Plist syntax validation passed and PlistBuddy confirms the new key value.

# iOS Build Number 2 TODO

## Plan

- [x] Confirm current iOS marketing version and build number.
- [x] Update `CURRENT_PROJECT_VERSION` from `1` to `2`.
- [x] Verify Xcode Release build settings resolve version `1.6.0` and build `2`.

## Verification Gates

- [x] `xcodebuild -project iosApp/iosApp.xcodeproj -target iosApp -configuration Release -showBuildSettings`

## Review Notes

- Updated `CURRENT_PROJECT_VERSION` to `2` in `iosApp/Configuration/Config.xcconfig`.
- Xcode Release build settings now resolve `MARKETING_VERSION = 1.6.0` and `CURRENT_PROJECT_VERSION = 2`.

# Epson Ticket Web Parity TODO

## Plan

- [x] Audit current ticket parser and Android/iOS Epson renderers for paper, logo, QR, and authorization behavior.
- [x] Add shared print profile metadata for 56/57/58/80mm columns and dot widths.
- [x] Force authorization key/value blocks to full-width wrapped text.
- [x] Ignore backend QR size hints and resolve QR module size/x-position from the required paper rules.
- [x] Align logo/image canvas sizing and centering with the web Epson output on Android and iOS.
- [x] Add focused parser tests for paper profiles, authorization wrapping, and QR sizing/position.
- [x] Run focused printer tests and common compile verification.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:testAndroidHostTest --tests com.teco.ventago.features.printers.TicketLayoutParserTest`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`
- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileKotlinIosSimulatorArm64`

## Review Notes

- Added shared ticket paper profiles: 56/57mm use 34 chars and 420 dots, 58mm uses 32 chars and 384 dots, and 80mm uses 48 chars and 576 dots.
- Authorization key/value blocks now print as normal left-aligned full-width text (`Autorización: value`) and ignore right-column/totals alignment hints.
- QR blocks now ignore backend size/width hints for print, resolve module width and explicit dot x-position from paper profile and QR version rules, and preserve backend error correction only for L/M/Q/H.
- Android and iOS Epson engines now print QR from left alignment with explicit horizontal positioning and reset position afterward.
- Logo/image blocks now render into a white fixed-width raster canvas and center the image inside that canvas; iOS now applies the same paper-aware image sizing Android already had.
- Focused parser tests, common metadata compilation, and iOS simulator Kotlin compilation passed. Existing project warnings remain unrelated.

# POS Credit Note Success Copy TODO

## Plan

- [x] Locate the POS success hero title logic.
- [x] Use existing POS document type state to detect credit notes.
- [x] Change only the completed-title copy for credit note orders.
- [x] Run focused common metadata compilation.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes

- Credit note POS success titles now show `¡Nota de crédito completada!` for document types `04` and `06`.
- Standard invoices keep `¡Factura completada!`; warning, payment-link, and failure copy remains unchanged.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# POS Yappy Pending QR Conflict TODO

## Plan

- [x] Trace POS Yappy onsite order creation error handling and navigation.
- [x] Model the backend pending-transaction error so the ViewModel can preserve branch and billing point.
- [x] Show a focused confirmation dialog when the pending QR conflict happens.
- [x] On keep-active, return to payment selection so the cashier can choose another method.
- [x] On cancel, call the existing cancel-pending endpoint, then retry the original Yappy onsite order creation.
- [x] Run focused compile/tests or the nearest reliable verification gate.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes

- Added a typed `YappyOnsitePendingTransactionDto` and `YappyOnsitePendingTransactionExistsException` for `yappy_onsite_pending_transaction_exists`.
- `OrdersRepository.createOrder` now handles the pending-QR error before decoding order data and treats any `success:false` create-order response as an error.
- POS state preserves the pending QR branch/billing point from the backend response for cancel-pending.
- The payment screen now shows the requested Spanish dialog copy.
- Choosing `Mantener QR activo` closes the dialog and returns the payment flow to manual/installments so the cashier can use another payment method.
- Choosing `Cancelar QR pendiente` calls `cancelPendingYappyOnsiteTransaction` with reason `cashier_cancelled_pending_qr`, then retries Yappy onsite order creation only when cancellation succeeds.
- Common metadata compilation passed. Existing warnings remain unrelated.

# Yappy QR Change Payment Crash TODO

## Plan

- [x] Inspect attached Android crash and identify the crashing Compose hierarchy.
- [x] Remove nested vertical scroll when showing the embedded replacement payment page from Yappy QR.
- [x] Run focused compile verification.
- [x] Record result and any lesson from the crash pattern.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes

- Crash was caused by rendering `PaymentScreenContent` inside `YappyOnsitePaymentScreen`'s outer `Column.verticalScroll`, while `PaymentScreenContent` owns its own scrollable layout.
- The pending-payment replacement branch now renders as a top-level full-height branch, outside the QR/status scroll container.
- The active QR/status states keep the existing scroll behavior.
- Common metadata compilation passed. Existing warnings remain unrelated.

# Yappy QR Change Payment Confirmation TODO

## Plan

- [x] Add a confirmation dialog before leaving an active Yappy QR to select another payment method.
- [x] On confirmation, release the active Yappy onsite pending intent through `/orders/{id}/payments/pending-intent/release` with `payment_method=yappy_onsite` and `reason=customer_selected_cash`.
- [x] Mark the pending source as already released so the replacement confirmation does not double-release it.
- [x] Keep cancel/dismiss behavior preserving the active QR.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes

- Added a confirmation dialog when the cashier taps `Seleccionar otro método de pago` on an active Yappy QR.
- Confirming the dialog calls the existing pending-intent release endpoint with `payment_method = "yappy_onsite"` and `reason = "customer_selected_cash"` before showing the replacement payment selector.
- Dismissing the dialog keeps the QR active and does not call the release endpoint.
- Added `pendingPaymentChangeSourceReleased` so subsequent replacement confirmation does not release the same Yappy pending intent again.
- Common metadata compilation passed. Existing warnings remain unrelated.

# Yappy QR Stop Polling On Release TODO

## Plan

- [x] Trace where the Yappy transaction polling loop is scheduled and where pending-intent release begins.
- [x] Add a state guard that suppresses transaction polling before calling pending-intent release.
- [x] Reset the guard only when a new Yappy onsite QR/session is created or POS state is reset.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes

- `YappyOnsitePaymentScreen` now stops scheduling the Yappy transaction polling loop once `yappyOnsitePollingSuppressed` is set.
- `releasePendingPaymentChangeIntent` now calls `stopYappyOnsitePollingForRelease()` before the pending-intent release endpoint when the source method is Yappy onsite.
- The ViewModel cancels the active Yappy transaction polling job and rejects delayed poll calls while release is in progress.
- The suppression flag resets on new POS sale state and when a new Yappy onsite QR/session is created.
- Common metadata compilation passed. Existing warnings remain unrelated.

# Payment Link Stop Polling On Release TODO

## Plan

- [x] Trace payment-link "select other payment method" CTA and current selector exit cancellation path.
- [x] Change payment-link CTA to stop polling and call pending-intent release before opening the replacement selector.
- [x] Reuse the released-source guard so replacement confirmation does not call release twice.
- [x] Verify exiting the selector still prompts and cancels the order through the cancel endpoint.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes

- Payment-link `Seleccionar otro método de pago` now calls a release-first ViewModel action instead of opening the selector directly.
- The new action stops payment-link polling, releases the pending payment link intent with `payment_method = "payment_link"` and `reason = "customer_selected_cash"`, then opens the replacement selector only on success.
- `pendingPaymentChangeSourceReleased` remains true after the pre-release, so confirming manual/Yappy replacement does not release the same payment link again.
- The selector exit flow was verified: back/home/new-sale attempts show the cancel-order dialog, and confirmation calls `POST /api/v1/orders/cancel` through `orderService.cancelOrder` with reason `customer_abandoned_payment_method_change`.
- Common metadata compilation passed. Existing warnings remain unrelated.

# POS Config Summary Excess Calls TODO

## Plan

- [x] Trace all POS calls that can hit `/api/v1/business/config-summary` during Yappy onsite replacement flow.
- [x] Identify why opening the replacement payment selector re-runs config warmup.
- [x] Prevent embedded replacement selectors from refreshing config summary.
- [x] Keep normal payment screen config warmup, but make it run once per screen mount instead of on config-state mutations.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes

- `PaymentScreenContent` was calling `onPaymentScreenVisible()` from a `LaunchedEffect` keyed by payment config state, so config-state changes could re-trigger payment config warmup.
- The Yappy/payment-link replacement selectors reuse `PaymentScreenContent`; opening them after pending-intent release should not refresh `/api/v1/business/config-summary`.
- `PaymentScreenContent` now runs `onPaymentScreenVisible()` only for the normal payment screen and only once per mount.
- Replacement selectors now use existing POS config state and do not initiate config-summary refresh.
- The `.165` Chrome log entries with `jwt_request_missing_fingerprint` / `invalid_token` are separate from the app `.124` POS flow and were not caused by this app-side replacement selector path.
- Common metadata compilation passed. Existing warnings remain unrelated.

# Order Details Status History Sheet TODO

## Plan

- [x] Wire the order details status badge click to local sheet state.
- [x] Render a bottom-sheet timeline from `order.orderHistories` with status, note, actor, and date.
- [x] Keep the empty-history state readable without adding a new API request.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes

- Tapping the status badge on order details now opens a bottom sheet backed by `order.orderHistories` from the existing order payload.
- The sheet shows each history entry's status label, note, changed-by user, and formatted timestamp in a scrollable timeline.
- Empty histories render a readable empty state and no new API request was added.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# Order Details Compact Number And Location TODO

## Plan

- [x] Parse the internal order number into branch code, billing-point code, and sequence number.
- [x] Show the compact sequence number in the order details header.
- [x] Collect branch data in the order details ViewModel and resolve branch/billing-point display names.
- [x] Add a detail card for branch and billing point using the existing card layout style.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes

- Order details now shows the compact order sequence, e.g. `#862`, instead of the full `ORD-business-branch-point-sequence` internal number.
- Added a card below the header with resolved branch and billing-point names from `BranchService.observe()`.
- The parser remains defensive and falls back to branch/billing-point codes or unavailable labels when names are not yet loaded.
- The status history sheet also uses the compact order number for consistency.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# Orders Screen Filter And Label Cleanup TODO

## Plan

- [x] Move filter sheet visibility into `OrdersState` so top-bar actions can open it.
- [x] Move payment-status filter chips from the main list header into the filter bottom sheet.
- [x] Move the filter button and active-filter badge into `OrdersScreenActions`.
- [x] Remove the orders/quotes tab UI from `OrdersScreen`.
- [x] Update Spanish orders labels used by this screen from `Pedidos` to `Órdenes`.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes

- Payment-status chips now live inside the filters bottom sheet and update the same `paymentStatusFilter` applied by the sheet action.
- The top app bar actions now include the filter icon with the active-filter count badge beside add and QR scanner.
- Removed the embedded orders/quotes tab UI and quotes list rendering from `OrdersScreen`.
- Spanish `orders` empty-state labels now use `Órdenes`.
- Common metadata compilation passed. Existing project warnings remain unrelated.

# Orders Screen Order Number Search TODO

## Plan

- [x] Collect branch/billing-point data in `OrdersViewModel` for the search sheet.
- [x] Add search-sheet visibility state and top-bar search icon before the filter icon.
- [x] Build a bottom sheet with conditional branch and billing-point dropdowns plus order sequence input.
- [x] Generate the full internal order number from business id, selected branch, selected billing point, and padded sequence.
- [x] Submit to the existing `findOrderByOrderNumber` flow and open order details on success.
- [x] Run focused compile verification and record results.

## Verification Gates

- [x] `./gradlew --no-build-cache --no-configuration-cache :composeApp:compileCommonMainKotlinMetadata`

## Review Notes

- Added a top-bar search icon before the filter icon on the orders screen.
- Added an order-number search bottom sheet with conditional branch and billing-point selectors plus numeric sequence input.
- The sheet builds full internal numbers like `ORD-4-0000-865-0000000870` from the selected branch, selected billing point, current business id, and zero-padded user input.
- Search submits through the existing `findOrderByOrderNumber` flow and opens order details when found.
- Common metadata compilation passed. Existing project warnings remain unrelated.
