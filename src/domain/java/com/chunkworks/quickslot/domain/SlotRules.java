/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.domain;

/** Stateless inventory rules. No stack or platform object crosses this boundary. */
public final class SlotRules {
    private SlotRules() {}

    /** The disposition of a stored stack at death. */
    public enum Death { KEEP, DROP, VANISH }

    /**
     * effects: returns whether an intent still refers to an eligible selected slot
     * and the current quick-slot revision; invalid indices are rejected, never clamped.
     */
    public static boolean maySwap(int requested, int selected, long expected, long actual,
            boolean alive, boolean spectator, boolean busy, boolean blocked) {
        return requested >= 0 && requested < 9 && requested == selected
                && expected >= 0 && expected == actual
                && alive && !spectator && !busy && !blocked;
    }

    /**
     * effects: follows vanilla keepInventory precedence, including retaining Vanishing
     * when inventory is retained; the server keep-on-death option acts like keepInventory.
     */
    public static Death onDeath(boolean keepInventory, boolean keepQuickSlot, boolean vanishing) {
        return keepInventory || keepQuickSlot ? Death.KEEP : vanishing ? Death.VANISH : Death.DROP;
    }
}
