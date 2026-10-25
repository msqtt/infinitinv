package com.infinitinv.event;

import com.infinitinv.InfiniteInv;
import com.infinitinv.network.InfinitInvNet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server side events of the infinite inventory. */
@Mod.EventBusSubscriber(modid = InfiniteInv.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {

    /**
     * Pushes the authoritative store to the client the moment it joins, so
     * the client side mirror is filled without the player having to touch
     * anything first.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InfinitInvNet.sendSnapshotTo(player);
        }
    }
}
