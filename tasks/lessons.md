# Lessons Learned

- En bugs de contratos API, no asumir que el problema está en el builder lógico; primero registrar el payload JSON final ya normalizado que sale por HTTP y contrastarlo con el ejemplo esperado del backend.
- Si un endpoint serializado con `encodeToJsonElement(request)` muestra un body vacío o inconsistente, forzar el serializer explícito del request (`Type.serializer()`) y cubrirlo con un test de serialización.
- Si un contrato crítico sigue saliendo mal aun con serializer explícito, construir el `JsonObject` del provider con claves tipadas y testear ese shape exacto; no insistir con magia de serialización.
- For interactive charts, validate label density per range and ensure smoothed paths pass through selected points so markers/tooltips never appear detached from the curve.
- Home summary metrics can be hierarchical: `overdue` amounts/counts may already be included in `pending` totals. Do not sum `pending + overdue` in UI; use `pending` as total and derive non-overdue pending as `max(total - overdue, 0)`.
- Prevention rule: before wiring KPI cards, verify metric semantics with API logs/sample payloads and add a short inline note in the composable when a derived value is used to avoid future double-count regressions.
- For side-by-side toolbar controls in Compose (search + segmented toggle), define one shared height constant and apply it to both controls to avoid visual mismatch from component defaults.
- In Compose list screens, never publish a shared mutable collection from the service layer into UI state. Snapshot with `toList()` before storing/rendering, or concurrent service mutations can crash `LazyColumn` with `ConcurrentModificationException`.
- When a backend paginates from page `1`, keep the client state/request defaults aligned to that contract and de-duplicate appended results by stable identifiers so repeated pages cannot crash keyed lazy lists.
