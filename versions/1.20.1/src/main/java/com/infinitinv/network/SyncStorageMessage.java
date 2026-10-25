//? if>=1.20 {
package com.infinitinv.network;

import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.StorageIO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: the authoritative state of the extended storage. */
public class SyncStorageMessage {

    private CompoundTag payload;

    public SyncStorageMessage() {
    }

    public SyncStorageMessage(ExtendedStorage storage) {
        payload = new CompoundTag();
        StorageIO.write(storage, payload);
    }

    /** Applies this snapshot to a client side mirror. */
    public void applyTo(ExtendedStorage storage) {
        if (payload == null || storage == null) {
            return;
        }
        StorageIO.read(storage, payload);
    }

    public static void encode(SyncStorageMessage message, FriendlyByteBuf buf) {
        buf.writeNbt(message.payload == null ? new CompoundTag() : message.payload);
    }

    public static SyncStorageMessage decode(FriendlyByteBuf buf) {
        SyncStorageMessage message = new SyncStorageMessage();
        message.payload = buf.readNbt();
        if (message.payload == null) {
            message.payload = new CompoundTag();
        }
        return message;
    }

    public static void handle(SyncStorageMessage message, Supplier<NetworkEvent.Context> ctx) {
        if (!InfinitInvNet.isToClient(ctx.get().getDirection())) {
            return;
        }
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        if (mc.player instanceof com.infinitinv.storage.IExtendedStorageHolder) {
            message.applyTo(((com.infinitinv.storage.IExtendedStorageHolder) mc.player)
                .infinitinv$getExtendedStorage());
        }
    }
}
//? }
