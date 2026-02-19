## Skill Invocation Policy

Apply `$disciplined-execution` on every task by default.

**Required on all tasks**:
- Start with plan-first execution for non-trivial work.
- Use verification gates before marking work complete.
- Keep task tracking in `tasks/todo.md` and corrections in `tasks/lessons.md`.
- Apply simplicity, root-cause, and minimal-impact principles from the skill.

## Reference Documentation

Before making changes, review the relevant documentation:

- **[Architecture](doc/architecture.md)** — Project structure, layers, data flow, navigation, DI, core services, and key dependencies.
- **[Code Conventions](doc/code-conventions.md)** — Naming, ViewModel/State/Event patterns, Service/Repository/Provider patterns, serialization, error handling, loading states, async, and DI usage.

---

# UI Style Guide for Invoice / Quote / Transaction Detail Screens

This document describes the standard UI patterns used to present invoices, quotes, and transaction details in the VentaGo KMP app.

## Layout Pattern — Card-Based Layout

All detail screens (Orders, Quotes, Expenses) use the same **card-based layout**.

Used by: `OrderDetailsScreen`, `QuoteDetailsScreen`, `ExpenseDetailsScreen`

**Structure:**
```
[Header Card - icon + title + status badge + info rows + divider + summary + total]
[Items Card - icon + title + item rows with dividers]
[Payment/Parties Card - icon + title + payment rows or party info]
[Additional Cards - invoicing, notes, CUFE, etc.]
[Action buttons below cards]
```

**Key Components:**
- **Card:** `Card(elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = cardContainerColor()))`
- **Section headers inside cards:** `Icon(20.dp, MaterialTheme.colorScheme.primary)` + `Spacer(8.dp)` + `Text(bodyMediumBold())`
- **Header card title:** `Icon(22.dp, primary)` + `Spacer(8.dp)` + `Text(titleMediumBold())` + status badge on the right
- **Info rows (label-value):**
  ```kotlin
  @Composable
  private fun InfoRow(label: String, value: String, maxLines: Int = 1) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(label, style = labelSmall(color = onSurfaceVariant), modifier = Modifier.weight(0.4f))
          Text(value, style = bodyMedium(), textAlign = TextAlign.End, maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(0.6f))
      }
  }
  ```
- **Sub-section dividers:** Standard `Divider()` within cards
- **Total row:** `bodyMediumBold()` label + `bodyMediumBold(color = primary)` value in a `Row(SpaceBetween)`
- **Item rows:** Item name (`bodyMedium`) + unit price (`labelSmall(onSurfaceVariant)`) on left, quantity badge (`bodyMediumBold`, e.g. "2x") on right, with `Divider` between items
- **Actions:** Full-width buttons (`ButtonM`, `OutlinedButtonM`, `TextButtonS`) placed below the cards
- **Outer column:** `.padding(16.dp)` + `Arrangement.spacedBy(16.dp)`

**Reference files:**
- `features/orders/ui/order_details/OrderDetailsScreen.kt`
- `features/quotes/ui/details/QuoteDetailsScreen.kt`
- `features/expenses/ui/details/ExpenseDetailsScreen.kt`

---

## Shared Conventions

### Typography (from `design_system/theme/Typography.kt`)
- `bodyMedium()` — standard body text
- `bodyMediumBold()` — emphasized body text, section titles in cards
- `bodySmall()` — secondary info (descriptions)
- `titleMediumBold()` — screen titles, prominent headers
- `labelSmall(color)` — metadata labels, hints, info row labels
- `latoFontFamily()` — used in specific custom text styles

### Formatting Utilities
- **Money:** `formatNumberToMoney(amount: String)` from `utils/StringFormatterUtils.kt`
- **Dates:** `DateFormat.getFormattedDate(input, inputFormat, outputFormat)` from `utils/DateUtils.kt`
- **Order dates:** `DateFormat.getOrdersFormattedDate(isoDate)` — formatted for order/quote context

### Color Tokens
- `cardContainerColor()` — card container background
- `MaterialTheme.colorScheme.primary` — accent color for totals, icons
- `MaterialTheme.colorScheme.onSurfaceVariant` — label text color in info rows
- Status colors: Green (`0xFF4CAF50`/`0xFF2E7D32`), Red (`0xFFF44336`/`0xFFD32F2F`), Amber (`0xFFFF9800`/`0xFFFFA000`)

### Status Badges
- **Pill-style (Orders):** `OrderStatusChip` / `SuggestionChip` with colored container/label
- **Pill-style (Quotes):** `Surface(shape = RoundedCornerShape(50))` with colored text
- **Card-style (Expenses):** `Text` with `.background(statusColor, RoundedCornerShape(4.dp)).padding(horizontal = 6-8.dp, vertical = 2-3.dp)`, white text

### Design System Components
- `ButtonM` — primary action button (full width)
- `OutlinedButtonM` — secondary action button (full width)
- `TextButtonS` — tertiary/link-style action
- `DMOutlinedTextField` — standard text input
- `shimmerBrush()` — loading skeleton animation
- `Card` + `CardDefaults` — section containers

---

## Engineering Guardrails (Expenses + Similar Flows)

- **Never build API JSON manually in ViewModels** (`buildJsonObject`, raw string payloads, etc.). Define `@Serializable` request models and pass typed request objects through `ViewModel -> Service -> Repository -> Provider`.
- **Every user-triggered API action must show `LoadingSheet` feedback** with `LoadingState.LOADING` while executing and `LoadingState.SUCCESS` or `LoadingState.ERROR` when it finishes.
- **Repository endpoint calls must be wrapped in `try/catch` with logger tracking** (`ILoggerService`, `Log`, `LogLevel.ERROR`) and rethrow errors after logging context (businessId, resourceId, action).
- **Cross-screen state consistency is mandatory after mutations** (e.g., payments in detail screens impacting list screens). Publish updates from the service layer (shared flow/listener) and update cache so list/detail UIs stay in sync when navigating back without forcing full reloads.
- **All date inputs must use a calendar picker component** (e.g., `InstallmentDueDateFieldKmp`). Avoid manual free-text date entry fields in forms.
- **Never double-count summary metrics when composing cards/charts.** If API fields are hierarchical (e.g., `overdue` is already included in `pending/total`), do not sum parent + child. Treat parent as total and derive sub-buckets (`nonOverdue = total - overdue`) before rendering ratios or totals.
