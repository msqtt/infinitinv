package com.infinitinv.network;

import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.IExtendedStorageHolder;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Networking for the infinite inventory.
 *
 * <p>The extended storage is authoritative on the server. The client keeps a
 * mirror that is refreshed by every {@link SyncStorageMessage}; every
 * mutation the player performs travels through {@link ActionMessage} with
 * <em>absolute</em> indices, so the server never needs to know which page
 * the client is looking at.
 */
public class InfinitInvNet {

    public static final SimpleNetworkWrapper CHANNEL =
        NetworkRegistry.INSTANCE.newSimpleChannel("infinitinv");

    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(
            SyncStorageMessage.Handler.class,
            SyncStorageMessage.class,
            nextId++,
            Side.CLIENT);
        CHANNEL.registerMessage(
            ActionMessage.Handler.class,
            ActionMessage.class,
            nextId++,
            Side.SERVER);
    }

    /** Builds a snapshot of the given store. */
    public static SyncStorageMessage snapshot(ExtendedStorage storage) {
        return new SyncStorageMessage(storage);
    }

    /** Sends the current state of the player's store to that player. */
    public static void sendSnapshotTo(EntityPlayerMP player) {
        if (player == null || !(player instanceof IExtendedStorageHolder)) {
            return;
        }
        CHANNEL.sendTo(
            snapshot(((IExtendedStorageHolder) player).infinitinv$getExtendedStorage()),
            player);
    }
}
