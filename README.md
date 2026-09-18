# Quick Slot

One extra player item stack for **Minecraft 1.21.1 / NeoForge 21.1.x**.
Required on both client and server. Java 21, official Mojang mappings.

**Version 0.1.0.** Real extra inventory slot, multiplayer body display, approved
placement, optional Luminance light and Backpacks+ companion presentation. The coordinated
release retires Stowed while preserving holsters through the [offline procedure](knowledge/validation/legacy-cutover.md).

Press **H** to exchange the quick slot with the currently selected hotbar stack.
Rebind **Swap Quick Slot** under Controls → Quick Slot. Scrolling still covers the
nine hotbar slots. While driving a Vanilla Wheels vehicle, H belongs to its lights;
Quick Slot is inactive. Passengers retain Quick Slot. Remove Stowed when installing Quick Slot because it implements overlapping functionality. B remains the backpack key.
The client logs unexpected binding conflicts. Successful swaps play
a very quiet local sound. The server rejects swaps while using an item, while another
container is open, while dead/spectating, or if the request no longer matches the slot.

The stored stack survives save/load and dimension travel. Death drops it with ordinary
inventory unless keepInventory or `keepOnDeath` is enabled. Vanishing destroys it on a
dropping death, matching vanilla. Server config lives in `quickslot-server.toml` in the
world's `serverconfig/`: `itemBlocklist` prevents admission, while existing blocked items
can still be removed with an empty hand.

The survival/adventure inventory cell sits directly above offhand. Normal clicks split or
exchange stacks; shift-click returns a quick-slot stack to inventory, merging compatible
stacks first. Shift-clicking an inventory stack fills an empty quick slot after normal
armor/offhand auto-equipping takes priority. Number-key exchanges and dropping use normal
server menu transactions. The HUD mirrors your main arm, shows count/durability, and leaves
space for the hotbar attack indicator. It briefly highlights a confirmed H swap.

**Creative limitation:** use H. The clickable cell is disabled in creative inventory,
whose vanilla item-creation protocol accepts only the original slot indices. Its other
inventory controls remain usable. This guard avoids phantom stacks while preserving the
server-only admission contract; broader creative-screen editing remains future work.

## Build and validation

The workspace's Vanilla Wheels 1.7.0 artifact must be installed in local Maven as
`com.chunkworks.vanillawheels:vanillawheels:1.7.0` for compilation. It is compile-only:
Quick Slot's normal runtime does not require or bundle Vanilla Wheels.

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --no-watch-fs clean build
```

`build` runs plain JUnit against a JDK-only domain source set plus real-server GameTests.
The test mod never enters the production jar. `runClient` and `runServer` use separate
development directories. Tests involving two real clients are a separate required gate;
GameTest player fixtures and codec round trips do not establish multiplayer correctness.

The final 2026-09-18 build passed **26 JUnit and 22 real-server GameTests**, followed by
full-pack two-client checks with Backpacks+. Version 0.1.0 shipped in pack 1.38.0;
see the [cutover record](knowledge/validation/legacy-cutover.md).

Earlier Phase 1 validation on 2026-09-17: **19 JUnit tests, 12 real-server GameTests, and 16 checks with
two actual clients connected to an isolated dedicated server**. Multiplayer coverage includes
full-stack swaps, invalid requests, tracking reentry, relog, Nether/End travel, death/respawn
with keepInventory and Vanishing, server retention, and driver/passenger/dismount H priority.
Stowed was absent from all three processes. Both clients and the server exited after the run.
See [the Phase 1 test record](knowledge/validation/phase-1.md) for the exact limits and replay steps.

For a manual Phase 1 check in an isolated profile, hold a named or damaged tool, press H,
select food, and press H again. The complete stacks should exchange, with a very subtle sound.
Check H while driving and after dismounting, then repeat the exchange after relog or death.
The slot appears on the HUD, in survival/adventure inventory, and on player models.
Later full-pack input/menu checks passed with Controlify loaded. Physical controller
hardware was unavailable.

Body display uses active resource-pack models, including **Refined Tools** where
provided. Its 3D models resolve through the installed model-selection path;
the pack deliberately uses different models in GUI/fixed/ground contexts. Model bounds
are measured from vertices emitted by the actual body-display renderer and refreshed on F3+T.
Quick Slot does not bundle or copy Refined Tools artwork. See [D-0003](knowledge/decisions/D-0003.md).
Third-person size and orientation are explicit visual gates, including front, rear and
side views, moving poses, both skin widths and worn equipment.

Large tools use the back; small items use the belt. Consumables have bottle, pouch
and hanging styles; bowl meals are always hidden. Exactly one item is displayed, regardless of stack size. Ordinary blocks,
armor and feast blocks are hidden; torches and lanterns have explicit visible belt overrides.
With optional Luminance 1.1+, quick-slotted items emit the same dynamic light as held items,
including other players' synced stacks. Underwater behavior and resource-defined brightness
come from Luminance; held, mounted and Quick Slot light combine by maximum. Hidden body items
still emit light. The existing `dynamicLight` client setting disables this contribution.
See the [lighting verification](knowledge/validation/lighting.md) and
[actual shader captures](knowledge/validation/lighting/index.html).
Default classification tags live under
`data/quickslot/tags/item/`; optional entries support Farmer's Delight, Create and other items.

Phase 3 has 26 domain tests and 19 server tests, plus real-client captures of both skin widths,
four viewing angles, Refined Tools/vanilla reload, enchanted model variants and H clearing the
back render. That checkpoint left crouching and equipment clearance for Phase 4.
See [the Phase 3 record](knowledge/validation/phase-3.md).

The approved appearance restores the earlier model-check placements and accepts minor
hair/helmet clipping. Armor and head motion do not add offsets or hide the display.
**Bowl meals always stay hidden. Wearing elytra hides every quick-slot body display**, including
food, potions and lanterns. Storage and H swaps still work. Removing elytra restores ordinary
visible items; bowl meals remain hidden. Capes retain the back-item hiding rule.
This follows [D-0006](knowledge/decisions/D-0006.md), which supersedes the earlier clearance pass.
[Reloadable placement resources](knowledge/placement-format.md) remain available for manual tuning;
they cannot override the bowl-meal or elytra visibility rules. The Backpacks+ release record covers the later cape, pose, full-pack and companion checks
and their limits. Quick Slot
dynamic lighting now has its separate real-client shader gate. The
[Phase 4 record](knowledge/validation/phase-4.md) documents the superseded clearance experiment.
The [current feedback checks](knowledge/validation/phase-4-feedback.md) passed 26 unit tests,
19 server tests and real-client model/visibility/H-swap checks. Try a bowl meal without
elytra, then a lantern with elytra: both displays should be hidden and H should still work.

Phase 2 passed 19 server GameTests (the original 12 plus seven menu tests), the 16 real-client
Phase 1 regressions, and seven additional real-client inventory checks with SB/Curios loaded.
Actual screenshots were inspected for the new cell, count/durability, mirrored HUD and active
attack-indicator clearance. See [the Phase 2 record](knowledge/validation/phase-2.md).

The official [NeoForge 1.21.1 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle)
provides the Gradle wrapper. The build pins NeoForge 21.1.248 and ModDevGradle 2.0.144,
without a Parchment overlay. The jar is `build/libs/quickslot-0.1.0.jar`.

## Structure

- `src/domain`: inventory eligibility and death policy, depending only on Java.
- `src/main`: serialized attachment, atomic mutations, lifecycle events and required payloads.
- `src/main/.../client`: keybinding, bounded pending snapshots and very quiet feedback;
  client subscribers are restricted to `Dist.CLIENT`.
- `src/test`: partition-driven domain tests.
- `src/gametest`: tests against a real Minecraft/NeoForge runtime, excluded from the artifact.
- `knowledge`: project phases and approved decisions.

Item snapshots are copied at ownership boundaries. Requests never carry client-provided
items. Updates are sent on changes and tracking/lifecycle boundaries, not every tick.

## Licence

Copyright (C) 2026 Rusty Shackleford and nfx. AGPL-3.0-or-later; see [LICENSE](LICENSE).

## Backpacks+ companion fit

A visibly worn Backpacks+ bag hides large Quick Slot back items and moves small lower-back
items to the hip. Held, inventory-only and cosmetically hidden bags leave the ordinary
body fit intact. Bowl meals and worn elytra always hide Quick Slot body items. Swaps,
storage and dynamic light remain independent of body visibility. Per-item `with_backpack`
transforms apply to visible items and reload with the resource pack.

See [the companion fit decision](knowledge/decisions/D-0011.md) and the Backpacks+
`devtools/verification/companion-fit.md` capture record. Both mods target the coordinated 0.1.0 release.
