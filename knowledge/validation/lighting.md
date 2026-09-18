# Quick Slot lighting verification — 2026-09-18

Release **HELD**. NeoForge 21.1.248, Java 21, Luminance 1.1.0. No new lighting engine,
world-light blocks or mixins. See [actual shader comparisons](lighting/index.html).

## Reproduction and implementation

Two real clients on the reused dedicated loopback server reproduced the missing feature:
a held torch gave level 14, but the synced Quick Slot torch gave zero for both clients.
[Baseline observations](lighting/before.json) preserve the failure.

The optional client adapter registers with Luminance's existing interpolated entity
provider and uses its item lookup. It caches wet/dry brightness per player, stack identity
and revision. Reload and logout clear those derived values. Quick Slot's `dynamicLight`
switch is now functional. Held/backpack/Quick Slot sources combine by the backend maximum.

An initial revision-only cache failed the real reconnect check: the driver cached EMPTY
at revision zero before its populated initial snapshot arrived at the same revision.
Including the authoritative stack identity fixed this. The exact relog and tracking-range
regression passed afterward, with shaders on the observer.

## Verified results

- Final build: **26 JUnit tests and 19 required dedicated-server GameTests pass**.
- Both real clients match held brightness and the actual terrain field for torch,
  soul torch, lantern, soul lantern and hidden-on-body glowstone.
- Actual H draw/stow preserves light with no duplicate hand source. A brighter held
  lantern wins; a backpack-mounted lantern plus Quick Slot torch has the same terrain
  brightness as that lantern alone. Removing the item clears the field. Real block
  light remains zero throughout the enclosed-room test.
- Quick Slot and Luminance's held/master settings disable the appropriate contribution.
  Underwater, torches extinguish and lanterns stay lit under the backend's own rules.
- Reloaded JSON changes an unchanged torch to level 9. Farmer's Delight apple cider
  takes its resource-defined level 11; removing the definition clears its unchanged
  Quick Slot stack. No hardcoded modded-item brightness is supplied by Quick Slot.
- Save/relog and leaving/reentering tracking range restore the remote source.
- The final build repeats the light and reload checks with **Iris 1.8.14-beta.1,
  Sodium 0.8.13-beta.2 and Complementary Unbound r5.8.1** on the observer. The shader's
  separate handheld effect is disabled (`HELD_LIGHTING_MODE=0`); its terrain visibly
  receives the remote Quick Slot light. Captures were inspected directly.
- A real minimal client/server run **without Luminance** passes startup, synced torch
  storage, actual H draw/stow and save/relog. [Absence evidence](lighting/absent.json).
- Strict mypy passes for `minecraft-backpacks-plus/devtools/quickslot_lighting_test.py`,
  which reuses that companion's real lighting/network fixture.

[Client observations](lighting/checks.json) include the original successful plain-client
checks and the final shader checks. The initial shader launch omitted Sodium and failed;
the matching existing booth dependency corrected that fixture before the passing run.
This was not a production-code compatibility failure.

## Limits and cleanup

The current gate covers dynamic lighting; it does not finish backpack-aware item placement,
all animation poses, controller/full-pack acceptance or legacy bag/Stowed retirement.
No additional appearance changes or gameplay changes were made. The complete shader
shadow/armor matrix remains separate from this lighting gate.

The owned Minecraft test clients and isolated servers were closed. All seven driver
profile jars were restored byte-for-byte, and the temporary shader/light-resource files
were removed or restored. No personal launcher profile, live pack or production world
was changed; no tag, push or release was made.
