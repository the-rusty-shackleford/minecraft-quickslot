# Quick Slot

One additional server-owned player item stack for Minecraft 1.21.1, NeoForge 21.1.x,
Java 21 and official Mojang mappings. Mod ID `quickslot`; namespace `com.chunkworks.quickslot`.
The development target is the pack's NeoForge 21.1.248. New releases are HELD.

## Approved behavior

Rusty's 2026-09-17 answers govern over the original recommendations:

1. A configurable key swaps with the selected hotbar slot. Scrolling remains nine slots.
2. Keep back items visible with elytra and position them as well as possible; do not default to hiding.
3. Drawing/sheathing has very subtle sound, no added animation.
4. Inventory slot near offhand, subject to a preview against Curios/SB controls.
5. Wide flat items move to side hang with a visible backpack.
6. A minimal SB pushout hook is allowed if needed; explain the exact hook before writing the mixin.
7. Torches and lanterns are visible at the hip and emit dynamic light.
8. Display exactly one item per stack.
9. Body scale is a priority: fit the player naturally, avoiding both oversized and undersized displays.
10. Prefer Refined Tools models where available. Preserve active resource-pack selection and
    size the resolved 3D model, including context/component-dependent variants; refresh on F3+T.
11. Third-person presentation is an explicit acceptance gate: inspect front, rear and both
    sides at normal camera distance, plus moving poses, wide/slim skins and worn equipment.
    Judge both scale and orientation in the real game before accepting body placement.

Rusty's subsequent input-priority correction assigns H to Quick Slot outside driving;
Vanilla Wheels lights take priority for the driver. Passengers keep Quick Slot. Stowed
must be removed from test profiles; production removal waits for release authorization.

Rendering remains independent of storage and lighting. Planned back/belt classification,
consumable styles, armor/cape/elytra offsets, visible-backpack detection and resource-reloadable
per-item placement overrides follow the full user brief. Normal blocks/armor stay hidden;
the approved torch/lantern exception gets explicit classification overrides. Supplied defaults
will cover the actual installed item registries, with optional tag entries for absent mods.

## Phases and gates

1. Attachment, lifecycle, required payload sync, swap key and server authority. Dedicated server + two clients.
2. HUD and real InventoryMenu slot, including shift-click and layout review.
3. Player layer, classification and default tags on wide/slim skins.
4. Armor, elytra, cape, anti-clipping and placement JSON reload.
5. Sophisticated Backpacks/Curios detection, layering and clearance.
6. Optional Luminance integration using its existing lookup/provider, including shaders and underwater behavior.
7. Complete client config, compatibility and polish.

After each phase report the exact tests run, what Rusty should try and remaining risks.
Two rendering test clients are explicitly allowed: Rusty's rule means reuse existing clients
where possible and clean up owned instances, not a hard one-client limit. Do not leave idle clients.

## Current state

Phase 1 passed 19 JUnit tests, 12 real-server GameTests and 16 two-real-client/dedicated-server
checks on 2026-09-17. Both clients and the server were shut down afterward, verified through
host processes. See [validation/phase-1.md](validation/phase-1.md) for coverage and limitations.
Phase 2 has the HUD and inventory injection. Phase 3 now has the body layer, measured model
fitting, classification and initial optional tags. Real captures cover wide/slim skins,
front/rear/sides, both main arms, Refined Tools/vanilla reload, glint and Fire Aspect variants.
Crouching head clearance remains an observed defect for the Phase 4 clearance pass;
equipment/shader/full-pack visual acceptance is not complete. See validation/phase-3.md.
The current proposal places its inventory item at (77, 42), directly above the existing
offhand item at (77, 62). HUD placement mirrors the main arm and reserves attack-indicator
space. Rusty approved the local interactive preview and InventoryMenu injection approach;
Phase 2 passed 19 server GameTests, the 16 real-client Phase 1 regressions and seven menu
checks on two clients with SB/Curios loaded. Active attack-indicator clearance was inspected
on both sides in a separate single-client capture. See validation/phase-2.md for limitations,
including creative mode using H while its menu cell is disabled.
Server blocklist/retention config is included now because Phase 1 admission/death tests require it;
the remaining client configuration belongs to later phases. No production pack was changed.

## Compatibility targets verified during orientation

Pack 1.36.0: NeoForge 21.1.248, Luminance 1.1.0, Sophisticated Backpacks
3.25.78.2107 and Curios 9.5.1. Luminance's jar declares mod ID `luminance`.
Its entity provider and `Providers.luminanceOf` are available without a lighting mixin.
The current SB branch uses `BackpackItem` and `BackpackLayerRenderer`; exact installed
jar/source matching and geometric clearance measurements remain Phase 5 work.

The personal Prism profile enables Refined Tools 3.0 and contains Modefite
`1.0.1+1.21.1` (jar mod ID `modefite`), which backports the newer item-definition system.
The diamond sword definition explicitly selects its sprite model for GUI/fixed/ground and
its 3D tool model otherwise, with a Fire Aspect branch. Visual gates must include that exact
resource-pack/model-loader combination; plain vanilla model bounds are insufficient.
