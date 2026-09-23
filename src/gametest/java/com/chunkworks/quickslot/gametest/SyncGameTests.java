/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.gametest;

import com.chunkworks.quickslot.Payloads;
import com.chunkworks.quickslot.Slot;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Partitions: a server player whose connection never negotiated the slot's channel (the
 * framework's mock, placed through the player list so the login listeners run): the
 * broadcast on login, an explicit broadcast and a tracking snapshot must skip them rather
 * than throw out of the listener (issue #1).
 */
@GameTestHolder("quickslot")
@PrefixGameTestTemplate(false)
public final class SyncGameTests {
    public SyncGameTests() {}

    @GameTest(template = "empty")
    public void aPlayerWithoutTheChannelIsSkippedNotCrashed(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();       // 0.1.1 threw inside the login listener here
        h.assertFalse(p.connection.hasChannel(Payloads.State.TYPE), "the mock negotiated nothing");
        Slot.broadcast(p, false);
        Slot.broadcast(p, true);
        Slot.syncTo(p, p);
        h.succeed();
    }
}
