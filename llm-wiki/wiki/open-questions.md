---
title: Open Questions
type: question
status: current
updated: 2026-04-24
sources:
  - ../../docs/PRODUCTION_READINESS.md
  - ../../docs/QA_MATRIX.md
  - ../../docs/RELEASE_CHECKLIST.md
  - ../../docs/PRIVACY_POLICY_DRAFT.md
  - ../../docs/SAFETY_DISCLAIMER_DRAFT.md
  - ../../runs/20260424-095641-ceo-sync/brief.md
  - ../../AGENTS.md
---

# Open Questions

## Product / Release

- Welche minimalen Features definieren die erste verkaufbare Beta?
- Wie werden `docs/SAFETY_DISCLAIMER_DRAFT.md` und `docs/PRIVACY_POLICY_DRAFT.md` juristisch finalisiert und in App/Store uebernommen?
- Welche Impressums-, Kontakt-, Rechtsgrundlagen-, Drittanbieter-, Loesch- und Exportangaben fehlen noch?
- Welche Crash-/Feedback-/Support-Triage wird fuer interne Tests genutzt?
- Wer entscheidet, ab wann `./scripts/seafox-product-check.sh --ci --release-r8` verpflichtend fuer jeden Kandidaten ist?

## QA / Test

- Welche Emulator-Konfiguration soll als Standard-Smoke dienen?
- Welche realen NMEA-Logs duerfen als Regression-Fixtures im Repo liegen?
- Welche Karten-Fixtures sind rechtlich sauber und klein genug fuer Tests?
- Wie wird manuelle Device-QA auf Tablet und Smartphone protokolliert?
- Wie wird Boot-Autostart auf Boot, Locked Boot, User Unlock und internem Alarm-Intent reproduzierbar getestet?
- Welcher Test beweist den user-facing Support-Diagnostics-Export/Share-Flow inklusive Redaction?

## Charts / Navigation

- Welche Kartenquelle wird nach Task 03 als naechster Offline-Schritt priorisiert? Aktuelle Entscheidung: Raster-MBTiles-Sideloading ist angebunden; das erste first-party Premium-Pack ist als Billing-/Statusvertrag modelliert; GeoPackage/Vector-MBTiles sind importierbar, aber noch nicht renderbar.
- Wird GeoPackage zuerst ueber lokalen Tile-Adapter gerendert, beim Import nach MBTiles konvertiert oder bis zur konkreten Provider-Registry verschoben?
- Welche kleine, rechtlich saubere Raster-MBTiles-Fixture darf fuer Emulator-/Screenshot-Tests im Repo liegen?
- Wie stark soll S-52-inspirierte, nicht zertifizierte Symbolisierung vor der ersten Beta sein?
- Welche reale ENC-Zelle dient als Safety-Contour-Referenz?
- Wann wird die bestehende ChartProvider-Abstraktion voll genutzt statt direkter Verdrahtung?
- Wird Safety Contour zuerst als DEPCNT-Linie, shallow DEPARE-Fill, Alarmzone oder separate Warnschicht umgesetzt?
- Welche Route-/MOB-/Layline-Screenshots gelten als minimale Chart-Release-Beweise?

## Architecture

- Welche Teile von `MainActivity.kt` werden zuerst extrahiert, um Aenderungsrisiko zu senken?
- Soll groessere Persistenz fuer Tracks, Routen und Kartenpakete ueber Room/SQLite erfolgen?
- Welche UI-State-Grenzen braucht ein robuster Fullscreen-Chart-Modus?
- Wie wird der vorhandene `Abo & Karten` Restore-Pfad zu Kauf-Flow, Trial, produktivem Backend-Validator und Premium-Pack-Auslieferung erweitert?
- Welche konkrete Klasse wird erster echter `ChartProvider` statt Registry-/Skeleton-Arbeit?

## Business / Licensing

- Welche App-Features werden verkauft, ohne Kartenanbieter-Lizenzen zu beruehren?
- Welche first-party Premium-Kartenpakete duerfen rechtlich verkauft und ausgeliefert werden, und wie werden ihre MBTiles/Assets signiert, versioniert und aktualisiert?
- Welche Provider-IDs gelten als getrennt lizenzierte Module?
- Gibt es eine klare Strategie fuer S-63, C-MAP, oeSENC oder Navionics, bevor sie in UI/Marketing auftauchen?
- Braucht oeSENC einen Vendor-SDK-/Permit-/Plugin-Pfad, oder bleibt es bis zu einem unterschriebenen Partnervertrag komplett ausserhalb von seaFOX?
- Wie bleiben inaktive C-MAP/S-63-Billing-Platzhalter unsichtbar, bis Vertrage, Zertifikate, Permit-Handling und Entitlements stehen?
