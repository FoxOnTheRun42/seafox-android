---
title: Product Safety Refresh 2026-04-24
type: source
status: current
updated: 2026-04-24
sources:
  - ../../../runs/20260424-095641-ceo-sync/brief.md
  - ../../../app/src/main/AndroidManifest.xml
  - ../../../app/src/main/java/com/boat/dashboard/BootAutostartPolicy.kt
  - ../../../app/src/main/java/com/boat/dashboard/BootCompletedReceiver.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/AutopilotSafetyGate.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/BackupPrivacyPolicy.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/SupportDiagnostics.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/EntitlementModels.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/FeatureAccessPolicy.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/BillingCatalog.kt
  - ../../../app/src/main/java/com/boat/dashboard/ui/DashboardViewModel.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/BootAutostartPolicyTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/data/BillingCatalogTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/data/FeatureAccessPolicyTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/data/SupportDiagnosticsBuilderTest.kt
  - ../../../docs/PRIVACY_POLICY_DRAFT.md
  - ../../../docs/SAFETY_DISCLAIMER_DRAFT.md
  - ../../../docs/PRODUCTION_READINESS.md
  - ../../../docs/QA_MATRIX.md
---

# Product Safety Refresh 2026-04-24

## Summary

Diese Source-Notiz ergaenzt die initiale LLM-Wiki-Ingestion um die Product/Safety-Lane aus dem CEO Sync und den nachgezogenen Produktquellen. Sie ist eine Faktensammlung fuer `data-ingestion-and-safety`, keine Codeaenderung am Android-Projekt.

## Findings

- `BootCompletedReceiver` delegiert Boot, Locked-Boot, User-Unlocked und den internen Alarm-Intent an `BootAutostartPolicy`. Der zuvor offene Code-Bypass ist dadurch gehaertet: deaktivierter oder fehlender Opt-in blockiert auch Unlock und den verzögerten internen Launch. `BootAutostartPolicyTest` deckt diese Entscheidung ab. Offen bleibt Device-QA fuer echte Boot-/Unlock-Flows.
- `AutopilotSafetyGate` blockiert nicht armierte Gates, nicht verifizierte Hosts, ungueltige Ports, fehlende Bestaetigungen und abgelaufene Bestaetigungen. `DashboardViewModel.sendAutopilotCommand` dispatcht erst nach positivem Gate-Entscheid.
- `BackupPrivacyPolicy` erlaubt oeffentliche Backups nur fuer `manualExport` oder `cloudAllowed` und behandelt MMSI-, Route-, MOB-, Router- und Gateway-Schluessel als sensibel.
- `SupportDiagnosticsBuilder` redigiert den Router-Host standardmaessig, `SupportDiagnosticsJson` liefert Map/JSON, und `SupportDiagnosticsExporter.writeReport` schreibt `seafox-diagnostics-{createdAt}.json` in ein bereitgestelltes Verzeichnis. Tests decken Builder, JSON und Datei-Export ab; ein UI-/Share-Flow ist in den aktuellen App-Quellen nicht sichtbar.
- `EntitlementPolicy`, `FeatureAccessPolicy` und `BillingCatalog` sind Produkt-/Domainlogik. Aktive Produkte sind nur App-Subscriptions fuer `Pro`, `Navigator` und `Fleet`; C-Map/S-63-Chart-Produkte sind inaktiv, `activeProducts()` enthaelt aktuell nur App-Subscriptions, und inaktive Chart-Placeholder schalten keine App- oder Kartenrechte frei.
- Die Privacy-Policy- und Safety-Disclaimer-Entwuerfe markieren lokale Speicherung, keine Standard-Cloud, manuelle Exporte, redigierte Supportdaten und den Nicht-ECDIS-/Assistenzcharakter als Produktwahrheit.

## Release Implication

Bis Boot-Autostart auf echten Android-Geraeten, Entitlement-Gates und Support-Diagnostics-Export in echten Laufzeitpfaden belegt sind, sollten UI, Store-Texte und Release-Dokumente diese Bereiche als vorbereitet oder runtime-unverified behandeln, nicht als vollstaendig produktiv.

## Related Pages

- [[modules/data-ingestion-and-safety]]
- [[modules/production-and-qa]]
- [[open-questions]]
