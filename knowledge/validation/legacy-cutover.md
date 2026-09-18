# Legacy retirement and holster conversion — 2026-09-18

Release is authorized under D-0012. This rehearsal is not deployment evidence.
Rusty also approved the private server-local rollback snapshot.
Backpacks+ D-0018/D-0019 authorize discarding existing Sophisticated Backpacks bags and
all their contents. No replacement bags are granted. New bags must be crafted.
Stowed's separate holstered items are preserved in Quick Slot.

## Executed

- Explicitly approved, private local audit copy: four current player saves and SB's
  UUID-indexed SavedData. Two populated Stowed holsters; one equipped player backpack.
  The raw inventories, player identities, archive and prepared outputs stay in local
  session scratch and must never be committed or published.
- Read-only remote region scan completed: 5,187 region files, 1,435,822 chunks, 44
  matching chunks. Structured inspection found legacy bags in entity data. No matching
  terrain chunks were found. This is a scan of saved files on a running server, not a
  consistent full-world backup and not proof of future state at release time.
- The native NBT operator tool prepared all four copied player files and moved exactly
  two holsters. It retains raw modded item components without registry coercion, changes
  only the attachment key, never edits its input, and emits source/output SHA-256 values.
- Quick Slot build passed 26 JUnit and 22 real-server GameTests. Three new tests cover
  native player attachment load, named/damaged and opaque modded data, conflict refusal,
  idempotence, actual compressed files, input byte preservation, existing output refusal,
  path aliases and unexpected filenames. The tool is excluded from the production jar.
- Backpacks+'s isolated server saved synthetic bags with SB/Core installed, shut down,
  then reopened the same world without them. Bags disappeared from ordinary inventory,
  chest equipment, Curios, a chest, armor-stand equipment and dropped-item storage.
  Unrelated diamonds, boots and a named/damaged Quick Slot sword survived.
- The first fixture incorrectly required the dropped-entity lookup to be entirely empty.
  Replay showed an already-discarded entity containing air; native loading had removed
  the bag correctly. The final assertion rejects any live or recoverable legacy item.
  The task now fails explicitly if the server experiment has no successful result.
- Installed client and server jar metadata: SB is the only dependency on Sophisticated
  Core. Recheck that at the eventual cutover. Curios remains installed.

## Preparing copied player files

From the Quick Slot repository, with Java 21:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --no-watch-fs prepareLegacyHolsters \
  -PlegacyInput=/absolute/path/to/offline-playerdata-copy \
  -PlegacyOutput=/absolute/path/to/new-prepared-directory
```

The output parent must exist; the output directory must be new and outside the input,
including through symbolic links. All current UUID-named `.dat` files are checked before
output is created. `.dat_old` files are not selected. An occupied Quick Slot, malformed
attachment, unexpected `.dat` filename or changed source refuses preparation.
A failed I/O attempt may leave partial output: only a directory with `COMPLETE` and a
verified `sha256.tsv` is eligible for review. The utility performs no deployment.

## Production procedure

Release and the server-local snapshot are authorized. Require a fresh `rcon-cli list` showing zero players:

1. Stop the server cleanly and take a consistent rollback backup of the full world,
   player data, SB SavedData, installed mods and current pack files. Do not reuse the earlier
   running-server audit copy as the release backup.
2. Run the tool entirely on the server, against the fresh rollback copy of every current
   player `.dat`; no fresh player files leave the server. Verify both
   source and prepared checksums. Stop on a conflict; never overwrite an occupied slot.
3. Install the reviewed Quick Slot and Backpacks+ builds on both sides; remove Stowed,
   Sophisticated Backpacks and, if still unused, Sophisticated Core from both pack lists.
   Keep Curios. Archive SB's SavedData outside the active world; do not convert or grant
   bags, recover their contents, or create recovery chests.
4. While still stopped, verify live input files match the tool's source hashes and
   install the prepared current saves. Keep the original `.dat`/`.dat_old` in rollback
   storage and use the prepared current save as the new fallback, avoiding restoration
   of an obsolete Stowed attachment by a later fallback load.
5. Start and inspect registry-removal warnings. Unknown SB stacks are discarded by
   native loading as players/chunks load; unloaded serialized references are not rewritten
   eagerly. Verify both preserved holsters with the actual modded item registries,
   Curios equipment, crafting, contents and client/server mod parity.
6. Rollback is a stopped-server restore of the matching world, player files and old pack
   together. Do not restore SB SavedData into a partially migrated active world.

The historical rehearsal above did not change production. At the authorized release
cutover, the consistent server-local snapshot completed and the native converter verified
four fresh saves, preparing two holsters. The operator deployment record follows separately.
Full-pack acceptance and release authorization are now complete.

## Completed deployment

Both 0.1.0 releases shipped with pack 1.38.0 on 2026-09-18. All four fresh saves were
installed after source/prepared checksum checks; two holsters were preserved. Original
current/fallback files remain in the private server-local rollback snapshot. Stowed and
SB/Core are absent, SB SavedData is archived outside the active world, and Curios remains.
The server's jar hashes match the releases, Mod Hub reports pack parity, and RCON reports
20 TPS. Live player login has not yet been observed. See the companion
[deployment record](https://github.com/the-rusty-shackleford/minecraft-backpacks-plus/blob/main/devtools/verification/deployment.md)
for hashes and exact verification scope.
