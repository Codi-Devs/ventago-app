# VentaGo Payments + Settings + In-App Notifications -> Kotlin Multiplatform Replication Spec

This document captures the branch diff `live..payments` for the requested scope:
- payment links
- payments/settings
- in-app notifications

Diff basis:
- `git diff live..payments`

Primary source files used:
- `src/modules/orders/**`
- `src/modules/settings/**`
- `src/modules/common/**`
- `src/saas/orders/**`
- `src/saas/settings/payment-methods.html`
- `src/saas/payments/ach/index.html`
- `src/saas/partials/topbar.html`
- `src/assets/scss/custom/components/_payment-methods.scss`
- `src/assets/scss/custom/components/_modal.scss`
- `src/assets/scss/custom/structure/_topbar.scss`

Notes:
- All UI copy remains Spanish.
- This spec is behavior-first for KMP Compose replication, not JS parity.

## 1) API Contracts (new + modified)

Base behavior in web transport:
- Auth header: `Authorization: Bearer <token>`
- Business context header: `X-Business-ID: <businessId>`
- Normalized app envelope expected by frontend usage:

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

### 1.1 Orders / Payment Links / ACH

#### A) Create payment link (new)
- `POST /api/v1/payments/links`

Request:
```json
{
  "order_id": 12345,
  "amount": "35.50",
  "expire_in_minutes": 1440
}
```

Response fields consumed:
```json
{
  "success": true,
  "data": {
    "payment_link_url": "https://..."
  },
  "error": null
}
```

#### B) ACH payment detail by intent id (new)
- `GET /api/v1/payments/ach/payments/{payment_intent_id}`

Response accepted is flexible; web normalizes from many aliases. Minimum stable shape to provide:
```json
{
  "success": true,
  "data": {
    "payment_uid": "pi_abc123",
    "payment_status_str": "pending_review",
    "amount": 35.5,
    "currency_code": "USD",
    "reference": "TRX-001",
    "payment_date": "2026-04-10T10:00:00-05:00",
    "customer_name": "Cliente Demo",
    "customer_email": "cliente@demo.com",
    "order_number": "ORD-1022",
    "bank_name": "Banco General",
    "destination_account": "****1234",
    "proof_id": 777,
    "proof_file_url": "https://...",
    "proof_file_name": "comprobante.pdf",
    "proof_content_type": "application/pdf",
    "risk_score": 42,
    "risk_level": "medium",
    "decision_suggested": "approve",
    "latest_fraud": {},
    "latest_ocr": {},
    "timeline": []
  },
  "error": null
}
```

#### C) Approve ACH payment (new)
- `POST /api/v1/payments/ach/payments/{payment_intent_id}/approve`

Request:
```json
{}
```

Response:
```json
{
  "success": true,
  "data": {
    "status": "approved"
  },
  "error": null
}
```

#### D) Reject ACH payment (new)
- `POST /api/v1/payments/ach/payments/{payment_intent_id}/reject`

Request:
```json
{
  "reason_code": "fraud",
  "reason_text": "Comprobante de pago fraudulento"
}
```

Response:
```json
{
  "success": true,
  "data": {
    "status": "rejected"
  },
  "error": null
}
```

#### E) Download ACH proof file (new, binary)
- `GET /api/v1/payments/ach/payments/{payment_id}/proofs/{proof_id}/file`

Request headers used:
- `Authorization: Bearer <token>`
- `X-Business-ID: <businessId>`
- `Accept: */*`

Response:
- raw blob/file stream
- consumes `Content-Disposition` for filename and `Content-Type` for mime.

#### F) Retry invoice generation (new)
- `POST /api/v1/orders/{order_id}/retry-invoice`

Request:
```json
{}
```

Response fields consumed:
```json
{
  "success": true,
  "data": {
    "invoice_warning_message": "Facturación intermitente..."
  },
  "error": null
}
```

#### G) Modified order payload fields consumed by UI
Used in order detail/payment-link logic:
```json
{
  "payment_link": "https://...",
  "payment_links": [
    {
      "link": "https://...",
      "status": "active",
      "created_at": "2026-04-10T09:00:00Z"
    }
  ],
  "links": [
    {
      "url": "https://...",
      "status": "pending"
    }
  ],
  "external_uuid": "...",
  "payment_flow_type": "...",
  "order_payments": [
    {
      "id": 100,
      "payment_intent_id": "pi_abc123",
      "payment_status_str": "requires_action",
      "is_automatic": true
    }
  ]
}
```

### 1.2 Settings / Payment Methods

#### A) Payment config summary alias + extended response (modified)
- `GET /api/v1/business/config-summary`

Key response additions consumed:
```json
{
  "success": true,
  "data": {
    "payment_summary": {
      "onboarding_completed": true,
      "pending_charges": 2.14,
      "next_billing_date": "2026-04-25T00:00:00-05:00",
      "fee_billing": {
        "currency_code": "USD",
        "pending_due_amount": 1.07,
        "overdue_amount": 0,
        "accrued_current_period_amount": 0.54,
        "paid_amount": 5.35,
        "next_batch_generation_at": "2026-04-20T00:00:00-05:00",
        "next_due_at": "2026-04-25T00:00:00-05:00"
      },
      "payment_methods": {
        "yappy": {
          "visible": true,
          "linked_account": true
        },
        "paypal": {
          "visible": true,
          "linked_account": true,
          "email": "merchant@paypal.com"
        },
        "ach": {
          "visible": true,
          "configured": true,
          "enabled": true,
          "pending_review_count": 2,
          "account": {
            "bank_name": "Banco General",
            "bank_code": "BANCO_GENERAL",
            "account_type": "checking",
            "account_number_masked": "****1234",
            "account_holder_name": "Comercio Demo"
          }
        }
      },
      "linked_paypal_billing_agreement": true,
      "auto_invoice_on_payment_success": true
    }
  },
  "error": null
}
```

#### B) Onboard payments (modified request)
- `POST /api/v1/business/payments/onboard`

Request:
```json
{}
```

Response:
```json
{
  "success": true,
  "data": {
    "successful": true
  },
  "error": null
}
```

#### C) Link Yappy (modified request)
- `PUT /api/v1/business/payment-methods/yappy/link`

Request:
```json
{
  "yappy_merchant_id": "MID123",
  "yappy_domain": "https://tecodigi.com",
  "yappy_secret_key": "SECRET"
}
```

#### D) Unlink Yappy (modified request)
- `DELETE /api/v1/business/payment-methods/yappy/unlink`

Request:
```json
{}
```

#### E) Auto invoice toggle (new)
- `PUT /api/v1/business/payment-methods/auto-invoice`

Request:
```json
{
  "enabled": true
}
```

#### F) PayPal connect URL (new)
- `GET /api/v1/business/payment-methods/paypal/connect`

Response consumed:
```json
{
  "success": true,
  "data": "https://www.paypal.com/...",
  "error": null
}
```

#### G) PayPal unlink (new)
- `DELETE /api/v1/business/payment-methods/paypal/unlink`

Request:
```json
{}
```

#### H) PayPal billing agreement URL (new)
- `POST /api/v1/business/billing/paypal/billing-agreement/create`

Request:
```json
{}
```

Response consumed:
```json
{
  "success": true,
  "data": "https://www.paypal.com/...",
  "error": null
}
```

#### I) ACH status (new)
- `GET /api/v1/payments/ach/status`

Response consumed:
```json
{
  "success": true,
  "data": {
    "configured": true,
    "enabled": true,
    "pending_review_count": 2,
    "account": {
      "bank_code": "BANCO_GENERAL",
      "bank_name": "Banco General",
      "account_type": "checking",
      "account_number": "1234567890",
      "account_number_masked": "****7890",
      "account_holder_name": "Comercio Demo"
    }
  },
  "error": null
}
```

#### J) ACH account read (new)
- `GET /api/v1/payments/ach/account`

Response consumed (same account structure as above).

#### K) ACH account configure (new)
- `PUT /api/v1/payments/ach/account`

Request:
```json
{
  "bank_code": "BANCO_GENERAL",
  "bank_name": "Banco General",
  "account_type": "checking",
  "account_number": "1234567890",
  "account_holder_name": "Comercio Demo",
  "account_holder_document": "",
  "currency_code": "USD",
  "instructions_text": "Transferir y subir comprobante de pago",
  "amount_tolerance": "0.02",
  "reference_required": true,
  "payment_validity_minutes": 10080,
  "is_active": true
}
```

#### L) ACH disable (new)
- `POST /api/v1/payments/ach/disable`

Request:
```json
{}
```

#### M) Fee summary (new)
- `GET /api/v1/payments/fees/summary?currency_code=USD`

Response consumed:
```json
{
  "success": true,
  "data": {
    "currency_code": "USD",
    "pending_due_amount": 1.07,
    "overdue_amount": 0,
    "accrued_current_period_amount": 0.54,
    "paid_amount": 5.35,
    "next_batch_generation_at": "2026-04-20T00:00:00-05:00",
    "next_due_at": "2026-04-25T00:00:00-05:00"
  },
  "error": null
}
```

#### N) Fee transactions list (new)
- `GET /api/v1/payments/fees/transactions?page=1&size=20&status=unpaid&payment_method=ACH&currency_code=USD`

Response consumed:
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "related_order_number": "ORD-1022",
        "order_id": 1022,
        "payment_method": "ACH",
        "fee_generated": 0.27,
        "date": "2026-04-10T11:00:00-05:00",
        "fee_status": "unpaid",
        "billing_bucket": "pending_due",
        "batch_id": 22,
        "batch_status": "issued"
      }
    ],
    "pagination": {
      "total": 1
    }
  },
  "error": null
}
```

#### O) Fee batches list (new)
- `GET /api/v1/payments/fees/batches?page=1&size=20&status=issued&currency_code=USD`

Response consumed:
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "period_start": "2026-04-01",
        "period_end": "2026-04-15",
        "status": "issued",
        "total_lines": 12,
        "platform_fee_total": 9.99,
        "tenant_markup_total": 0.99,
        "total_fee_total": 10.98,
        "issued_at": "2026-04-16T00:00:00-05:00",
        "due_at": "2026-04-25T00:00:00-05:00",
        "paid_at": null
      }
    ],
    "pagination": {
      "total": 1
    }
  },
  "error": null
}
```

### 1.3 In-App Notifications (new)

#### A) List notifications
- `GET /api/v1/notifications?limit=20&offset=0`

Response consumed:
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1001,
        "kind": "ach.proof_uploaded",
        "title": "Comprobante ACH recibido",
        "message": "La orden ORD-1022 requiere revisión.",
        "action_type": "navigate",
        "action_url": "/payments/ach/index.html?payment_uid=pi_abc123",
        "metadata": {},
        "seen": false,
        "dismissed": false,
        "removed": false,
        "created_at": "2026-04-17T09:30:00-05:00",
        "updated_at": "2026-04-17T09:30:00-05:00"
      }
    ],
    "total": 57,
    "limit": 20,
    "offset": 0
  },
  "error": null
}
```

#### B) Unread count
- `GET /api/v1/notifications/unread-count`

Response consumed:
```json
{
  "success": true,
  "data": {
    "unread_count": 12
  },
  "error": null
}
```

#### C) Mark notification seen
- `POST /api/v1/notifications/{notification_id}/seen`

Request:
```json
{}
```

#### D) Dismiss notification
- `POST /api/v1/notifications/{notification_id}/dismiss`

Request:
```json
{}
```

#### E) Remove notification (hard delete)
- `DELETE /api/v1/notifications/{notification_id}`

Request:
```json
{}
```

### 1.4 Error code mapping updates used by these flows

New user-facing codes mapped in web:
- `O_RP_001`, `O_RP_002`, `O_RP_004`, `O_RP_005`
- `PAY_001`, `PAY_002`, `PAY_PP_001`
- `INV_001`, `INV_002`

KMP should map these to equivalent Spanish messages for parity.

## 2) UI Changes (visual + structure, no JS parity)

### 2.1 Order Creation (Step 3 Payments)

- Payment option tabs now include `Crear enlace de pago` as visible tab.
- Link tab shows a `Nuevo` badge the first time (per business).
- Link option helper text changed to: link is generated to share with customer for easy payment.
- If user selects link option and no configured payment method exists, show confirmation dialog:
  - title: `Configura tus métodos de pago`
  - CTA: `Configurar`
  - secondary: `Más tarde`

### 2.2 Order Details page (`orders/order-details.html`)

Action area changes:
- New button `Generar Link de Pago`.
- Existing link button changed to copy action (`Copiar Link de Pago`).
- Existing share payment link button kept.
- New button `Reintentar facturación` (conditional visibility).

Payment summary block:
- Added divider before `Pagos registrados` list.
- Payment intents list can show per-intent ACH details/actions inline.

New modals:
- **Generate Payment Link Modal**:
  - balance hero (`Saldo pendiente por cobrar`)
  - amount input (`Monto a cobrar`)
  - optional collapsible section with expiry presets (`1 hora`, `12 horas`, `24 horas`, `3 días`) and custom minutes.
- **Invoice Success Modal**:
  - success icon, total, CUFE, actions `Descargar factura` and `Compartir factura`.
- **ACH Reject Modal** (simple textarea version for order detail context).
- **ACH Proof Preview Modal**:
  - inline preview body
  - conditional download action.

### 2.3 New ACH Review page (`/payments/ach/index.html`)

New full screen/page for ACH case review:
- Hero header card with:
  - payment UID, reference, status pill, risk pill, score pill, recommendation.
- Commercial summary card (customer/order/amount/reference/date/bank/account).
- `Esperado vs Detectado` comparison table.
- Findings/factors card split into `Aumentan riesgo` / `Reducen riesgo`.
- Proof card with:
  - preview panel (PDF/image/unsupported fallback)
  - action buttons (approve/reject/download as allowed)
  - expected values side panel.
- Timeline card with chronological events and per-event status chips.
- Loading skeleton state, error state, and content state.
- Modal `Rechazar pago ACH` with reason code select + conditional custom reason.
- Modal `¿Qué significa el score ACH?`.

Visual direction:
- Distinct anti-fraud themed surface with gradients, rounded cards, animated reveal, and custom chips.
- Desktop magnifier for image proofs.

### 2.4 Settings Home page

- New settings list item `Pagos y cobros` with `Nuevo` badge.
- Navigates to `/settings/payment-methods.html`.

### 2.5 Payment Methods page (`settings/payment-methods.html`)

Page-level redesign:
- Wide shell layout.
- New onboarding hero with Lottie animation, headline, benefits, and primary CTA `Comenzar configuración`.

Configuration mode includes:
- **General Summary card**:
  - fee headline and bucket breakdown (`Por pagar`, `Vencido`, `Acumulado del período`, `Pagado histórico`)
  - next cycle/due date label
  - switch `Emitir factura automáticamente al recibir un pago`.
- **Available Channels list**:
  - row per method (Yappy, ACH, PayPal)
  - configured badge
  - per-method fee badge with tooltip (shown only when method is configured).
- **Method screen / wizard**:
  - 3-step structure (info, config, success)
  - method-specific content and forms.
- **Fee detail section**:
  - tab `Transacciones` + tab `Ciclos de cobro`
  - DataTables with server pagination + filters.

Updated modal copy:
- address required, address selector, generic confirm modal all reworded to Spanish UX copy used in web.

### 2.6 Topbar In-App Notifications

Topbar additions:
- bell dropdown block with unread badge.
- header with `Notificaciones`, unread count text, `Archivar todas`.
- scrollable notification list.
- footer button `Cargar más`.

Notification card UI:
- icon by kind (`payment.success`, `ach.proof_uploaded`, `ach.decision.*`, etc.).
- unread dot on title when unseen.
- archive icon button per row.
- timestamp line.
- empty state: `No tienes notificaciones nuevas.`

Responsive style updates:
- larger dropdown width on desktop.
- compact badge with count and 99+ behavior.
- styles for archive actions, unread dot, and list controls.

## 3) UI Logic / Behavior Changes

### 3.1 Authorization + route policy changes

Required policy parity:
- New action gate `orders.payment_link` -> requires scope `invoice:create_payment_link`.
- New ACH action gates:
  - `ach_payment.view`
  - `ach_payment.approve`
  - `ach_payment.reject`
- New route gate `ach_payment_details` (`/payments/ach/...`) -> requires `ach_payment:view`.
- Sub-user permissions UI now groups module `Pagos ACH`.

### 3.2 Payment link behavior (order creation + order detail)

Order creation:
- Link option requires both:
  - payments beta feature enabled for current business
  - scope `orders.payment_link`.
- On selecting link option:
  - mark `Nuevo` badge as seen in local storage per business.
  - fetch payment config summary; if no configured Yappy/PayPal/ACH -> confirm and redirect to payment settings.
  - before redirect, save current order checkpoint to resume flow.
- On restore/tamper or submit:
  - if option is `LINK` but user lacks access, force fallback to `MANUAL` and show warning.

Order detail:
- Active link source resolution priority:
  - `payment_links[]` then `payment_link` then `links[]`.
- Link status handling:
  - active unless `completed|expired|cancelled`
  - pending statuses block generating another link.
- Show/hide buttons by state:
  - generate shown when unpaid, remaining > 0, and no pending link.
  - copy/share shown when active link exists and order not paid.
- Generate modal validation:
  - `expire_in_minutes > 0`
  - amount optional, but if provided: `> 0` and `<= remaining`.
- On successful generation:
  - refresh order detail
  - open share modal with generated link when returned.

### 3.3 Invoice retry behavior

- New retry button appears when order is paid but not issued yet (and valid state constraints).
- Retry flow:
  - call retry endpoint
  - refresh order data
  - if now issued + CUFE, show success modal
  - else show warning (`invoice_warning_message` or default pending verification text).

### 3.4 ACH review behavior

Order detail payment intents:
- Identify ACH automatic payments via method/id/name + `is_automatic`.
- Normalize status tokens and map to badges (`Pendiente`, `Requiere acción`, `Pagado`, `Rechazado`, etc.).
- Lazy-load ACH detail per payment intent with in-memory cache + in-flight dedupe.
- Row can render:
  - metadata (bank, account holder, reference, fraud notes)
  - actions (`Ver detalles`, `Ver comprobante`) depending on status/permission.

Approve/reject from order detail:
- Approve requires confirmation.
- Reject requires reason text in modal.
- Both invalidate home summary cache and reload order detail on success.

Dedicated ACH review page:
- Fetch by payment uid from query param.
- Normalize multiple backend field aliases into a single detail model.
- Render sections only when user has ACH view permission.
- Proof access:
  - view allowed when proof exists and payment not rejected
  - download allowed only when approved.
- Reject flow uses predefined reason codes + custom text for `other`.
- High risk approval shows stronger confirmation copy.
- Proof preview pipeline:
  - prefer authenticated blob endpoint (`proof_id`)
  - fallback to URL preview if needed.
- Protection behavior in proof panel:
  - blocks print/save shortcuts where possible
  - disables context menu and drag in preview
  - rotating watermark ticker.

### 3.5 Payment settings behavior

Entry + feature gating:
- Payment settings page checks payments beta access; if missing, toast + redirect to general settings.

Onboarding:
- Shows onboarding hero until `payment_summary.onboarding_completed == true`.
- Onboarding action blocked if business has no address; opens address-required modal.
- Address selection flow updates business address then retries onboarding.

Method status rules:
- Yappy configured: `linked_account == true`.
- PayPal configured: both PayPal linked and billing agreement linked.
- ACH configured/ready: account configured and ACH enabled.

Method actions:
- Yappy: link/unlink.
- PayPal: connect account, create billing agreement, unlink.
- ACH: save account config, disable ACH.

General settings:
- Auto-invoice switch persists via API and rolls back UI on failure.

Fee billing:
- Summary panel refresh action fetches latest summary.
- Fee summary panel hidden when all tracked amounts are zero.
- Transactions + batch tables are server-side paginated, filtered, and responsive.

### 3.6 In-app notifications behavior

Lifecycle:
- Module initialized globally from common bootstrap.
- Polling every 30s.
- On visibility restore: refresh unread/list.

Caching:
- Local storage cache key per business.
- First paint can render cached notifications immediately.

Unread behavior:
- unread badge shown with cap `99+`.
- opening dropdown triggers list load and marks visible unseen notifications as seen.

List pagination:
- list API uses `limit` + `offset`.
- `Cargar más` appends, dedupes by id, and preserves existing rows.

Per-item actions:
- click card opens `action_url`.
- each item has archive action.
- swipe-left gesture on pointer devices archives item.

Bulk action:
- `Archivar todas` asks confirmation.
- loops list+dismiss in batches until empty/limit reached.
- supports partial-failure toast outcomes.

Analytics events (notification module):
- `notification_received`
- `notification_opened`
- `notification_read`
- `notification_archived`

Analytics params sent:
```json
{
  "business_id": 123,
  "user_id": 99,
  "user_email": "user@demo.com",
  "notification_id": 1001,
  "notification_type": "ach.proof_uploaded"
}
```

### 3.7 Extra analytics added around payments/settings

Payment analytics events introduced and used in these flows include:
- payment settings viewed/onboarding lifecycle
- payment method config attempted/succeeded/failed
- order payment link actions
- order creation payment option selected
- printer onboarding/config events (inside settings area)

KMP should emit equivalent events (same names and param keys) where the same user actions exist.
