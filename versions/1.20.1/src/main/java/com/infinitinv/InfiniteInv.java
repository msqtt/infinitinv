//? if>=1.20 {
package com.infinitinv;

import com.infinitinv.network.InfinitInvNet;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Infinite Inventory — gives the player an unbounded, paginated, searchable
 * and sortable item store in place of the 27 main-inventory slots.
 *
 * <p>The hotbar, the armour slots, the offhand and the crafting area keep
 * their exact vanilla behaviour and appearance.
 */
@Mod(InfiniteInv.MODID)
public class InfiniteInv {

    public static final String MODID = "infinitinv";

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class CommonSetup {

        @SubscribeEvent
        public static void setup(FMLCommonSetupEvent event) {
            event.enqueueWork(InfinitInvNet::register);
        }
    }
}
//? }
