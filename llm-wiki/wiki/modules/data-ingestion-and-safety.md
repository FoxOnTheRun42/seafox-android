---
title: Data Ingestion and Safety
type: module
status: current
updated: 2026-04-24
sources:
  - ../sources/product-safety-refresh-2026-04-24.md
  - ../../../AGENTS.md
  - ../../../README.md
  - ../../../docs/PRODUCTION_READINESS.md
  - ../../../docs/QA_MATRIX.md
  - ../../../docs/PRIVACY_POLICY_DRAFT.md
  - ../../../docs/SAFETY_DISCLAIMER_DRAFT.md
  - ../../../app/src/main/AndroidManifest.xml
  - ../../../app/src/main/java/com/boat/dashboard/BootAutostartPolicy.kt
  - ../../../app/src/main/java/com/boat/dashboard/BootCompletedReceiver.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/NmeaNetworkService.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/AutopilotSafetyGate.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/BackupPrivacyPolicy.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/SupportDiagnostics.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/EntitlementModels.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/FeatureAccessPolicy.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/BillingCatalog.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/BillingEntitlementMapper.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/PlayBillingClientGateway.kt
  - ../../../app/src/main/java/com/boat/dashboard/CrashReporting.kt
  - ../../../app/src/main/java/com/boat/dashboard/ui/DashboardViewModel.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/BootAutostartPolicyTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/data/BillingCatalogTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/data/BillingEntitlementMapperTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/CrashReportFormatterTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/data/FeatureAccessPolicyTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/data/SupportDiagnosticsBuilderTest.kt
---

# Data Ingestion and Safety

## Summary

seaFOX sammelt Borddaten ueber Netzwerk, GPS, BLE und Simulation. Weil es ein Marine-Kontext ist, sind Safety-Gates, Datenschutz, Diagnose-Redaktion und ehrliche Produkt-/Billing-Aussagen Produktbestandteile, nicht nur technische Details.

Stand 2026-04-24: Die Domainlogik fuer Autopilot Safety Gate, Backup Privacy, Boot-Autostart-Opt-in, Support Diagnostics, Entitlements, Feature Access, Billing-Restore-Mapping und lokale Crash-Reports ist sichtbar und getestet. Die groessten Product/Safety-Risiken liegen nicht in fehlender Syntax, sondern in Runtime-Truth: Boot-Autostart ist im Code gegen Boot, Unlock und internen Delayed Launch gehaertet, aber noch nicht auf Device/Emulator bewiesen; Entitlements sind noch nicht an Kauf-UI/Backend-Receipt-Validierung/UI-Laufzeitgates angeschlossen; Support Diagnostics hat JSON-/Datei-Export im Datenlayer, aber keinen nachgewiesenen Nutzer-Export-/Share-Flow.

Seit Chart Roadmap Task 03 unterscheidet `EntitlementSnapshot` auch eigene Kartenpakete (`ownedChartPackIds`) von externen Provider-Lizenzen (`licensedChartProviderIds`). Das erste first-party Pack ist `seafox-premium-de-coast` ueber Play-`INAPP` `seafox.chartpack.de_coast`.

## Data Sources

- UDP/TCP via `NmeaNetworkService`, Default-Port `41449`.
- Android GPS via Location APIs.
- BLE for Daly BMS via `DalyBmsBleManager`.
- Integrated NMEA simulator.
- AIS position reports and CPA/TCPA-related state.

## Accepted Simple Network Formats

Aus `README.md`:

- JSON: `{"battery_soc":85,"wind_speed":14.2,"wind_angle":47}`
- Key-value: `battery_soc=85;wind_speed=14.2;wind_angle=47`

## Important PGNs

- `129025`, `129029`: Position / GNSS
- `129026`: COG and SOG
- `127250`: Vessel Heading
- `130306`: Wind Data
- `127245`: Rudder
- `127489`: Engine Speed
- `127506`: Fluid Level
- `127508`: Battery
- `127237`: Autopilot
- `129038`, `129039`: AIS Position Reports

## Safety and Privacy Facts

- Autopilot commands route through `AutopilotSafetyGate.evaluate` in `DashboardViewModel.sendAutopilotCommand` before any UDP/SignalK/NMEA0183 dispatch.
- The gate blocks when `safetyGateArmed` is false, when the target host is blank/broadcast/localhost-like, when the port is outside `1..65535`, when confirmation is missing, or when confirmation is older than the request timeout.
- A confirmed command to an explicit host is accepted only while the confirmation is fresh. Rejected commands update telemetry text with safety state, status and command id instead of dispatching.
- Backups default to `BackupPrivacyMode.privateOnly`. Public backup/export paths are allowed only for `manualExport` or `cloudAllowed`.
- `BackupPrivacyPolicy.treatsAsSensitive` treats MMSI, route, MOB, router host and gateway host keys as sensitive.
- The privacy draft says dashboard state is local, backups do not leave the app automatically, support data is optional, and no default cloud account/upload should be assumed.
- The safety disclaimer draft says seaFOX is not ECDIS or an official navigation system; GPS, AIS, NMEA, BLE, network and chart data can fail or be wrong.

## Current Release Risks

- **Boot autostart is code-gated but needs device proof:** `BootCompletedReceiver` delegates Boot, Locked-Boot, User-Unlocked and the internal delayed action to `BootAutostartPolicy.decide`. Disabled or missing state returns `skipDisabled`; enabled Boot schedules a delayed launch; enabled Unlock/internal actions launch immediately. `BootAutostartPolicyTest` covers the bypass case that previously let Unlock/internal paths skip the opt-in. Remaining risk: no real Android boot/unlock/device QA has been run locally because `adb` is unavailable.
- **Entitlements are not fully runtime-enforced:** `EntitlementPolicy` models `Free`, `Pro`, `Navigator` and `Fleet`, separates first-party chart packs from licensed chart providers, and blocks expired snapshots. `PlayBillingClientGateway` and `BillingEntitlementMapper` prepare restore/acknowledge plus verified entitlement mapping, but purchase UI, backend receipt validation, trial rules and UI/action enforcement remain open.
- **Billing catalog now distinguishes app subscriptions, first-party chart packs and external placeholders:** `BillingCatalog` defines active app-subscription product ids for `Pro`, `Navigator` and `Fleet`, plus `seafox.chartpack.de_coast` as an active first-party Play-`INAPP` that grants only `ownedChartPackIds`. Chart-license products for C-Map and S-63 are inactive external placeholders and do not grant app tiers, first-party packs or chart provider licenses.
- **Support diagnostics have data-layer JSON/file export only:** `SupportDiagnosticsBuilder`, `SupportDiagnosticsJson.toMap`, `toJsonString` and `SupportDiagnosticsExporter.writeReport` exist and are covered by JVM tests. `writeReport` writes a `seafox-diagnostics-{createdAt}.json` file into a supplied directory and creates that directory if needed. Search in current app UI/ViewModel sources found no user-facing export/share entry point yet, so product docs should keep this as a prepared utility, not a proven support workflow.
- **Device proof is missing:** The product check passed in the CEO sync, but `adb` was unavailable. Boot autostart, emulator flows, device GPS/BLE and hardware-near autopilot behavior remain unverified locally.

## Entitlement and Billing Truth

- `EntitlementSnapshot` contains an app tier, optional owned first-party chart pack ids, optional licensed chart provider ids and optional expiry.
- Free keeps simulator, basic dashboard, online charts and MOB available.
- Pro adds offline packages, full widget configuration, routes/tracks, AIS CPA, anchor watch, laylines and trend curves.
- Navigator adds safety contour, advanced alarms, import/export and support diagnostics.
- Fleet adds fleet management.
- Expired snapshots grant nothing.
- App subscription products are active in the catalog: `seafox.pro.monthly`, `seafox.pro.yearly`, `seafox.navigator.monthly`, `seafox.navigator.yearly`, `seafox.fleet.monthly` and `seafox.fleet.yearly`.
- First-party chart-pack product `seafox.chartpack.de_coast` is active as a Play `INAPP` and maps to `ownedChartPackIds = ["seafox-premium-de-coast"]`; it does not grant a subscription tier or external provider license.
- Commercial chart-license placeholders are inactive: `seafox.chart.cmap.external` and `seafox.chart.s63.external` return no app tier and no chart provider license. They must not be marketed as currently sellable or runtime-enabled.
- `BillingCatalog` normalizes product lookup by trim/lowercase and exposes `activeProducts()`, `tierForProductId()`, `chartPackForProductId()`, `chartProviderForProductId()`, `activeSubscriptionProductIds()` and `activeInAppProductIds()`.
- `PlayBillingClientGateway` uses Google Play Billing for subscription restore, one-time in-app-product restore and acknowledge paths.
- `BillingEntitlementMapper` grants tiers only from verified `purchased` records; pending, unverified and rejected purchases do not grant access.
- `BillingEntitlementMapper` grants first-party chart-pack ownership only from verified `purchased` records; pending, unverified and rejected pack purchases do not grant ownership.
- Unacknowledged verified purchase tokens are surfaced for acknowledge handling.

## Support Diagnostics Truth

- `SupportDiagnosticsBuilder.build` summarizes app version, Android SDK, creation time, privacy mode, boot autostart, simulation, NMEA protocol/host/port, page count, widget count, active route, MOB marker and detected source count.
- Router host is redacted by default as `<redacted>` unless `includeSensitive = true`.
- JSON serialization is stable through `SupportDiagnosticsJson.toMap` and `toJsonString`; tests assert stable fields such as `appVersionName`, `androidSdk`, `createdAtEpochMs`, `backupPrivacyMode`, `nmeaRouterProtocol`, `nmeaRouterHost` and `udpPort`.
- `SupportDiagnosticsExporter.writeReport` writes `seafox-diagnostics-{createdAt}.json` to a supplied directory and creates that directory when needed.
- Missing product surface: a gated export/share UI, user consent copy, support destination and triage workflow are not proven in the current app sources.

## Crash Reporting Truth

- `LocalCrashReporter.install` is called from `SeaFoxApplication.onCreate`.
- Uncaught exceptions are written as private app files under `crash-reports/`.
- `CrashReportFormatter` records app version, Android SDK, timestamp, thread, throwable class/message and stacktrace.
- No external crash-reporting SDK or cloud upload is active. A support/share UI for crash reports is still missing.

## Boot Autostart Truth

- `DashboardViewModel.updateBootAutostartEnabled` persists the user's setting into `DashboardState`.
- `DashboardRepository` serializes `bootAutostartEnabled`; `BootCompletedReceiver` reads it directly from the same `dashboard_state_json` SharedPreferences payload.
- `BootAutostartPolicy` centralizes decisions for Boot, Locked-Boot, User-Unlocked and the internal delayed launch.
- The boot/locked-boot path respects disabled autostart before scheduling.
- `AndroidManifest.xml` registers `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `USER_UNLOCKED` and the internal `com.seafox.nmea_dashboard.action.AUTOSTART_INTERNAL` action for `BootCompletedReceiver`.
- The unlock/internal paths now re-check the same policy before launching. Until device-tested, boot autostart remains runtime-unverified, but the known code-level bypass is fixed.

## QA Hooks

- JVM tests for parsers, `BootAutostartPolicy`, `AutopilotSafetyGate`, `BackupPrivacyPolicy`, `SupportDiagnostics`, `CrashReportFormatter`, `EntitlementPolicy`, `FeatureAccessPolicy`, `BillingCatalog` and `BillingEntitlementMapper`.
- Device or bench tests for real autopilot/BLE behavior.
- Emulator permission tests for GPS.
- Replay tests for NMEA UDP/AIS.
- Manual or instrumented boot/unlock tests proving disabled autostart cannot launch the app.
- UI test or manual proof for a support diagnostics export/share flow once wired.

## Open Questions

- Welche NMEA-Replay-Fixtures sollen als erste stabile Regression Suite dienen?
- Wie wird Support-Diagnose exportiert und triagiert, ohne sensible Daten zu verlieren oder ungewollt zu teilen?
- Welche Hardware-Bench-Checkliste ist fuer Autopilot-Kommandos minimal notwendig?
- Welche Device-/Emulator-Bootprozedur beweist, dass `BootAutostartPolicy` auf echten Android-Versionen nicht durch OEM-Verhalten umgangen wird?
- Wo wird die erste runtime-faehige Entitlement-Pruefung verankert, ohne App-Subscriptions mit externen Kartenlizenzen zu vermischen?
