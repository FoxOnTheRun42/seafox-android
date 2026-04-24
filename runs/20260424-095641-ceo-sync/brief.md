# seaFOX CEO Sync - 2026-04-24 09:56

## Situation

Two active work lanes appear to be converging in the same app surface:

- Productization / safety lane: onboarding, backup privacy, boot autostart, entitlement policy, support diagnostics, autopilot safety gate, production docs.
- Chart / navigation lane: chart provider registry, safety contour policy, hazard overlay wrappers, route/MOB/safety contour contract wiring, fullscreen chart controls.

The central product gate was run after the latest local changes:

- `./scripts/seafox-product-check.sh --ci`
- Result: pass for static health, `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, and `:app:lintDebug`.
- Known gap: `adb` is not installed, so emulator/device QA did not run.

## CEO Decision

Stop adding broad new product surface until the two lanes close their integration proof. The code compiles and unit tests pass, but the current release risk is not syntax; it is feature truthfulness and runtime proof.

## Lane A - Productization / Safety

Owner focus:

- Keep safety/commercial claims honest in UI and docs.
- Finish user-visible flows for privacy and support diagnostics.
- Ensure entitlements are domain logic only until Billing exists.

Observed completed work:

- First-run safety/onboarding dialog is wired in `MainActivity`.
- Backup privacy and boot autostart settings persist through `DashboardViewModel` and repository state.
- Autopilot dispatch runs through `AutopilotSafetyGate`.
- Entitlement tiers and chart-license separation have JVM tests.
- Support diagnostics redacts sensitive router host by default and has JVM tests.

Next assignment:

1. Add or document the actual support-diagnostics export entry point. The builder exists, but the user-facing export path is not proven yet.
2. Add a short "domain only / no Billing yet" note wherever entitlement status can be misunderstood.
3. Confirm boot autostart behavior on device or leave it explicitly as unverified in QA.

Do not take ownership of chart rendering internals except wording, disclaimers, and release-gate docs.

## Lane B - Chart / Navigation

Owner focus:

- Convert chart promises into rendered, testable behavior.
- Keep provider availability aligned with what the app can really load.
- Prove safety contour behavior against fixture-like data before calling it feature-complete.

Observed completed work:

- `ChartProviderRegistry` gates C-Map/S-63 and exposes only selectable/beta providers.
- `SafetyContourPolicy` and `HazardOverlayBuilder` provide depth filtering and contract helpers.
- `NavigationOverlay` renders route, MOB, guard zone, laylines, and a safety-contour contract feature.
- `ChartWidget` wires active route/MOB/settings/fullscreen and OpenSeaMap overlay.

Next assignment:

1. Replace or supplement the safety-contour placeholder with a real ENC/GeoJSON-derived visual path, or clearly label it as a contract-only marker.
2. Add focused tests for `HazardOverlayBuilder.filterDepthAreaFeatures` and the placeholder/contract feature shape.
3. Produce one reproducible chart fixture or screenshot plan for NOAA/S-57 depth features.

Do not change product pricing, entitlement tiers, or privacy UX.

## Shared Integration Rules

- No overlapping edits in `MainActivity.kt` without calling out the exact section first.
- Product lane owns docs, safety copy, privacy/support UX, entitlement domain.
- Chart lane owns `ui/widgets/chart/**`, chart settings behavior, route/MOB/chart visual proof.
- Any new marine-safety claim must have either a JVM test plus fixture plan, or stay marked beta/unverified.
- Before final handoff, rerun `./scripts/seafox-product-check.sh --ci`.

## Current Blockers

- Emulator/device QA blocked locally because `adb` is unavailable.
- Safety contour is still partly a contract/placeholder path, not yet a proven real ENC contour renderer.
- Support diagnostics builder exists, but export UX/workflow still needs proof.
- Play Billing, signing, release store flow, and licensed chart-provider integrations are still future work.

## Integration Scout Findings

The read-only scout confirmed these release risks:

1. `BootCompletedReceiver` can launch on `ACTION_USER_UNLOCKED` without checking `bootAutostartEnabled`.
2. `EntitlementPolicy` is domain-only; app state and production call sites do not enforce premium gates yet.
3. Safety contour currently renders a placeholder/contract feature, not a proven ENC-derived contour.
4. `ChartProvider` abstraction exists, but runtime still mostly uses the legacy enum/download path.
5. `SupportDiagnosticsBuilder` is tested, but there is no user-facing export/share workflow yet.

## Updated Two-Agent Dispatch

Agent A now owns the safety/commercial truth lane:

- Fix or verify `BootCompletedReceiver` so disabled boot autostart cannot launch the app after unlock.
- Add an entitlement snapshot to app state or explicitly mark all entitlement code as non-enforcing until Billing work starts.
- Wire support diagnostics into a gated export/share path, or mark it as domain-only in product docs.

Agent B now owns the chart truth lane:

- Make safety contour honest: either connect the depth filtering path to real ENC/GeoJSON rendering or label the existing marker as a placeholder contract.
- Bridge `ChartProvider` into the current runtime selection path, or document it as architecture-only until provider migration.
- Add fixture/screenshot proof for one real chart/depth scenario before calling the chart lane release-ready.
