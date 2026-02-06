# VentaGo Expenses Module -> Kotlin Multiplatform Replication Spec

This document is the source-grounded prompt/spec to replicate the current web `expenses` module in a Kotlin Multiplatform app.

Scope is based on the current implementation in:
- `src/modules/expenses/**`
- `src/saas/expenses/expenses.html`
- `src/saas/expenses/new-expense.html`
- `src/saas/expenses/expense-details.html`
- `src/core/api/api.client.js`
- `src/core/utils/date.utils.js`

## Copy/Paste Prompt For KMP Team

Implement the full `Expenses` module in Kotlin Multiplatform with parity to the current web behavior. Do not redesign behavior. Replicate these flows exactly:

1. Expenses list with server pagination, search, filters, export, import, onboarding, register card, crawl-jobs summary, and background cache refresh.
2. New expense flow with two modes:
- `manual`: create/edit manual expense, optional manual CUFE, invoice file upload, items, optional initial payments.
- `cufe`: import by CUFE or QR/PDF scan.
3. Expense details flow:
- render full expense info, responsive items, payment summary/history/actions, manual-only edit, duplicate, delete, DGI link, file download or generated non-fiscal PDF.
4. Payments CRUD under expense details:
- create/edit/delete/mark-as-paid, proof file upload, overpayment confirmation, credit lock rule.
5. Local caching:
- instant cache-first rendering for list, background sync, no duplicate API calls for same request, no duplicated rows, preserve active filters while syncing.
6. Feature flags:
- respect `expenses_qr`, `expenses_ocr` behavior gates.
7. Date/time and payload formats:
- API date strings must be `YYYY-MM-DDTHH:MM:SS-05:00` for Panama offsets in filters/payments/manual creation.
- UI date display parity (list emission date is date-only).

### Hard Rules

- Keep all user-facing labels in Spanish where current UI uses Spanish.
- Keep business restrictions:
- only manual expenses can be edited.
- duplicate excludes invoice number and CUFE.
- register payment blocked when a pending full-amount credit line already exists.
- generated PDF must explicitly label `NO ES FACTURA FISCAL`.
- Cache keys and scope must be per business.
- Do not call list endpoint twice on initial load for same page/filter request.

---

## 1) API Contract (from frontend usage)

Base URL: `ORDERS_BASE_PATH` (`src/core/config/env.js`).

Headers used by frontend:
- `Authorization: Bearer <ACCESS_TOKEN>`
- `X-Business-ID: <BUSINESS_ID>`
- `Content-Type: application/json` for JSON requests
- `multipart/form-data` for file requests

Response handling in web client (`api.client.js`):
- Success usually comes in `response.data` (or full response if no `data` key).
- Error comes as `response.error` code or HTTP error.
- Frontend normalizes to `{ success, data, error }`.

Use this normalized envelope in KMP transport layer:

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

### 1.1 Expenses List

Endpoint:
- `POST /api/v1/expenses/list`

Request body used:
- `business_id`, `page`, `page_size`
- optional filters: `start_date`, `end_date`, `source`, `issuer_name`, `issuer_ruc`, `invoice_number`

Curl:

```bash
curl -X POST "$BASE/api/v1/expenses/list" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Business-ID: $BUSINESS_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "business_id": 123,
    "page": 1,
    "page_size": 10,
    "start_date": "2026-01-01T00:00:00-05:00",
    "end_date": "2026-01-31T23:59:59-05:00",
    "issuer_name": "BANCO",
    "issuer_ruc": "123456",
    "invoice_number": "FAC-001",
    "source": "manual"
  }'
```

Expected data fields consumed:

```json
{
  "success": true,
  "data": {
    "expenses": [
      {
        "id": 1,
        "invoice_number": "FAC-001",
        "cufe": "FE...",
        "emission_date": "2026-01-21T00:00:00-05:00",
        "issuer": {"name": "Proveedor", "ruc": "123", "dv": "4"},
        "receiver": {"name": "Mi Empresa", "ruc": "456", "dv": "1", "type": "business"},
        "subtotal": 10,
        "itbms_total": 0.7,
        "total_amount": 10.7,
        "payment_status": "not_paid",
        "payments": [],
        "source": "manual",
        "created_at": "2026-01-21T10:00:00Z"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 10
  },
  "error": null
}
```

### 1.2 Expense Detail

Endpoint:
- `GET /api/v1/expenses/:expense_id`

Curl:

```bash
curl -X GET "$BASE/api/v1/expenses/123" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Business-ID: $BUSINESS_ID"
```

Expected fields consumed:
- core: `id`, `invoice_number`, `cufe`, `source`, `file_url`, `emission_date`, `authorization_date`, `authorization_protocol`, `payment_method`, `notes`
- parties: `issuer.*`, `receiver.*`
- items: `items[]`
- payment: `payment_status`, `total_paid`, `payment_summary`, `payments[]`

### 1.3 Create Manual Expense

Endpoint:
- `POST /api/v1/expenses/create`

Content types:
- JSON (no files)
- multipart with `payload` JSON + optional `file` + optional `proof_file_0...proof_file_n`

Curl JSON:

```bash
curl -X POST "$BASE/api/v1/expenses/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Business-ID: $BUSINESS_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "business_id": 123,
    "invoice_number": "FAC-001",
    "cufe": "FE...",
    "emission_date": "2026-01-21T00:00:00-05:00",
    "payment_method": "bank_transfer",
    "issuer": {"name": "Proveedor", "ruc": "123", "dv": "4"},
    "receiver": {"name": "Mi Empresa", "ruc": "456", "dv": "1", "type": "business"},
    "items": [
      {
        "line_number": 1,
        "description": "Producto",
        "quantity": 1,
        "unit_price": 10,
        "discount_amount": 0,
        "subtotal": 10,
        "itbms_amount": 0.7,
        "total": 10.7
      }
    ],
    "subtotal": 10,
    "itbms_total": 0.7,
    "total_amount": 10.7,
    "payment": {
      "payment_method": "bank_transfer",
      "amount_paid": 10.7,
      "payment_date": "2026-01-21T00:00:00-05:00"
    }
  }'
```

Curl multipart example:

```bash
curl -X POST "$BASE/api/v1/expenses/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Business-ID: $BUSINESS_ID" \
  -F 'payload={"business_id":123,"invoice_number":"FAC-001","emission_date":"2026-01-21T00:00:00-05:00","items":[{"line_number":1,"description":"Producto","quantity":1,"unit_price":10,"subtotal":10,"itbms_amount":0.7,"total":10.7}],"subtotal":10,"itbms_total":0.7,"total_amount":10.7};type=application/json' \
  -F "file=@/path/invoice.pdf" \
  -F "proof_file_0=@/path/payment-proof.jpg"
```

### 1.4 Update Manual Expense

Endpoint:
- `PUT /api/v1/expenses/:expense_id`

Rules used by frontend:
- only for `source == manual`
- update supports JSON or multipart (`payload` + optional `file`)
- when user removes existing file and uploads none: send `remove_file: true`

Curl JSON:

```bash
curl -X PUT "$BASE/api/v1/expenses/123" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Business-ID: $BUSINESS_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "invoice_number": "FAC-001-EDIT",
    "cufe": "FE...",
    "emission_date": "2026-01-21T00:00:00-05:00",
    "issuer": {"name": "Proveedor Editado"},
    "receiver": {"name": "Mi Empresa"},
    "items": [{"line_number":1,"description":"Producto","quantity":2,"unit_price":5,"subtotal":10,"itbms_amount":0.7,"total":10.7}],
    "subtotal": 10,
    "itbms_total": 0.7,
    "total_amount": 10.7,
    "notes": "Actualizado",
    "remove_file": false
  }'
```

### 1.5 Delete Expense

Endpoint:
- `DELETE /api/v1/expenses/:expense_id`

Curl:

```bash
curl -X DELETE "$BASE/api/v1/expenses/123" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Business-ID: $BUSINESS_ID"
```

### 1.6 Crawl Jobs (CUFE import)

Endpoints:
- `POST /api/v1/expenses/crawl` body: `{ cufe, business_id }`
- `GET /api/v1/expenses/crawl/:job_id/status`
- `POST /api/v1/expenses/crawl/list` body: `{ page, page_size }`

List sample (seen in project behavior):

```json
{
  "success": true,
  "data": {
    "jobs": [],
    "total": 0,
    "page": 1,
    "page_size": 100,
    "total_pages": 0
  },
  "error": null
}
```

Status fields consumed:
- `status` (`pending|processing|success|failed`)
- `expense_id`
- `error_message`
- `attempt_count`, `max_attempts`, `next_attempt_at`, `last_attempt_at`, `message`

### 1.7 Payments

Endpoints:
- `POST /api/v1/expenses/:expense_id/payments`
- `GET /api/v1/expenses/:expense_id/payments`
- `GET /api/v1/expenses/:expense_id/payments/:payment_id`
- `PUT /api/v1/expenses/:expense_id/payments/:payment_id`
- `DELETE /api/v1/expenses/:expense_id/payments/:payment_id`

Create JSON sample:

```json
{
  "payment_method": "credit",
  "amount_paid": 25.0,
  "reference": "REF-123",
  "notes": "Pago parcial",
  "payment_date": null,
  "due_date": "2026-02-10T23:59:59-05:00"
}
```

Create multipart sample:
- form field `payload` JSON
- optional `proof_file`

Payment object fields consumed:
- `id`, `expense_id`, `payment_method`, `payment_status`, `amount_paid`, `reference`, `proof_file_url`, `proof_file_name`, `notes`, `payment_date`, `due_date`, `is_overdue`, `days_overdue`

### 1.8 Excel Import

Endpoints:
- `POST /api/v1/expenses/import` (multipart `file`)
- `POST /api/v1/expenses/import/list`

Import template columns expected by frontend parser:
1. Numero de Factura
2. Fecha
3. Metodo de Pago
4. Nombre emisor
5. RUC emisor
6. DV Emisor
7. Direccion emisor
8. Telefono emisor
9. Item No.
10. Item Descripcion
11. Item Cant.
12. Item Precio Unit.
13. Item Descuento
14. Item Total
15. Notas
16. CUFE

---

## 2) Data/Domain Mapping

Mirror these entities in shared KMP module:

### Expense
- `id`, `business_id`, `invoice_number`, `cufe`, `emission_date`, `issuer`, `receiver`, `subtotal`, `itbms_total`, `total_amount`, `currency_code`, `payment_method`, `notes`, `authorization_protocol`, `authorization_date`, `file_url`, `source`, `items[]`, `payments[]`, `payment_status`, `payment_summary`, `total_paid`, `created_at`, `updated_at`

### Payment
- `id`, `expense_id`, `payment_method`, `payment_status`, `amount_paid`, `reference`, `proof_file_url`, `proof_file_name`, `notes`, `payment_date`, `due_date`, `is_overdue`, `days_overdue`, `created_at`, `updated_at`

### ExpenseItem
- `line_number`, `item_code`, `description`, `quantity`, `unit_price`, `discount_amount`, `subtotal`, `itbms_amount`, `total`

---

## 3) Caching and Sync (must match behavior)

### Keys and scope
- Expenses cache key: `expensesCache`
- Payments cache key: `paymentsCache`
- Crawl jobs cache key: `expensesCrawlJobs`
- Onboarding completion key: `expenses_onboarding_completed`

All caches are business-scoped.

### Expenses list strategy

Implement this exact behavior:

1. Build `requestKey = JSON.stringify({page,size,sortedFilters})`.
2. If there is `pendingRefreshResult` for same `requestKey`, use it immediately and do not call API again.
3. On first load (`cacheHydrationCompleted == false`), if cache has rows:
- render cached page immediately,
- show sync indicator: `Verificando nuevos gastos...`,
- call API in background.
4. Background refresh stores result in `pendingRefreshResult` and triggers table reload without page reset.
5. Deduplicate same in-flight network request by `requestKey` (`inFlightRequest`).
6. Ignore stale background responses when current request key changed (filters/page changed).
7. Merge fresh expenses into cache by `id` to avoid duplicates.
8. Keep active filters when updating rendered rows.

### Filters applied over cached data
- `start_date`, `end_date` (date-only compare)
- `invoice_number` (contains)
- `issuer_name` (contains)
- `issuer_ruc` (contains on ruc or `ruc-dv`)
- `source` exact
- `payment_status` in selected statuses

### Export range constraint
- max 3 months range.
- If no date filters, auto-apply last 3 months.
- If only start date, end date = min(start + 3 months, today).
- If only end date, start date = end - 3 months.
- Reject ranges > 3 months.

---

## 4) UI Behavior by Screen

## 4.1 Expenses List

Required:
- Register button visible immediately for non-onboarding users.
- Crawl summary area behavior:
- if `expenses_qr` enabled: keep two-column layout (`register 4 / summary 8`), show summary loader while API loads.
- if no jobs and user has QR beta: show empty summary card text (`Sin facturas pendientes de revision`) to preserve proportions.
- if no QR beta: summary hidden and register card full-width.
- Search input with explicit search button.
- Search input currently targets invoice number.
- Issuer name filtering is available in filters dropdown.
- Date column shows only date (`formatDate`), not time.
- Sync indicator shown during background refresh.

## 4.2 New Expense - Manual mode

- Prefill `receiver-name` and `receiver-ruc` from current business if input is empty.
- Optional `manual-cufe` field; send as payload `cufe`.
- Items:
- editable table on desktop,
- mobile card editor modal,
- Enter key on last row adds new item,
- totals recalc on any item change.
- File upload:
- invoice file accepts PDF/image,
- drag/drop + click,
- preview + replace/remove actions,
- in edit mode support `remove_file` behavior.
- Payments (create mode only):
- optional rows,
- credit method requires due date,
- non-credit requires payment date,
- amount > 0 and cannot exceed expense total,
- optional proof files accepted only for non-credit and beta access.
- Submit labels must adapt:
- create: `Registrar Gasto`
- edit: `Actualizar Gasto`

## 4.3 New Expense - CUFE mode

- Show two clear options:
1. Enter CUFE manually and press import button.
2. Upload PDF with QR (desktop drag/drop) or scan QR with camera (mobile).
- Desktop upload element acts as a drag/drop zone and click target.
- Import button is visually tied to CUFE input (same input group).
- PDF processing:
- validate type PDF and max 10MB,
- convert first page via PDF.js,
- scan QR via jsQR with scales `[3.0, 2.5, 4.0, 2.0]` and inversion attempts,
- extract CUFE via parser,
- auto-trigger import job.
- Job status widget states:
- `pending`, `processing`, `success`, `failed`, `timeout`.
- Poll every 2s until terminal state or retry threshold logic.

## 4.4 Expense Details

- Replace raw payment method codes with Spanish friendly label.
- Emission date displayed as date-only.
- Responsive items:
- desktop table + mobile cards.
- Actions:
- Edit (manual only)
- Duplicate (always, but excludes invoice number and CUFE)
- Delete
- Download:
- if `file_url` exists: download original file
- if no file: generate and download a non-fiscal PDF receipt with warning text
- DGI action shown only for crawled expenses with CUFE.

Generated PDF content requirements:
- title: `Comprobante de Gasto`
- warning: `Documento informativo - NO ES FACTURA FISCAL`
- include metadata, issuer/receiver, items, payments, totals.

## 4.5 Payments in Expense Details

- Summary cards: total expense, total paid, remaining, status badges.
- Register payment button hidden when paid.
- Credit lock rule:
- if there is a pending credit payment with due date and amount >= total expense, block new payment registration.
- message explains user must mark paid or delete that credit line.
- Table actions per payment:
- mark as paid,
- edit,
- delete,
- download proof if available.
- Payment modal:
- create/edit/mark-as-paid modes,
- overpayment asks confirmation,
- payment date and due date use Flatpickr behavior parity,
- proof upload allows PDF/images up to 10MB.

---

## 5) Business Rules and Constraints

- Expense `source` values used: `manual`, `crawled`.
- Only manual expenses can be edited.
- Duplicate flow:
- store prefill payload in session,
- remove `invoice_number` and `cufe` from prefill,
- include issuer/receiver/items/notes/payments.
- CUFE parsing accepts:
- URL query param `chFE=FE...`
- URL path `/FacturasPorCUFE/FE...`
- direct plain `FE...`
- CUFE validity in frontend: starts with `FE` and length >= 50.

---

## 6) Date and Formatting Rules

Use these formats:
- API day-start: `YYYY-MM-DDT00:00:00-05:00`
- API day-end: `YYYY-MM-DDT23:59:59-05:00`
- UI date-only: `DD/MM/YYYY`
- UI datetime (Panama): `DD/MM/YYYY HH:mm`

Where applied:
- list emission date -> date only.
- details emission date -> date only.
- payments due/payment display -> datetime format in details.

---

## 7) Events and UI refresh triggers

Replicate event-driven updates:
- `payment:created`
- `payment:updated`
- `payment:deleted`

On these events:
- refresh expense details payment summary/list,
- refresh expenses list table,
- refresh overdue alert banner.

---

## 8) Suggested KMP Architecture (implementation shape)

Use your app standard stack, but keep this split:
- `shared:data`: API DTOs + remote datasource + local cache datasource
- `shared:domain`: entities + mappers + use cases
- `shared:presentation`: state machines for list/details/new/payment flows
- `app`: platform UI rendering

Recommended state containers:
- `ExpensesListState`
- `ExpenseDetailsState`
- `ExpenseEditorState` (manual/cufe modes)
- `PaymentsState`
- `ImportExpensesState`

---

## 9) Acceptance Checklist

Ship only when all pass:

1. List opens from cache instantly when available; background sync updates rows without full refresh.
2. No duplicated list API call for same initial request key.
3. No duplicate rows after refresh.
4. Filters remain applied after background sync.
5. Search button works and issuer-name filter works.
6. Export rejects ranges > 3 months.
7. Manual creation sends optional `cufe` and correct item totals fields (`subtotal`, `total`, plus global totals).
8. Edit flow works only for manual expenses and uses update endpoint.
9. Duplicate flow preloads everything except invoice number and CUFE.
10. CUFE mode supports manual input and PDF/QR flow.
11. PDF QR scanner works only when PDF.js + jsQR are loaded; clear error when missing.
12. Payment lock rule for full credit line is enforced.
13. Details download generates non-fiscal PDF when no original file.
14. Emission date in list/details shows date only.
15. Mobile items rendering is readable in details and editor.

---

## 10) Notes for KMP parity testing

Test with:
- user with `expenses_qr` enabled and disabled.
- empty cache and warm cache.
- no crawl jobs vs active crawl jobs.
- manual expense with file, without file, with payments, and credit-only pending payment.
- imported/crawled expense to validate read-only edit rule.

