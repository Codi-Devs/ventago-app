# Lessons Learned

- For interactive charts, validate label density per range and ensure smoothed paths pass through selected points so markers/tooltips never appear detached from the curve.
- Home summary metrics can be hierarchical: `overdue` amounts/counts may already be included in `pending` totals. Do not sum `pending + overdue` in UI; use `pending` as total and derive non-overdue pending as `max(total - overdue, 0)`.
- Prevention rule: before wiring KPI cards, verify metric semantics with API logs/sample payloads and add a short inline note in the composable when a derived value is used to avoid future double-count regressions.
