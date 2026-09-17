# Phase 3 rendering checkpoint — 2026-09-17

Initial player layer, classification and measured body fit are implemented. Unreleased.
This checkpoint does not claim the later equipment/pose/shader clearance gates passed.

## Implementation and evidence

- Both player renderers receive a body-anchored layer. Invisible/spectator players and
  hidden categories are skipped. Synced stack identity controls one-item display copies.
- The renderer measures actual emitted vertices once per stack/resource change in NONE
  context. This follows Modefite's Refined Tools 3D selection, including Fire Aspect,
  and the custom shield/trident paths. Fits use a bounded diagonal and measured broad face.
- Tags precede fallback rules; food precedes BlockItem, named block items remain belt
  items, and ordinary blocks/armor stay hidden. Optional feast overrides hide cake and FD
  feast blocks. Torches/lanterns are visible exceptions, without quick-slot lighting yet.
- Bottle/pouch/bowl/hang styles use small carrying hardware, with flat sprites parallel
  to the surface. The holder meshes are original code geometry using a vanilla texture.
- A real enchanted-sword run reproduced `IllegalArgumentException: Duplicate delegates`
  from the measurement's glint pass. Distinct stream wrappers now share one accumulator;
  repeat captures show correct scale and no measurement warning on either client.
- Side views exposed edge-on Refined bows/crossbows. Aligning the measured broad face
  fixed both. Shield depth is reduced so its handle does not hold the plate far off the back.

26 JUnit tests pass, including crop/block/tag precedence, styles, oversized/invalid bounds
and all authored face planes. The last full build passed all 19 real-server GameTests.
Strict mypy and Ruff passed for the booth controller. Production rendering is client-only;
these domain/server tests do not substitute for the visual runs below.

## Real-client visual runs

Two actual clients connected to the reused isolated dedicated-server profiles. Refined Tools
3.0 and the installed Modefite 1.0.1+1.21.1 were copied into the test clients only. No artwork
was added to the mod. Stowed remained absent. SB/Core/Curios and VW/TB were present; their
presence establishes loading compatibility, not worn-backpack clearance.

Captured front, rear and both sides for sword, pickaxe, trident, shield, potion, apple and
lantern, then revised orientation/holders. Later captures cover bow, crossbow, mace, shears,
torch, bowl, milk bucket, carrot, berries, seeds, redstone, string, cake, chestplate and stone.
The final broad-face pass rechecks bow/crossbow/shield/bowl/sword from all four directions.

QuickDriver and QuickObserver naturally select SLIM. Reusing the observer profile with
`-PquickslotObserverName=QuickViewer` selects vanilla WIDE, verified from the running client.
The wide renderer, local third-person camera, mirrored main arm and running pose were captured.
The same connected observer switched Refined Tools off and on through resource reload:
the stored sword changed to the vanilla sprite and back to the 3D model without a relog.
Real H input cleared the back while drawing, and restored it while sheathing.

The Phase 2 recipe-book follow-up is now unobscured: `phase3-inventory-state-b.png` shows
the expanded recipe panel with the quick cell, offhand and Curios button clear of controls.
`phase3-recipe-book-unobscured.png` actually shows the closed-panel state and must not be
mislabelled as an expanded recipe-book capture.

## Remaining gates and manual checks

The crouching side capture shows the upper sword handle intersecting the head. This is a
known Phase 4 clearance defect, not accepted appearance. Armor, elytra, cape, visible SB/Curios
packs, swimming/gliding/sleeping/riding and shader shadows remain unverified. The full installed
mod list and oversized real modded weapons also remain later compatibility gates. Custom
renderers that do not emit ordinary vertices use a logged fallback and need item overrides.

Try H with a sword, pickaxe, bow, shield, torch and a stack of food. Look from both sides and
at normal third-person distance; flip the main arm and reload Refined Tools. Confirm one item
per stack and no residual back item while it is held. Equipment combinations may still clip.

Evidence is in ignored `run/network-*/screenshots/phase3-*` and session scratch logs named
`phase3-*`. `devtools/body_booth.py` drives the reusable capture path. The initial and intermediate
captures intentionally retain failures for comparison; use the final/plane captures for the
current standing fit. All release and production pack changes remain HELD.
