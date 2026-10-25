//? if>=1.20 {
package com.infinitinv.client;

import com.infinitinv.client.gui.StoreScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

/** Client only: opens the store screen when the key is pressed. */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = com.infinitinv.InfiniteInv.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientTick {

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.screen != null) {
            return;
        }
        while (ClientKeys.OPEN_KEY.consumeClick()) {
            mc.setScreen(new StoreScreen(mc.player));
        }
    }
}
//? }
