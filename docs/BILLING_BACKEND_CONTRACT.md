# seaFOX Billing Backend Contract

Stand: 2026-04-24

Ziel: App-Abos duerfen erst nach Servervalidierung als Entitlement gelten. Kartenlizenzen bleiben rechtlich getrennt.

## Client Inputs

- `packageName`: `com.seafox.nmea_dashboard`
- `productIds`: Play-Produkt-IDs aus `BillingCatalog`
- `purchaseToken`: Google-Play-Kauftoken
- `purchaseState`: `purchased`, `pending` oder `unspecified`
- `acknowledged`: Client-Status aus Play Billing
- Produktarten: App-Stufen laufen als Play `SUBS`; first-party Kartenpakete wie `seafox.chartpack.de_coast` laufen als Play `INAPP`.

Client-Mapping im Code: `PlayBillingPurchaseMapper` uebersetzt echte Play-`Purchase`-Objekte in `BillingPurchaseRecord`s und setzt sie standardmaessig auf `verificationStatus = unverified`. Eine Backend-Entscheidung muss danach explizit ueber den Restore-Coordinator kommen.

## Server-Verifikation

Der Server prueft das Token gegen die Google Play Developer API fuer Subscriptions/In-App Purchases. Der Client darf `EntitlementSnapshot` nur aus Records mit `verificationStatus = verified` ableiten.

Client-Seam im Code: `BillingValidationJson` parst Backend-Antworten nur zu Token-Entscheidungen (`verified`, `rejected`, sonst `unverified`) plus optionalem Ablaufzeitpunkt. `PlayBillingClientGateway` fragt ProductDetails ab, startet den Google-Play-Kaufdialog und leitet `PurchasesUpdated` in dieselbe Validation-Pipeline wie Restore. `BillingRestoreCoordinator` erzeugt aus Play-Restore-Daten `BillingValidationRequest`s, behandelt fehlende Serverantworten als `unverified`, merged Serverentscheidungen zurueck in `BillingPurchaseRecord`s und ruft danach `BillingEntitlementMapper.restoreFromPurchases(...)` auf. `BillingValidationHttpClient` kann diese Requests per POST an `SEAFOX_BILLING_VALIDATION_URL` senden. `BillingRuntimeRestoreApplier` schreibt Restore-/Kaufergebnisse nur dann in den App-State, wenn keine Backend-Validation fehlt; pending oder unverified Kaeufe ueberschreiben bestehende Freischaltungen nicht.

Empfohlene Antwort:

```json
{
  "verificationStatus": "verified",
  "tier": "NAVIGATOR",
  "validUntilEpochMs": 1770000000000,
  "ownedChartPackIds": ["seafox-premium-de-coast"],
  "licensedChartProviderIds": []
}
```

## Sicherheitsregeln

- `pending` oder `unverified` gewaehrt keine Features.
- `rejected` wird geloggt, aber nicht freigeschaltet.
- Fehlende Serverantwort gilt als `unverified` und schaltet nichts frei.
- Ein fehlender `SEAFOX_BILLING_VALIDATION_URL`-Endpoint schaltet trotz Play-Kauf nichts frei.
- Pending-Kaeufe duerfen einen bestehenden lokalen Snapshot nicht downgraden.
- Ein erfolgreicher Restore ohne aktive Kaeufe darf den lokalen Snapshot auf `Free` zuruecksetzen.
- Unbekannte Server-Statuswerte gelten als `unverified`.
- Unacknowledged Tokens werden an den Billing-Gateway-Acknowledge-Pfad gemeldet.
- App-Abo-Tiers duerfen niemals C-Map/S-63-Kartenlizenzen implizit freischalten.
- First-party Kartenpakete duerfen nur `ownedChartPackIds` setzen; sie schalten keine App-Stufe und keine externen Kartenprovider frei.
- C-Map/S-63 bleiben externe, inaktive Platzhalter bis Vertrag, Zertifikate, Permit-Handling und Entitlement-Abrechnung stehen.
