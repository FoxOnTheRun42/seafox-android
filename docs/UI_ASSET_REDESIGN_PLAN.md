# seaFOX Premium UI/Asset Redesign Plan

Goal: redesign the visual layer of seaFOX from the ground up without changing behavior. The app should feel like a premium marine cockpit: calm, expensive, precise, readable under stress, and intuitive on a tablet at sea.

## Design Direction

- Product feel: professional navigation instrument, not a generic dashboard.
- Visual language: matte graphite, mineral glass, warm brass/coral safety accents, cold cyan signal accents, restrained light theme.
- Density: keep the existing dashboard power-user layout, but make surfaces quieter and hierarchy clearer.
- Motion/interaction: no functional changes in this pass; edit handles and controls receive more polished states.
- Asset rule: use generated raster images only where they add premium material quality. Keep icons, gauges, charts, and instrument marks code-native for sharpness.

## Generated Asset Inventory

### Foundation Surfaces

1. `seafox_dashboard_field_dark.png`
   - Use: dark dashboard background behind all widgets.
   - Prompt intent: subtle luxury nautical instrument texture, midnight graphite, faint bathymetric/chart contours, no text, no symbols.
   - Size: 2048x2048, repeat-tolerant.

2. `seafox_dashboard_field_light.png`
   - Use: light dashboard background.
   - Prompt intent: warm off-white marine chart paper with polished instrument-panel grain, subtle contour lines, no labels.
   - Size: 2048x2048, repeat-tolerant.

3. `seafox_panel_shell_dark.png`
   - Use: widget panel underlay and dialog surface reference.
   - Prompt intent: dark translucent mineral glass with brushed carbon depth and edge bloom, no text.
   - Size: 1536x1024.

4. `seafox_panel_shell_light.png`
   - Use: light widget panel reference and light dialogs.
   - Prompt intent: frosted ivory glass, satin aluminum microtexture, soft edge illumination, no text.
   - Size: 1536x1024.

### Brand/App Polish

5. `seafox_launcher_mark_premium.png`
   - Use: future launcher/social/app-store source asset, not wired into the adaptive icon in this pass unless validated.
   - Prompt intent: premium abstract seaFOX compass mark, no words, simple enough to trace later.
   - Size: 1024x1024.

6. `seafox_empty_state_navigation.png`
   - Use: empty-state background/illustration for no-page state.
   - Prompt intent: premium cockpit navigation table, chart and compass vibe, no text, quiet negative space.
   - Size: 1536x1024.

### Future Expansion Backlog

7. Widget micro-illustrations: battery, tanks, wind, compass, AIS, autopilot, echosounder, anchor, NMEA.
   - Use only if a screen needs illustrative onboarding or marketplace-like widget previews.
   - The live widgets remain code-native for readability.

8. Weather/sea-state pack: day, night, fog, rain, offshore, harbor.
   - Only useful if the app gains a contextual theme or launch screen later.

9. Store/marketing screenshots and hero backgrounds.
   - Out of app runtime unless a release page is built.

## UI Implementation Plan

1. Replace the current black/white global surfaces with premium dark and light color schemes.
2. Add a layered dashboard background that uses the generated field assets plus subtle Compose overlays.
3. Redesign the top bar:
   - Compact glass instrument rail.
   - Clear centered page title.
   - Smaller version metadata.
   - More tactile menu affordance.
4. Redesign widget frames:
   - Glass/mineral panels with restrained borders.
   - Strong alarm state without visual panic unless active.
   - Better header hierarchy and icon/menu affordance.
5. Redesign dialogs/menus:
   - Premium card container.
   - Consistent dividers, text color, and control accents.
   - Preserve compact behavior and all existing options.
6. Refine chart overlay controls:
   - Pill controls with translucent glass.
   - Better AIS count chip.
   - Clearer MOB danger state.
7. Keep all sensor logic, layout logic, gestures, NMEA parsing, chart logic, and settings unchanged.

## Generation Policy

- Generate the first six assets with `gpt-image-2`.
- Use high quality for the foundation assets.
- Save API originals under `output/imagegen/premium-ui-assets/`.
- Copy runtime assets into `app/src/main/res/drawable-nodpi/`.
- Do not generate hundreds of files by default. Expand only when a specific screen needs a specific asset, because thousands of runtime images would slow the app, bloat the APK, and make visual QA worse.

Ready-to-run prompt manifest:

- `docs/premium-ui-assets.gpt-image-2.jsonl`

CLI command, once the API billing limit is available again:

```bash
export CODEX_HOME="${CODEX_HOME:-$HOME/.codex}"
python "$CODEX_HOME/skills/.system/imagegen/scripts/image_gen.py" generate-batch \
  --input docs/premium-ui-assets.gpt-image-2.jsonl \
  --out-dir output/imagegen/premium-ui-assets \
  --model gpt-image-2 \
  --quality high \
  --output-format png \
  --concurrency 3 \
  --max-attempts 3
```
