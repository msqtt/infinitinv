package com.infinitinv;

import com.infinitinv.network.InfinitInvNet;
import net.minecraftforge.fml.common.Mod;

/**
 * Infinite Inventory - gives the player an unbounded, paginated, searchable
 * and sortable item store in place of the 27 main-inventory slots.
 *
 * <p>The hotbar, the armour slots, the offhand and the crafting area keep
 * their exact vanilla behaviour and appearance.
 */
@Mod(InfiniteInv.MODID)
public class InfiniteInv {

    public static final String MODID = "infinitinv";

    public InfiniteInv() {
        // The channel has to exist before the client and the server start
        // talking to each other, so it is registered here instead of
        // waiting for a setup event.
        InfinitInvNet.register();
    }
}
