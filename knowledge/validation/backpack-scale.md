# Approved worn-backpack scale check — 2026-09-17

Scope: D-0007's small model-resource adjustment, not Phase 5 completion.
Six item-model JSON files inherit Sophisticated Backpacks' block models and supply only
the `sophisticatedbackpacks:worn` display transform at uniform 0.8 scale, zero rotation
and zero translation. The installed 3.25.78.2107 jar's BackpackBakedModel.applyTransform
uses that context's resource transform. Its custom enum is declared in the jar's
META-INF/enumextensions.json. No external model artwork or Java code is copied.

Validation performed in this session:

- `./gradlew --no-watch-fs build`: successful; 26 JUnit tests and 19 required dedicated
  server GameTests passed. No Java implementation changed.
- Actual dedicated server plus the two reused network profiles, with the separate
  `Backpack Scale Preview` resource pack disabled. The resource reload log confirms
  Quick Slot supplies these resources alongside the installed SB/Core/Curios mods.
- All six chest-slot tiers inspected from the rear: basic, copper, iron, gold, diamond,
  netherite. All show the approved smaller silhouette and retain their tier clips.
- Basic backpack inspected from the side, standing and crouched. It matches the approved
  80% comparison, including the existing small mounting gap while crouching.
- Refined Tools diamond sword without a backpack retains the approved Quick Slot fit.
- Replay controller `devtools/backpack_booth.py` passes strict mypy and Ruff.

The replay writes `run/network-observer/screenshots/backpack-approved-*.png` and refuses
to overwrite existing captures. Archive previous captures before replaying. Existing
network test profiles must have SB/Core/Curios and the established two-client test setup;
run the network server/driver/observer Gradle tasks, then the Python controller.

No claim is made here for Curios-equipped rendering, armor clearance, tanks/battery
upgrades, shaders or the full pose matrix. Those remain compatibility gates. The known
Refined Tools model-load errors for unsupported spear/crossbow resources remain in the
client log; these checks do not establish a clean full-pack resource load. The backpack
model overrides loaded and rendered successfully.

The owned clients disconnected before the isolated server was stopped. No personal
Prism profile, production server, published artifact or modpack was changed. Releases HELD.
