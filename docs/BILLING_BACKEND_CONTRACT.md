# seaFOX Billing Backend Contract

Stand: 2026-04-24

Ziel: App-Abos duerfen erst nach Servervalidierung als Entitlement gelten. Kartenlizenzen bleiben rechtlich getrennt.

## Client Inputs

- `packageName`: `com.seafox.nmea_dashboard`
- `productIds`: Play-Produkt-IDs aus `BillingCatalog`
- `purchaseToken`: Google-Play-Kauftoken
- `purchaseState`: `purchased`, `pending` oder `unspecified`
- `acknowledged`: Client-Status aus Play Billing

## Server-Verifikation

Der Server prueft das Token gegen die Google Play Developer API fuer Subscriptions/In-App Purchases. Der Client darf `EntitlementSnapshot` nur aus Records mit `verificationStatus = verified` ableiten.

Empfohlene Antwort:

```json
{
  "verificationStatus": "verified",
  "tier": "NAVIGATOR",
  "validUntilEpochMs": 1770000000000,
  "licensedChartProviderIds": []
}
```

## Sicherheitsregeln

- `pending` oder `unverified` gewaehrt keine Features.
- `rejected` wird geloggt, aber nicht freigeschaltet.
- Unacknowledged Tokens werden an den Billing-Gateway-Acknowledge-Pfad gemeldet.
- App-Abo-Tiers duerfen niemals C-Map/S-63-Kartenlizenzen implizit freischalten.
- C-Map/S-63 bleiben externe, inaktive Platzhalter bis Vertrag, Zertifikate, Permit-Handling und Entitlement-Abrechnung stehen.
