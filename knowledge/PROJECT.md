Current status (2026-09-18): Quick Slot 0.1.0 is published and deployed alongside
Backpacks+ 0.1.0 in pack 1.38.0 under D-0012. The approved server-local rollback snapshot,
fresh holster conversion and client/server updates completed. Later lighting, companion
and full-pack records supersede historical phase gates below.

# Quick Slot

One additional server-owned player item stack for Minecraft 1.21.1, NeoForge 21.1.x,
Java 21 and official Mojang mappings. Mod ID `quickslot`; namespace `com.chunkworks.quickslot`.
The development target is the pack's NeoForge 21.1.248. Version 0.1.0 is released.

## Approved behavior

Rusty's 2026-09-17 answers govern over the original recommendations:

1. A configurable key swaps with the selected hotbar slot. Scrolling remains nine slots.
2. Elytra hides every quick-slot body display. Bowl meals are always hidden, with or without
   elytra. Both rules affect appearance only (D-0006).
3. Drawing/sheathing has very subtle sound, no added animation.
4. Inventory slot near offhand, subject to a preview against Curios/SB controls.
5. Wide flat items may side hang with a visible backpack if the fit looks good, otherwise hide (D-0005).
6. A minimal SB pushout hook is allowed if needed; explain the exact hook before writing the mixin.
7. Torches and lanterns are visible at the hip and emit dynamic light.
8. Display exactly one item per stack.
9. Body scale is a priority: fit the player naturally, avoiding both oversized and undersized displays.
10. Prefer Refined Tools models where available. Preserve active resource-pack selection and
    size the resolved 3D model, including context/component-dependent variants; refresh on F3+T.
11. Third-person presentation is an explicit acceptance gate: inspect front, rear and both
    sides at normal camera distance, plus moving poses, wide/slim skins and worn equipment.
    Judge both scale and orientation in the real game before accepting body placement.
12. Restore the Earlier model checks placements from Phase 3. Accept minor hair/helmet
    clipping; do not add automatic armor/head offsets or hide items just because of head pose.
13. Sophisticated Backpacks worn models use the approved uniform 80% scale (D-0007).
    Keep this as a small resource-only adjustment; deeper backpack refinement is deferred
    in favor of Rusty's future custom backpack mod.

Rusty's subsequent input-priority correction assigns H to Quick Slot outside driving;
Vanilla Wheels lights take priority for the driver. Passengers keep Quick Slot. Stowed
must be removed from test profiles; production retirement completed in pack 1.38.0.

Rendering remains independent of storage and lighting. Planned back/belt classification,
consumable styles, equipment visibility, visible-backpack detection and resource-reloadable
per-item placement overrides follow the full user brief. Normal blocks/armor stay hidden;
the approved torch/lantern exception gets explicit classification overrides. Supplied defaults
will cover the actual installed item registries, with optional tag entries for absent mods.

## Phases and gates

1. Attachment, lifecycle, required payload sync, swap key and server authority. Dedicated server + two clients.
2. HUD and real InventoryMenu slot, including shift-click and layout review.
3. Player layer, classification and default tags on wide/slim skins.
4. Armor, elytra, cape, anti-clipping and placement JSON reload.
5. Backpacks+ integration with visible gear mounts; backpack development moved to
   `minecraft-backpacks-plus` under D-0008. Sophisticated Backpacks was retired in pack 1.38.0 under the approved discard policy.
6. Optional Luminance integration using its existing lookup/provider, including shaders and underwater behavior.
7. Complete client config, compatibility and polish.

After each phase report the exact tests run, what Rusty should try and remaining risks.
Two rendering test clients are explicitly allowed: Rusty's rule means reuse existing clients
where possible and clean up owned instances, not a hard one-client limit. Do not leave idle clients.

## Current state

D-0009 implements optional Quick Slot lighting through Luminance. The real two-client
gate checks held-equivalent brightness, backpack/hand maxima, underwater behavior,
resource reloads, switches, shaders and reconnect/tracking recovery. The relog check
caught and fixed caching an empty initial stack at revision zero. See
`validation/lighting.md` for exact results; earlier pending-light lines below are
historical. The final full-pack release checks are complete.

Rusty's release request on 2026-09-17 was followed by "Complete remaining features first"
and the request to build Backpacks+. That historical audit held the release. D-0012
subsequently authorized completion and publication; both 0.1.0 mods are now deployed.
Backpack development now belongs to the sibling Backpacks+ repository (D-0008).

Phase 1 passed 19 JUnit tests, 12 real-server GameTests and 16 two-real-client/dedicated-server
checks on 2026-09-17. Both clients and the server were shut down afterward, verified through
host processes. See [validation/phase-1.md](validation/phase-1.md) for coverage and limitations.
Phase 2 has the HUD and inventory injection. Phase 3 now has the body layer, measured model
fitting, classification and initial optional tags. Real captures cover wide/slim skins,
front/rear/sides, both main arms, Refined Tools/vanilla reload, glint and Fire Aspect variants.
The initial Phase 4 checkpoint added offsets and head collision handling, but Rusty's
D-0006 feedback supersedes that fit. The approved earlier placement is restored, automatic
clearance is removed, bowl meals are always hidden and elytra hides every body display.
Resource placement reload remains. The old Phase 4 checkpoint passed 29 JUnit tests,
19 server tests and two-client checks; that historical count includes three now-removed
head collision tests. Current feedback verification passed 26 JUnit tests, 19 server tests
and two-client captures/H exchanges; see validation/phase-4-feedback.md.
Later full-pack checks cover cape-equipped runtime, poses and shaders; see the Backpacks+
release verification record for exact coverage and limits.
The approved 80% Sophisticated Backpacks worn scale is now supplied by six model-resource
overrides; all six chest-slot tiers were checked in two real clients with the temporary
preview pack disabled. See validation/backpack-scale.md. D-0011 companion presentation supersedes the old Phase 5 layering proposal.
See validation/phase-3.md and validation/phase-4.md for the precise coverage.
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

## Legacy retirement preparation — 2026-09-18

D-0010 implements copied-save holster conversion. The four-player rehearsal migrated two
complete Stowed stacks; 26 JUnit and 22 real-server GameTests passed. Backpacks+ D-0019
supersedes lossless backpack migration: discard old bags and all contents, craft new bags.
Native removal was rehearsed on a disposable server world. See validation/legacy-cutover.md.
No production change or release occurred.

## Backpacks+ presentation — 2026-09-18

D-0011 records the approved hip/pocket-verse policy for the independent Quick Slot.
The optional client callback and resource transforms are implemented; real-client
visual verification is underway. Dedicated backpack mount slots remain unchanged.

## Hotbar HUD — 2026-09-18, unreleased

Version 0.1.1 implements D-0013: use vanilla hotbar and selection sprites, honoring
resource packs while retaining cell placement, H behavior and inventory data.
Existing domain and real-server checks passed. Visual evidence is recorded with
Backpacks+ D-0023. Publication and deployment remain held.

D-0013 validation complete: 26 JUnit and 22 real-server tests passed. The full pack
confirmed H independence and mirrored/compact HUD appearance under shaders; see
Backpacks+ `devtools/verification/explicit-stowing.md`. Release remains held.


## Release authorization — 2026-09-18, pack 1.40.0

Rusty explicitly authorized publication and deployment of the backpack mount correction
and the coordinated Backpacks+ 0.2.0 / Quick Slot 0.1.1 update. This supersedes the
release holds for these two versions above. Clean builds and actual-item server checks
gate publication; a fresh empty-player check gates the production restart. Client
delivery is through Mod Hub; Rusty updates Prism themselves. The separate materials
packaging follow-up remains outside this release.
