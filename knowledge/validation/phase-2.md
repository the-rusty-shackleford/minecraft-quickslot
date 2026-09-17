# Phase 2 validation — 2026-09-17

Status: survival/adventure inventory cell and HUD verified for the stated scope. Unreleased.
Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21. Phase 1 source checkpoint: b191623.

## Gates run

- Build passed: 19 plain JUnit tests remain green, and all 19 real-server GameTests passed.
  The seven new menu tests exercise real `InventoryMenu.clicked`: pickup/place/split,
  shift in both directions, number-key exchange, full-inventory failure, armor/offhand
  priority, working-stack ownership, external/clone refresh and the creative guard.
- The 16 dedicated-server/two-client Phase 1 regressions passed with the Phase 2 code and
  Sophisticated Backpacks 3.25.78.2107, Sophisticated Core 1.4.89.2291 and Curios 9.5.1
  added to all three isolated profiles. Stowed remained absent.
- Seven further two-client checks passed: opening inventory through its key and placing a
  full stack through actual mouse coordinates; split and one-item insertion; shift both
  directions; number-key exchange; a real dropped item entity; HUD capture in both arm/
  attack settings; creative screen usability and authoritative H swapping.
- Strict mypy and Ruff passed for both Python controllers before the first menu run.
  The corrected controller preserves vanilla's merge-first behavior; its final checks
  are recorded separately from the already-passing 16 regression checks.

The first menu controller incorrectly expected shift-click to create a separate main-
inventory stack. Server evidence showed all 17 apples correctly merged into the existing
hotbar stack. Only the test expectation changed; the same running clients were reused.

## Visual evidence

The actual inventory image shows the new cell directly above offhand, with count 17 and
the Curios button clear of it. Recipe-book position updates use the current GUI origin.
The HUD images show durability and correct left/right placement. A subsequent real attack
key capture shows the active attack indicator and quick slot separated on both sides.
The creative inventory tab was also opened and inspected: the unsupported extra cell is
inactive, leaving the ordinary inventory/offhand/armor/trash controls intact.

The recipe-book capture has a vanilla movement-tutorial toast covering part of the upper
inventory. It establishes the shifted screen geometry, but an unobscured follow-up capture
should be taken with tutorialStep:none during the body-render booth. Do not treat the
covered portion as visually verified. Inventory/HUD screenshots use vanilla GUI assets;
they say nothing about the future Refined Tools body models or shader appearance.

Scratch evidence: `phase2-build.log`, `phase2-network-results.log` (16 regressions followed
by the bad merge expectation), `phase2-menu-results.log` (seven passing corrected checks),
`network-phase2-menu-final/results.json`, and `run/network-driver/screenshots/phase2-*.png`.
Test adapters remain outside the production artifact. The controllers closed their owned
clients and servers on success. Production and the personal Prism profile remain untouched.

## Manual checks and limits

Try normal click, right-click, shift-click and a hotbar number key on the cell; repeat with
a damaged/named tool, a full inventory, the recipe book open, and Curios installed. Flip
the main arm and enable the hotbar attack indicator. A successful H swap briefly highlights
the HUD cell. Full-pack customized button offsets, controller inputs, HUD config, body
placement, Refined Tools and shaders remain later gates.

Creative players currently use H to edit Quick Slot. The clickable creative cell is
deliberately inactive because vanilla's creative stack-creation packets only admit indices
1..45. Supporting it requires a separate design that preserves the brief's server-owned
item contract; no unvalidated client stack-creation extension was added.

Replay the combined real-client gate with `uv run --no-project python devtools/network_menu_test.py`
after preparing the three isolated profiles described in phase-1.md. Preserve or move prior
screenshots before replay; the script refuses to overwrite them.
