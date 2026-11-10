# PayWise PNG 🇵🇬

**A simple and reliable way to manage your money.**  
Created for Papua New Guineans.  
Developed by **PodzSphere** | Authored by **Able Sam** (`@astp2019`)

---

## Overview

**PayWise PNG** is an offline-first personal finance, fortnightly payday budgeting, expense tracking, and savings management Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, **MVVM Architecture**, and **Room Database**.

### Key Features
- **Fortnightly Payday Management**: Default payday anchored to `17 July 2026` with automatic 14-day pay period calculation, countdown of days remaining until next payday, fortnightly salary configuration, and historical payday records.
- **Exact `BigDecimal` Financial Rules**:
  - `Available Balance = Total Income - Total Expenses - Current Savings`
  - Savings deposits reduce available spending money without being counted as ordinary expenses.
  - Savings withdrawals increase available spending money.
  - Zero duplicate transaction counting.
- **PNG Default & Custom Budget Categories**:
  - Includes `Dinau (Person)`, `Relatives`, `Food`, `Transport`, `Phone Credit`, `Flex / Rait Kad` (with quick `K3`, `K5`, `K10`, `K20` voucher chips), and `Other Expenses`, plus user-created custom categories.
- **Payday Budget Checkbox System**:
  - Plan upcoming fortnightly expenses before payday. Checking an item as paid automatically deducts the amount from Available Balance, adds it to Total Expenses, and records the linked transaction. Unchecking reverses the transaction cleanly without duplicates.
- **Savings Goals & Progress Tracking**:
  - Create savings goals (e.g., `K2,000.00`), deposit and withdraw funds, and track progress with percentage indicators (`K500 / K2,000 = 25%`).
- **Financial History & Export**:
  - Filter transactions by Daily, Weekly, Fortnightly, Monthly, or All Time, by Category, and by Type.
  - Offline JSON Backup & Restore + Export Transactions to CSV.
- **Security**:
  - 100% local Room SQLite storage, optional 4-digit PIN lock & biometric unlock, and delete confirmation dialogs.

---

## APK Build Instructions

### Prerequisites
- **Android Studio** Ladybug or newer
- **JDK 17+**
- **Android SDK 36**

### 1. Build Debug APK from Command Line
```bash
gradle :app:assembleDebug
```
The compiled installable APK will be generated at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Run Financial & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

### 3. Automatic GitHub Actions APK Build
A complete GitHub Actions workflow is included at `.github/workflows/android-build.yml`. Pushing to `main` or `master` (or triggering `workflow_dispatch` manually in the GitHub Actions tab) runs all unit tests and uploads `PayWise-PNG-debug-apk` as a downloadable artifact.
