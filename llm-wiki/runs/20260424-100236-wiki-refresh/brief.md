# LLM Wiki Refresh - 2026-04-24 10:02

## Goal

Bring `llm-wiki/` to the best current state for future LLM agents working on seaFOX.

## Success Criteria

- Wiki reflects the latest project state and the 2026-04-24 CEO integration findings.
- Each module page has concrete code/source links, current risks, and open checks.
- Index and log are accurate.
- No major orphan/stale page or broken local source reference remains.
- Product/safety/license claims are marked as source-backed or still unverified.

## Constraints

- Markdown wiki only; do not change Android app code.
- Internal repo files are primary sources.
- Keep page ownership disjoint while subagents work.
- No marketing-style claims about marine safety, commercial chart support, or monetization unless the repo proves them.

## Inputs

- `llm-wiki/wiki/**`
- `../AGENTS.md`
- `../README.md`
- `../docs/**`
- `../runs/20260424-095641-ceo-sync/brief.md`
- Key Kotlin files under `../app/src/main/java/com/boat/dashboard/**`

## Parallel Lanes

1. Code freshness audit: identify stale/missing wiki claims against current code and docs.
2. Chart/navigation wiki update: chart-provider, safety-contour, ENC, route/MOB truth.
3. Product/safety wiki update: onboarding, privacy, support diagnostics, entitlements, boot autostart.
4. QA/release wiki update: product gate, tests, device gaps, release gates.
5. Wiki hygiene: index coverage, frontmatter/source paths, wikilinks, open-question priority.

## Final Gate

- `rg -n "TODO|FIXME|needs-check|stale|Contradictions|Open Questions" wiki`
- local source path sanity check
- review `wiki/index.md` and `wiki/log.md`
