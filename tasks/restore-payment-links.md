# Restore Payment Links & Payment Methods UI

Once the backend bug with payment links is fixed, restore the hidden UI by following these steps.

---

## 1. PaymentScreen.kt — Restore "Enlace de Pago" tab

**File:** `composeApp/src/commonMain/kotlin/com/teco/ventago/features/pos/ui/PaymentScreen.kt`

**What was hidden:**
- The `TabRow` with tabs "Manual/Cuotas" and "Enlace de Pago" (shown when the document type is not a credit/debit note)
- The `PaymentLinkSection` composable rendered when the "Enlace de Pago" tab is selected

**How to restore:**
1. Find the comment `// HIDDEN: Payment Link tab temporarily disabled (backend bug)`
2. Uncomment the full `if (!isCreditOrDebitNote) { ... TabRow ... }` block
3. Restore the conditional logic around `ManualAndInstallmentsSection`:
   - Wrap it back in `if (isCreditOrDebitNote || ui.paymentFlowMode == PaymentFlowMode.MANUAL_OR_INSTALLMENTS)`
4. Find the comment `// HIDDEN: PaymentLinkSection temporarily disabled (backend bug)`
5. Uncomment the `else { PaymentLinkSection(...) }` block
6. Remove the comment `// Always show manual payment while payment links are disabled`

**Restored code should look like:**
```kotlin
// Credit notes (04) and debit notes (05) cannot use payment links or be saved as drafts
val isCreditOrDebitNote = ui.selectedDocType == "04" || ui.selectedDocType == "05"

if (!isCreditOrDebitNote) {
    val modes = listOf(PaymentFlowMode.MANUAL_OR_INSTALLMENTS, PaymentFlowMode.PAYMENT_LINK)
    val labels = listOf("Manual/Cuotas", "Enlace de Pago")
    TabRow(
        selectedTabIndex = modes.indexOf(ui.paymentFlowMode),
        modifier = Modifier,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[modes.indexOf(ui.paymentFlowMode)]),
                color = MaterialTheme.colorScheme.secondary
            )
        },
        containerColor = MaterialTheme.colorScheme.background) {
        modes.forEachIndexed { i, m ->
            Tab(
                selected = (m == ui.paymentFlowMode),
                onClick = { viewModel.setPaymentFlow(m) },
                text = { Text(labels[i]) }
            )
        }
    }
}

Spacer(Modifier.height(12.dp))

if (isCreditOrDebitNote || ui.paymentFlowMode == PaymentFlowMode.MANUAL_OR_INSTALLMENTS) {
    ManualAndInstallmentsSection(...)
} else {
    PaymentLinkSection(
        totalToCharge = totalToCharge,
        enabled = hasPositiveAmount,
        onConfirm = {
            requestGovernmentWarningOrProceed {
                if (!ui.paymentsConfigured) {
                    navigate(PosScreens.Payments, null)
                } else {
                    viewModel.createOrder(createPaymentLink = true, saveAsDraft = false)
                }
            }
        }
    )
}
```

---

## 2. SettingsScreen.kt — Restore "Metodos de pago" card

**File:** `composeApp/src/commonMain/kotlin/com/teco/ventago/features/settings/ui/settings/SettingsScreen.kt`

**What was hidden:**
- The entire Payments Card showing PayPal, Yappy, and Bank Transfer payment methods

**How to restore:**
1. Find the comment `// HIDDEN: Payments Card temporarily disabled (backend bug)`
2. Replace that comment block with the full Payments Card. The card should be placed between the Profile Card (business info) and the QuoteSettingsSection.

**Restored code:**
```kotlin
// Payments Card
Card(modifier = Modifier.fillMaxWidth().padding(all = 16.dp),
    elevation = CardDefaults.elevatedCardElevation(4.dp),
    colors = CardDefaults.cardColors(
        containerColor = cardContainerColor(),
    ),
    shape = RoundedCornerShape(10.dp),
    onClick = {
        navigate(PosScreens.Payments)
    })
{
    Text(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        text = stringResource(Res.string.payment_methods),
        style = titleMedium()
    )
    if (uiState.loadingPaymentMethods) {
        val brush = shimmerBrush()
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.fillMaxWidth().height(100.dp)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .clip(shape = RoundedCornerShape(4.dp)).background(brush = brush))
            Spacer(modifier = Modifier.fillMaxWidth().height(48.dp)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .clip(shape = RoundedCornerShape(4.dp)).background(brush = brush))
        }
        return@Card
    }
    Text(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        text = stringResource(Res.string.settings_configure_payment),
        style = bodyMedium()
    )
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // PayPal, Yappy, Bank Transfer PaymentItems + DottedButton
        // (see git history for exact code)
    }
}
```

**Tip:** Use `git diff` or `git log -p` to see the exact original code that was replaced.

---

## 3. HomeScreen.kt — Restore Yappy payment config card

**File:** `composeApp/src/commonMain/kotlin/com/teco/ventago/features/home/ui/HomeScreen.kt`

**What was hidden:**
- The card prompting users to configure Yappy payments (shown when `!appState.value.paymentsConfigured`)

**How to restore:**
1. Find the comment `// HIDDEN: Yappy/payment methods configuration card temporarily disabled (backend bug)`
2. Replace with the original conditional block:

**Restored code:**
```kotlin
if (!appState.value.paymentsConfigured) {
    Card(
        modifier = Modifier.padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
    ) {
        Row(modifier = Modifier.padding(top = 8.dp), ...) {
            Image(painter = painterResource(Res.drawable.yappy_logo_portrait), ...)
            Column(...) {
                Text("Recibe pagos con Yappy", ...)
                Text("Conecta tu cuenta Yappy y acepta pagos...", ...)
            }
        }
        Row(...) {
            Button(onClick = { navigate(PosScreens.Payments) }) {
                Icon(imageVector = Icons.Rounded.Payment, ...)
                Text(stringResource(Res.string.configure), ...)
            }
        }
    }
}
```

**Tip:** Use `git diff` or `git log -p` to see the exact original code.

---

## Quick restore via git

The fastest way to restore all three files is to check the git diff from when these changes were made:

```bash
git log --oneline --all | head -20   # find the commit that hid payment links
git show <commit-hash>               # review the exact changes
git revert <commit-hash>             # revert all changes at once
```

---

## Checklist

- [ ] Backend payment link bug is fixed and verified
- [ ] PaymentScreen.kt — TabRow and PaymentLinkSection restored
- [ ] SettingsScreen.kt — Payments Card restored
- [ ] HomeScreen.kt — Yappy config card restored
- [ ] Test: POS flow shows "Enlace de Pago" tab
- [ ] Test: Settings shows payment methods card
- [ ] Test: Home shows Yappy setup prompt for unconfigured users
- [ ] Delete this file after restore is complete
