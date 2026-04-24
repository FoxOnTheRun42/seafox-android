# seaFOX Parallel Run 20260404-195241

## Goal

Raise the chart/navigation subsystem toward the project codex target with autonomous parallel execution.

## User Segment

Skippers using older Android tablets on board with unreliable or absent internet and long-running 24/7 operation requirements.

## Hard Constraints

- Android app only in this repo
- minSdk 24, compileSdk 35
- offline-first behavior required
- no guaranteed `gradle` binary in current workspace
- avoid touching neighbor projects
- keep work aligned with existing Compose + SharedPreferences + manual JSON patterns

## Current Acceptance Target

- strengthen ENC rendering correctness
- improve chart data selection behavior
- expand chart overlays toward missing navigation features
- preserve existing widget settings flow and MapLibre integration

## KPI Targets

- reduce clearly wrong ENC polygon assembly cases versus current linear edge concatenation
- make zoom-level rendering load fewer low-value SCAMIN features
- render at least the baseline own-ship navigation vectors on chart
- add at least one more chart intelligence increment in parallel lanes

## Parallel Lanes

1. ENC source selection and CATALOG.031-aware prioritization
2. AIS overlay upgrade with motion vectors / CPA visuals
3. Navigation/state expansion without conflicting write scopes
4. Orchestrator integration and quality gate review

## Gate Status

- Gate 1 Strategy Lock: pass
- Gate 2 Blueprint Lock: pass
- Gate 3 Build Lock: in progress
- Gate 4 Hardening Lock: blocked by missing local Gradle toolchain
