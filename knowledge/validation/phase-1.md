# Phase 1 validation — 2026-09-17

Status: passed for the attachment, lifecycle, network and key interaction scope. Unreleased.
Minecraft 1.21.1, NeoForge 21.1.248, Java 21, official Mojang mappings.

## Automated gates

- 19 plain JUnit tests. The domain test runtime contains JUnit/JDK dependencies only.
  Partitions cover selected slot/index bounds, stale revisions, eligibility and death policy.
- 12 real-server GameTests: complete-stack exchange, empty stacks and defensive copies,
  actual player save/load, death/Vanishing/keepInventory, clone ownership, invalid requests,
  use-in-progress, actual registry-aware payload codecs and overfull-stack rejection.
  GameTests use the Minecraft player fixture; multiplayer is verified separately below.
- 16 checks with two actual Minecraft clients on an isolated dedicated server:
  Stowed absent; real H key mapping swaps and swap-back; full item component sync;
  malformed/stale/mismatched-slot payload rejection; spectator rejection; tracking exit
  and reentry; real disconnect/relog; Nether/End travel; four actual death/respawn cases
  crossing keepInventory and Vanishing; server keep-on-death; driver light priority with
  server rejection of a forged quick-slot request; passenger swap; dismount swap.

The final multiplayer run used the final production death-drop implementation. An earlier
passing run preceded a correction to match vanilla drop pickup delay (40 ticks). The
regression test first reproduced the old 10-tick delay, then passed against vanilla
`Player.drop`; the full build and multiplayer gate were rerun afterward. The drop stays in
the normal LivingDropsEvent collection, preserving event cancellation semantics.

The test adapters coordinate actions/evidence through JSON files. Client swap actions enter
the actual KeyMapping path; malformed requests use the production custom payload; all slot
sync crosses the production NeoForge network connection. They do not substitute direct
client attachment writes for synchronization. Test adapters are excluded from the mod jar.

## Test environment and limits

The dedicated server binds localhost only. Driver and observer have separate development
directories, muted audio and small render distances. Their optional fixture mods are
Vanilla Wheels 1.7.0 and Trailblazer 1.7.0, with nested Luminance/Metals and Materials.
Stowed is excluded. Production and the personal Prism profile were not changed.

The final controller exited successfully and requested clean shutdown of all three game
processes. Host-level `jps` afterward showed no Minecraft process. The developer's older
one-client note is superseded by Rusty's explicit permission to use multiple clients,
reuse existing clients where possible, and clean up owned instances.

This gate does not establish HUD/inventory click correctness, body rendering, Refined Tools
model selection, shaders, dynamic light, SB/Curios layering, controller behavior or full-pack
compatibility. Those features/gates belong to later phases. Sound delivery is exercised;
human judgement of its subtle volume remains a manual check. Scripted fixture teleport
warnings are not evidence about production vehicle movement correctness.

## Replaying

Run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --no-watch-fs clean build`
for JUnit and GameTests. Gradle development runs `runNetworkServer`, `runNetworkDriver`
and `runNetworkObserver` provide the real-client adapters; each uses the gametest source
set and `run/network-*`. They require isolated local server configuration, accepted EULA,
matching Vanilla Wheels/Trailblazer fixture jars in each `mods/`, and muted client options.
Read `devtools/network_test.py` before replaying: it expects all three processes, controls
test player inventories/death/travel, and shuts them down only on success. Never point it
at a production server or a personal world. It binds through the test server at port 25586.

Run the controller from the repository using:

```sh
uv run --no-project python devtools/network_test.py
```

Evidence from this session is under the local scratch directory
`/tmp/claude-1000/codex-orientation-20260916/resume/quickslot/`:
`phase1-final-build.log`, `drop-delay-reproduction.log`, `network-results-final.log`, and
the final client/server logs. Detailed synchronized states are in the ignored
`run/network-control/results.json`. Python controller checks passed strict mypy and Ruff.

Final production jar SHA-256:
`2a0ae888fa313a97d3991eaf68198dc68db4bc77085b60efaa2404e0afd658e6`.
