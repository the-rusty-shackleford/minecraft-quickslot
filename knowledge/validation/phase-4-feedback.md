# Approved-fit feedback verification — 2026-09-17

Implements D-0006; supersedes the automatic-clearance appearance in phase-4.md.
Development only, releases HELD. No production or personal Prism profile was changed.

## Current behavior

The default back/belt transforms are restored from Phase 3 commit 0cfea66, the source of
the gallery's Earlier model checks. Actual Refined Tools models, normalization, broad-face
orientation, glint handling and manual placement resources remain. Automatic armor offsets,
head collision displacement and head-pose hiding are removed, including their unused mesh
cache and three geometry tests. Minor hair/helmet intersections are explicitly accepted.

Bowl meals always have a hidden body display. This includes bowls identified by tags,
crafting remainders or food conversion remainders. A placement resource cannot force a
recognized bowl meal into a pouch. Empty bowls remain ordinary components.

Every quick-slot body display is hidden while elytra is equipped, including belt items,
food, potions, ingredients and lanterns. There are no elytra alternate positions. H swaps
and stack storage remain independent of body rendering. Removing elytra restores the
display for items that are otherwise visible.

## Executed checks

- 26 JUnit tests passed, including the revised unconditional bowl classification partitions.
  The lower count reflects removal of the three obsolete collision tests, not skipped tests.
- All 19 required dedicated-server GameTests passed.
- Strict mypy and Ruff passed for the real-client booth controllers.
- Two reused real clients, muted, with Refined Tools/Modefite and SB/Core/Curios loaded;
  Stowed absent. The observer uses a wide skin and the driver a slim skin.
- Captured sword, pickaxe, bow, crossbow, shield and trident from rear and side. Inspected
  the restored sword next to the actual earlier reference: size and sling angle match.
- Captured crouched, armored, head-up sword: normal geometry remains rendered; helmet
  occlusion/clipping is accepted and there is no automatic offset or visibility switch.
- Rear/side captures of elytra hiding sword, apple, potion, ingot, shield and lantern.
  Removing elytra makes the lantern visible again.
- All four vanilla bowl meals captured without elytra: mushroom stew, beetroot soup,
  rabbit stew and suspicious stew. Body item and holder are hidden. An explicit hip/pouch
  resource override for mushroom stew still leaves it hidden after a real resource reload.
- H exchanges of the hidden lantern and hidden mushroom stew verified against server and
  both clients: the exact stack draws and stows, with no missing/duplicated item.
- Final normal F5 camera capture confirms the restored everyday scale.

Replay: devtools/feedback_booth.py uses the existing isolated network profiles and refuses
to overwrite prior captures. Both clients and the empty isolated server were cleanly
stopped afterward; a host-level process check found no remaining test Minecraft JVMs.

## Review and remaining work

Review the gallery's Restored fit and Pocket verse tabs. In game, try a bowl meal without
elytra, then a lantern or apple with elytra; H must work in each case. Removing elytra
should restore the lantern/apple display while a bowl meal stays hidden.

Backpack/Curios layering, quick-slot dynamic light, shader/full-pack tests and the remaining
pose matrix still belong to later phases. This feedback pass does not claim those gates.
