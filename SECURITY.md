# Security Policy

## Supported State

seaFOX is currently pre-release software. Security and safety issues are
triaged for the active `main` branch only.

## Reporting

Do not create public GitHub issues for vulnerabilities, secrets, signing
material, location privacy problems, billing bypasses, or safety-critical
autopilot behavior.

Report privately to the repository owner until a dedicated security contact is
published in the production support materials.

## Sensitive Areas

- Release signing files, passwords, API tokens, Play Billing credentials, and
  map-provider credentials must never be committed.
- Boat identity, MMSI, route history, MOB events, router hosts, backup exports,
  and support diagnostics are treated as sensitive data.
- Autopilot command paths require explicit opt-in, visible status, confirmation,
  command IDs, and timeout/error handling.

## Current Gaps

- No public bug bounty or SLA exists yet.
- Crash reporting is local-only until a privacy-reviewed provider is selected.
- Store release credentials and server-side billing validation are not part of
  this repository.
