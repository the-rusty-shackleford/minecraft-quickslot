/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.client;

import com.chunkworks.quickslot.QuickSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Client lifecycle invalidation, isolated from dedicated-server class loading. */
@EventBusSubscriber(modid = QuickSlot.ID, value = Dist.CLIENT)
public final class BodyReload {
    private BodyReload() {}
    /** effects: invalidates tag-based presentation after a client tag update. */
    @SubscribeEvent public static void tags(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) BodyLayer.clear();
    }
    /** effects: releases cached world presentations on disconnect. */
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { BodyLayer.clear(); }
}
