# VentaGo KMP Replication Plan: Payments Links + Payment Settings + In-App Notifications

Last updated: 2026-04-17 (Iteration 2 completed)
Owner: Codex agent + Oscar
Source of truth: user-provided `live..payments` replication spec

## Iteration Matrix

- [x] Iteration 0: Planning + Baseline Freeze
- [x] Iteration 1: Platform Foundation (contracts/authz/data layer)
- [x] Iteration 2: Orders creation + Order detail payment link/invoice retry UX
- [ ] Iteration 3: ACH in order detail + Dedicated ACH review screen
- [ ] Iteration 4: Settings entry + Payment methods redesign + ACH/fees
- [ ] Iteration 5: Global in-app notifications + Topbar UX
- [ ] Iteration 6: Analytics, hardening, parity sweep

## Iteration 0 - Planning + Baseline Freeze

### Deliverables
- Persist full plan and acceptance checklist for multi-iteration execution.
- Keep rollback notes and open blockers centralized.

### Acceptance checklist
- [ ] API contract parity checklist
- [ ] Loading UX parity checklist
- [ ] Error mapping parity checklist
- [ ] Authz parity checklist
- [ ] Analytics parity checklist

### Rollback notes
- All changes are additive and feature-gated where possible.
- If a regression appears in a later iteration, rollback by reverting iteration-specific commits while preserving model additions that are backward-compatible.

## Iteration 1 - Platform Foundation (Contracts, Authz, Data Layer)

### Scope
- Extend authz scopes/actions/routes for payment-link and ACH permissions.
- Add typed request/response/domain models for:
  - Payments links + ACH review endpoints
  - Payment settings ACH/fees/auto-invoice endpoints
  - In-app notifications endpoints
- Add providers/repositories/services methods with logger + rethrow discipline.
- Add shared normalizers:
  - Payment-link source resolution (`payment_links[]` -> `payment_link` -> `links[]`)
  - ACH detail alias normalization
- Add error code mapping with Spanish messages for:
  - `O_RP_001`, `O_RP_002`, `O_RP_004`, `O_RP_005`
  - `PAY_001`, `PAY_002`, `PAY_PP_001`
  - `INV_001`, `INV_002`

### Exit criteria
- [x] Compile passes on shared metadata + Android.
- [x] Foundation APIs callable from domain services.
- [x] No manual JSON building in ViewModels.

## Iteration 2 - Orders Creation + Order Detail

### Progress
- Completed:
  - POS payment-link tab preflight now refreshes config summary before allowing link mode.
  - `Nuevo` badge lifecycle is persisted and tab-attempt marks it as seen.
  - POS redirect to payment settings now stores a full payment-step snapshot checkpoint for resume.
  - Order detail payment-link matrix implemented (`Generar`, `Copiar`, `Compartir`) using resolved link priority and status classification.
  - Generate-link modal implemented with amount/expiry validation and create-link API call.
  - Order detail first-load now renders shimmer skeleton parity, including payment-link action surfaces.
  - Retry-invoice flow now differentiates issued-success dialog vs pending-warning dialog.
  - Retry-invoice success dialog now includes visual parity for `Total facturado` + `CUFE` structure and action row (`Descargar factura` / `Compartir factura`).

### Scope
- Re-enable payment link option in POS step 3 under authz+beta gate.
- Add `Nuevo` badge lifecycle per business in local storage.
- Add preflight config summary check and redirect dialog to payment settings.
- Add order detail button state matrix and generate/copy/share/retry behavior.
- Add generate-link modal with expiry presets + validation.

### Exit criteria
- [x] User-triggered API actions show `LoadingSheet` states.
- [x] First-load states show shimmer/skeleton.
- [x] Payment-step checkpoint restore uses full state snapshot (not flag-only).
- [x] Invoice success modal renders total/CUFE parity details.

## Iteration 3 - ACH Order Detail + Dedicated ACH Review Screen

### Scope
- ACH intent inline cards in order detail with lazy detail loading, dedupe, status chips.
- Approve/reject/proof actions from order detail with invalidation and refresh.
- Dedicated ACH review route/screen with hero, findings, proof, timeline, modals.
- Proof preview pipeline (blob-first, URL fallback) + download policy.

## Iteration 4 - Settings Entry + Payment Methods + ACH/Fees

### Scope
- Settings entry `Pagos y cobros` with new badge and feature gate.
- Payment methods shell redesign (onboarding + configured mode).
- ACH account read/configure/disable.
- Auto invoice switch persistence.
- Fee summary + transactions/batches paginated tables.

## Iteration 5 - In-App Notifications + Topbar UX

### Scope
- Global notification module with 30s polling and visibility refresh.
- Local storage cache for fast first paint, pagination dedupe, unread count cap `99+`.
- Topbar bell dropdown with list, archive per row, archive all, load more.

## Iteration 6 - Analytics + Hardening + Parity Sweep

### Scope
- Notification analytics events + required params.
- Payment/settings analytics parity for onboarding/config/payment-link actions.
- Spanish copy parity in `values-es`.
- Final visual polish and regression sweep for list/detail consistency.

## Open blockers / assumptions

- Local branches `live` and `payments` are unavailable in this workspace; implementation follows provided spec text only.
- Existing `LocalStorage`, `AnalyticsService`, `FinancialProfileService`, and DI setup are reused.
- Full parity spans multiple commits/iterations; each iteration must close its compile+verification gates before moving on.
