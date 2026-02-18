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
