# Phase 4 clearance checkpoint — 2026-09-17

Development only. Releases remain HELD. This is a tested checkpoint, not acceptance of
every pose, shader or equipment combination in the full brief.

## Implemented

- Actual ArmorItem checks, chest/leg inflation, bulky-armor tag and per-item extra offsets.
- Reloadable placement resources with anchor, consumable style, transform and armor factor.
- Lower/wider back sling fitted against the actual Refined Tools and custom-rendered geometry.
- Cached mesh/head intersection tests, using real posed body/head transforms. At most one
  pixel of rearward adjustment; obstructed poses hide the body display instead of floating.
- Elytra rear-hip carry for narrow tools. Shields, bows and crossbows use the hidden fallback.
- Visible capes hide back displays and move lower-back belt displays to the hip.
- Client presentation config. Backpack/light/HUD settings are reserved for their later phases.

Rusty's D-0005 correction permits hidden displays when no natural alternate fit exists.
Storage, server validation, H swaps and eventual luminance are independent of this decision.

## Executed checks

Full build passed 29 JUnit tests and all 19 required dedicated-server GameTests. The three
new geometry tests cover touching/interior faces, crossings with all triangle vertices
outside the box, and disjoint planes/corners. Python controllers pass strict mypy and Ruff.

Two reused real clients on the isolated dedicated server had SB/Core/Curios and
Modefite/Refined Tools installed, with Stowed absent. Captures cover bare/chest/legs/full
armor; standing/crouching; looking up/down; sword/pickaxe/trident/shield/bow/crossbow;
elytra; and potion/apple/lantern armor clearance. Inspected images demonstrate the revised
sling, not just successful compilation. The first rearward-only clearance approach visibly
floated off the back and was rejected. A wider-angle-only trial did not solve extreme head
pitch either. Neither rejected fit is the final policy.

Resource reload was exercised in the running observer: hidden override, hip override,
malformed anchor (logged fallback), deleted override, and restoration without relog.
Client TOML render off/on was also captured through actual file-watcher reload.

The final fallback was exercised with actual H input: a shield hidden by elytra draws into
the selected hand, leaves the slot empty, and swaps back into the hidden slot intact.
An obstructed crouched/head-up sword disappears and returns when the head lowers.
The final sword scale was inspected in the driver's own normal F5 camera as well as the
observer's closer rear/side cameras. This pass used a slim driver and wide observer;
the earlier Phase 3 pass separately rendered both skin widths.

Evidence stays in ignored run/network-*/screenshots and the session scratchpad. The complete
capture controller is devtools/equipment_booth.py. It refuses to overwrite screenshots;
archive prior captures before replay. Both rendering clients and the isolated server were
cleanly shut down after capture; a host-level process check found no remaining test JVMs.

## Remaining acceptance and what to try

In F5, swap a sword, pickaxe, trident and shield; crouch and look up/down with and without
armor, then equip elytra. A hidden item must always remain accessible through H. Try both
main arms and a resource reload. Check pouch/vial/lantern clearance with leggings and chestplate.

Actual cape-equipped runtime, bulky modded armor, swimming/gliding/sleeping/riding pose
coverage, shaders/shadows and oversized modded weapons remain open visual gates. Their
behavior is not claimed as validated. Backpack/Curios placement and quick-slot dynamic
light are later implementation phases. Per-item JSON with_backpack is parsed but not wired
until that phase. The final full-pack pass must revisit all interactions together.
