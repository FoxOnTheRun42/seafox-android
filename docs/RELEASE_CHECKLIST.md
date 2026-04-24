# seaFOX Release Checklist

Stand: 2026-04-24

Ziel: Jeder Store-Kandidat muss reproduzierbar gebaut, pruefbar signiert, rollback-faehig und ehrlich in Bezug auf Karten-/Safety-Funktionen sein.

## 1. Vorbereitende Gates

- `./scripts/seafox-product-check.sh --ci --release-r8` muss gruen sein.
- Lint darf 0 Errors haben; Warnings muessen bekannt oder als technische Schuld akzeptiert sein.
- JVM-Tests muessen fuer Safety, Privacy, Boot-Autostart, Kartenprovider, Hazard-Depth-Filter, Billing-Katalog, Billing-Restore-Mapping, Entitlements, Feature Access, lokale Crash-Reports und Diagnose-Export gruen sein.
- `adb`-Device-QA muss separat auf mindestens einem Phone und einem Tablet dokumentiert werden.

## 2. Signing

Release-Signing wird nur aktiviert, wenn alle Variablen gesetzt sind:

```bash
export SEAFOX_RELEASE_STORE_FILE=/secure/path/seafox-release.jks
export SEAFOX_RELEASE_STORE_PASSWORD=...
export SEAFOX_RELEASE_KEY_ALIAS=seafox
export SEAFOX_RELEASE_KEY_PASSWORD=...
```

Danach:

```bash
./gradlew --no-daemon :app:bundleRelease
```

Wichtig: `assemble`, `bundle`, `install` und `package` erhoehen die Buildnummer in `version.properties`. Fuer Pruefungen ohne Versions-Increment `compileDebugKotlin`, `testDebugUnitTest`, `lintDebug` oder `minifyReleaseWithR8` nutzen.

## 3. Store-Wahrheit

- seaFOX als Marine-Assistent/Dashboard beschreiben, nicht als zertifiziertes ECDIS.
- S-63/C-Map nicht als kaufbar oder aktiv bewerben, solange Vertrage, Zertifikate, Permit-Handling und Entitlements fehlen.
- Eigenes seaFOX Premium-Kartenpaket nur als first-party Offline-Pack bewerben, wenn Play-Console-`INAPP`, Backend-Verifikation, Auslieferung/Download und lokale Paketvalidierung fuer den Kandidaten nachgewiesen sind.
- DALY-BMS als Beta kennzeichnen, bis BLE-Device-QA abgeschlossen ist.
- Safety Contour nur als Assistenzfunktion bewerben, bis echte ENC-Fixtures und Device-Screenshots vorliegen.

## 4. Device-QA

- Nicht-destruktiver Smoke-Start:

```bash
./scripts/seafox-device-smoke.sh --serial <device-id> --apk app/build/outputs/apk/debug/app-debug.apk
```

- Phone und Tablet, Portrait und Landscape.
- Sonne/Nacht-Kontrast.
- Offline ohne Netzwerk.
- GPS aus/an und Berechtigungen verweigern/erteilen.
- Simulator ein/aus.
- MOB ausloesen und loeschen.
- Fullscreen-Chart oeffnen/schliessen.
- Kartenquelle wechseln.
- NMEA-Router nicht erreichbar.
- Privaten Diagnosebericht erzeugen und Inhalt auf Redaction pruefen.
- Lokalen Crash-Report-Ordner nach Testabsturz/Debug-Drill pruefen, ohne Cloud-Upload.
- Boot-Autostart nur nach bewusstem Opt-in.

## 5. Rollback

- Release-AAB/APK, Mapping-Dateien und `version.properties` zusammen archivieren.
- Letzte funktionierende Version und Store-Track notieren.
- Rollback-Plan vor Rollout festlegen: interner Track, Closed Testing, Prozent-Rollout, Stopp-Kriterium.
