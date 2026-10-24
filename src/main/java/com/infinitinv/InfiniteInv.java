package com.infinitinv;

import com.infinitinv.network.InfinitInvNet;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Infinite Inventory — gives the player an unbounded, paginated, searchable
 * and sortable item store in place of the 27 main-inventory slots.
 *
 * <p>The hotbar (slots 0-8), the armour slots, the offhand and the crafting
 * area keep their exact vanilla behaviour and appearance.
 */
@Mod(modid = InfiniteInv.MODID)
public class InfiniteInv {

    public static final String MODID = "infinitinv";

    @Mod.Instance(MODID)
    public static InfiniteInv instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        InfinitInvNet.register();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide() == Side.CLIENT) {
            com.infinitinv.client.ClientHooks.init();
        }
    }
}
