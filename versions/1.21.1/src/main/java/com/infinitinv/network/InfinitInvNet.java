package com.infinitinv.network;

import com.infinitinv.storage.IExtendedStorageHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * Networking for the infinite inventory.
 *
 * <p>The extended storage is authoritative on the server. The client keeps a
 * mirror that is refreshed by every {@link SyncStorageMessage}; every
 * mutation the player performs travels through {@link ActionMessage} with
 * <em>absolute</em> indices.
 */
public class InfinitInvNet {

    public static final SimpleChannel CHANNEL = net.minecraftforge.network.ChannelBuilder
        .named(ResourceLocation.parse("infinitinv"))
        .networkProtocolVersion(1)
        .clientAcceptedVersions(Channel.VersionTest.ACCEPT_MISSING)
        .serverAcceptedVersions(Channel.VersionTest.ACCEPT_MISSING)
        .simpleChannel();

    public static void register() {
        CHANNEL.messageBuilder(SyncStorageMessage.class)
            .encoder(SyncStorageMessage::encode)
            .decoder(SyncStorageMessage::decode)
            .consumer(SyncStorageMessage::handle)
            .add();
        CHANNEL.messageBuilder(ActionMessage.class)
            .encoder(ActionMessage::encode)
            .decoder(ActionMessage::decode)
            .consumer(ActionMessage::handle)
            .add();
    }

    /** Sends the current state of the player's store to that player. */
    public static void sendSnapshotTo(ServerPlayer player) {
        if (player == null || !(player instanceof IExtendedStorageHolder)) {
            return;
        }
        CHANNEL.send(
            new SyncStorageMessage(
                ((IExtendedStorageHolder) player).infinitinv$getExtendedStorage(),
                player.level().registryAccess()),
            PacketDistributor.PLAYER.with(player));
    }

    /** Sends a message from the client to the server. */
    public static void toServer(ActionMessage message) {
        CHANNEL.send(message, PacketDistributor.SERVER.noArg());
    }

    /** True when the message arrived on the client side. */
    public static boolean isToClient(net.minecraftforge.event.network.CustomPayloadEvent.Context ctx) {
        return ctx.isClientSide();
    }

    /** True when the message arrived on the server side. */
    public static boolean isToServer(net.minecraftforge.event.network.CustomPayloadEvent.Context ctx) {
        return ctx.isServerSide();
    }
}
