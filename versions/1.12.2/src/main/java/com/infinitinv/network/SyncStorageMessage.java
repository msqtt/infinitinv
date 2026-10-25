package com.infinitinv.network;

import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.StorageIO;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Server to client: the authoritative state of the extended storage. */
public class SyncStorageMessage implements IMessage {

    private NBTTagCompound payload;

    public SyncStorageMessage() {
    }

    public SyncStorageMessage(ExtendedStorage storage) {
        payload = new NBTTagCompound();
        StorageIO.write(storage, payload);
    }

    /** Applies this snapshot to a client side mirror. */
    public void applyTo(ExtendedStorage storage) {
        if (payload == null || storage == null) {
            return;
        }
        StorageIO.read(storage, payload);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            int length = buf.readInt();
            byte[] data = new byte[length];
            buf.readBytes(data);
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            payload = CompressedStreamTools.read(in);
        } catch (IOException e) {
            payload = new NBTTagCompound();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            CompressedStreamTools.write(payload == null ? new NBTTagCompound() : payload, out);
            out.flush();
            byte[] data = bytes.toByteArray();
            buf.writeInt(data.length);
            buf.writeBytes(data);
        } catch (IOException e) {
            buf.writeInt(0);
        }
    }

    /** Client side handler; only ever executed on the client. */
    public static class Handler implements IMessageHandler<SyncStorageMessage, IMessage> {
        @Override
        public IMessage onMessage(SyncStorageMessage message, MessageContext ctx) {
            if (ctx.side != net.minecraftforge.fml.relauncher.Side.CLIENT) {
                return null;
            }
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc == null || mc.player == null) {
                return null;
            }
            if (mc.player instanceof com.infinitinv.storage.IExtendedStorageHolder) {
                message.applyTo(((com.infinitinv.storage.IExtendedStorageHolder) mc.player)
                    .infinitinv$getExtendedStorage());
            }
            return null;
        }
    }
}
