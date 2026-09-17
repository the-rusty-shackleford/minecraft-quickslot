/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Partitions: hotbar endpoints/interior/outside; matching/stale/negative revision;
 * living/dead, spectator/active, busy/free, allowed/blocked. Death: keepInventory and
 * quick-slot retention independently on/off, each with ordinary/Vanishing equipment.
 */
final class SlotRulesTest {
    @ParameterizedTest
    @CsvSource({"0,0,true", "4,4,true", "8,8,true", "-1,-1,false", "9,9,false", "2,3,false"})
    void hotbarIdentity(int requested, int selected, boolean allowed) {
        assertEquals(allowed, SlotRules.maySwap(requested, selected, 7, 7, true, false, false, false));
    }
    @Test void staleOrInvalidRevisionCannotOperateOnANewerSlot() {
        assertFalse(SlotRules.maySwap(0, 0, 6, 7, true, false, false, false));
        assertFalse(SlotRules.maySwap(0, 0, -1, -1, true, false, false, false));
    }
    @ParameterizedTest
    @CsvSource({"false,false,false,false", "true,true,false,false", "true,false,true,false", "true,false,false,true"})
    void ineligiblePlayersCannotSwap(boolean alive, boolean spectator, boolean busy, boolean blocked) {
        assertFalse(SlotRules.maySwap(0, 0, 0, 0, alive, spectator, busy, blocked));
    }
    @ParameterizedTest
    @CsvSource({"false,false,false,DROP", "false,false,true,VANISH", "true,false,false,KEEP", "true,false,true,KEEP",
            "false,true,false,KEEP", "false,true,true,KEEP", "true,true,false,KEEP", "true,true,true,KEEP"})
    void deathPreservesVanillaRetentionPrecedence(boolean keepInventory, boolean keepSlot, boolean curse, SlotRules.Death expected) {
        assertEquals(expected, SlotRules.onDeath(keepInventory, keepSlot, curse));
    }
}
