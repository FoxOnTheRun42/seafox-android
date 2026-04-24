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

Client-Seam im Code: `BillingRestoreCoordinator` erzeugt aus Play-Restore-Daten `BillingValidationRequest`s, behandelt fehlende Serverantworten als `unverified`, merged Serverentscheidungen zurueck in `BillingPurchaseRecord`s und ruft danach `BillingEntitlementMapper.restoreFromPurchases(...)` auf. Der Coordinator ist absichtlich noch kein Netzwerkclient.

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
- Unacknowledged Tokens werden an den Billing-Gateway-Acknowledge-Pfad gemeldet.
- App-Abo-Tiers duerfen niemals C-Map/S-63-Kartenlizenzen implizit freischalten.
- First-party Kartenpakete duerfen nur `ownedChartPackIds` setzen; sie schalten keine App-Stufe und keine externen Kartenprovider frei.
- C-Map/S-63 bleiben externe, inaktive Platzhalter bis Vertrag, Zertifikate, Permit-Handling und Entitlement-Abrechnung stehen.
