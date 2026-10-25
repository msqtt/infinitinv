//? if>=1.20 {
package com.infinitinv.network;

import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.IExtendedStorageHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Networking for the infinite inventory.
 *
 * <p>The extended storage is authoritative on the server. The client keeps a
 * mirror that is refreshed by every {@link SyncStorageMessage}; every
 * mutation the player performs travels through {@link ActionMessage} with
 * <em>absolute</em> indices.
 */
public class InfinitInvNet {

    //? if=1.20.1 {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("infinitinv"),
        () -> "1",
        NetworkRegistry.acceptMissingOr("1"),
        NetworkRegistry.acceptMissingOr("1"));
    //? } else {
    // public static final SimpleChannel CHANNEL = net.minecraftforge.network.ChannelBuilder
    //     .named(new ResourceLocation("infinitinv"))
    //     .networkProtocolVersion(() -> "1")
    //     .simpleChannel();
    //? }

    private static int nextId = 0;

    public static void register() {
        CHANNEL.messageBuilder(SyncStorageMessage.class, nextId++)
            .encoder(SyncStorageMessage::encode)
            .decoder(SyncStorageMessage::decode)
            .consumerMainThread(SyncStorageMessage::handle)
            .add();
        CHANNEL.messageBuilder(ActionMessage.class, nextId++)
            .encoder(ActionMessage::encode)
            .decoder(ActionMessage::decode)
            .consumerMainThread(ActionMessage::handle)
            .add();
    }

    /** Sends the current state of the player's store to that player. */
    public static void sendSnapshotTo(ServerPlayer player) {
        if (player == null || !(player instanceof IExtendedStorageHolder)) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new SyncStorageMessage(((IExtendedStorageHolder) player).infinitinv$getExtendedStorage()));
    }

    /** Sends a message from the client to the server. */
    public static void toServer(ActionMessage message) {
        CHANNEL.sendToServer(message);
    }

    /** The direction a message travels in, used to keep the two apart. */
    public static boolean isToClient(NetworkDirection direction) {
        return direction == NetworkDirection.PLAY_TO_CLIENT;
    }
}
//? }
