# Quick Slot

One extra player item stack for **Minecraft 1.21.1 / NeoForge 21.1.x**.
Required on both client and server. Java 21, official Mojang mappings.

**Development preview — unreleased.** Storage/sync and the inventory/HUD phases have passed
their current gates. Initial body display is implemented; equipment clearance and
dynamic-light integration are still in development.

Press **H** to exchange the quick slot with the currently selected hotbar stack.
Rebind **Swap Quick Slot** under Controls → Quick Slot. Scrolling still covers the
nine hotbar slots. While driving a Vanilla Wheels vehicle, H belongs to its lights;
Quick Slot is inactive. Passengers retain Quick Slot. Stowed is excluded from test
profiles because it implements overlapping functionality. B remains the backpack key.
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

Validated on 2026-09-17: **19 JUnit tests, 12 real-server GameTests, and 16 checks with
two actual clients connected to an isolated dedicated server**. Multiplayer coverage includes
full-stack swaps, invalid requests, tracking reentry, relog, Nether/End travel, death/respawn
with keepInventory and Vanishing, server retention, and driver/passenger/dismount H priority.
Stowed was absent from all three processes. Both clients and the server exited after the run.
See [the Phase 1 test record](knowledge/validation/phase-1.md) for the exact limits and replay steps.

For a manual Phase 1 check in an isolated profile, hold a named or damaged tool, press H,
select food, and press H again. The complete stacks should exchange, with a very subtle sound.
Check H while driving and after dismounting, then repeat the exchange after relog or death.
The slot appears on the HUD, in survival/adventure inventory, and on player models.
Full-pack input conflicts,
controller bindings and human judgement of sound volume remain later compatibility checks.

Body display uses active resource-pack models, including **Refined Tools** where
provided. Its 3D models resolve through the installed model-selection path;
the pack deliberately uses different models in GUI/fixed/ground contexts. Scale and clearance
are measured from vertices emitted by the actual body-display renderer and refreshed on F3+T.
Quick Slot does not bundle or copy Refined Tools artwork. See [D-0003](knowledge/decisions/D-0003.md).
Third-person size and orientation are explicit visual gates, including front, rear and
side views, moving poses, both skin widths and worn equipment.

Large tools use the back; small items use the belt. Consumables have bottle, pouch, bowl
and hanging styles. Exactly one item is displayed, regardless of stack size. Ordinary blocks,
armor and feast blocks are hidden; torches and lanterns have explicit visible belt overrides.
Dynamic light from this slot is not implemented yet. Default classification tags live under
`data/quickslot/tags/item/`; optional entries support Farmer's Delight, Create and other items.

Phase 3 has 26 domain tests and 19 server tests, plus real-client captures of both skin widths,
four viewing angles, Refined Tools/vanilla reload, enchanted model variants and H clearing the
back render. Crouching head clearance, worn equipment, shaders and full-pack compatibility
are not accepted yet. See [the Phase 3 record](knowledge/validation/phase-3.md).

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
