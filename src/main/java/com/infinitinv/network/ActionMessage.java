package com.infinitinv.network;

import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.IExtendedStorageHolder;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
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

/**
 * Client to server: a mutation request against the extended storage.
 *
 * <p>All indices are <em>absolute</em> indices into the store, so the server
 * never has to know which page the client is looking at.
 */
public class ActionMessage implements IMessage {

    public enum Action {
        /** Move to another page. */
        SET_PAGE,
        /** Change the search query. */
        SET_QUERY,
        /** Change the sort order. */
        SET_SORT,
        /** Insert the given stack into the store. */
        INSERT,
        /** Remove one item from the given absolute index. */
        REMOVE
    }

    private Action action = Action.SET_PAGE;
    private int index;
    private String text = "";
    private NBTTagCompound stackTag;

    public ActionMessage() {
    }

    public ActionMessage(Action action, int index, String text, ItemStack stack) {
        this.action = action;
        this.index = index;
        this.text = text == null ? "" : text;
        if (stack != null && !stack.isEmpty()) {
            this.stackTag = stack.writeToNBT(new NBTTagCompound());
        }
    }

    public Action getAction() {
        return action;
    }

    public int getIndex() {
        return index;
    }

    public String getText() {
        return text;
    }

    public ItemStack getStack() {
        if (stackTag == null || !stackTag.hasKey("id")) {
            return ItemStack.EMPTY;
        }
        try {
            ItemStack stack = new ItemStack(stackTag);
            return stack.getItem() == null ? ItemStack.EMPTY : stack;
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int ordinal = buf.readInt();
        Action[] values = Action.values();
        action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : Action.SET_PAGE;
        index = buf.readInt();
        int textLength = buf.readInt();
        byte[] textBytes = new byte[Math.max(0, Math.min(textLength, 256))];
        buf.readBytes(textBytes);
        try {
            text = new String(textBytes, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            text = "";
        }
        int length = buf.readInt();
        stackTag = null;
        if (length > 0) {
            byte[] data = new byte[length];
            buf.readBytes(data);
            try {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
                stackTag = CompressedStreamTools.read(in);
            } catch (IOException e) {
                stackTag = null;
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action.ordinal());
        buf.writeInt(index);
        byte[] textBytes;
        try {
            textBytes = text.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            textBytes = new byte[0];
        }
        buf.writeInt(textBytes.length);
        buf.writeBytes(textBytes);
        if (stackTag == null) {
            buf.writeInt(0);
            return;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            CompressedStreamTools.write(stackTag, out);
            out.flush();
            byte[] data = bytes.toByteArray();
            buf.writeInt(data.length);
            buf.writeBytes(data);
        } catch (IOException e) {
            buf.writeInt(0);
        }
    }

    /** Server side handler. */
    public static class Handler implements IMessageHandler<ActionMessage, IMessage> {
        @Override
        public IMessage onMessage(ActionMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null || !(player instanceof IExtendedStorageHolder)) {
                return null;
            }
            ExtendedStorage storage =
                ((IExtendedStorageHolder) player).infinitinv$getExtendedStorage();
            switch (message.getAction()) {
                case SET_PAGE:
                    storage.setPage(message.getIndex());
                    break;
                case SET_QUERY:
                    storage.setQuery(message.getText());
                    break;
                case SET_SORT: {
                    ExtendedStorage.SortMode[] modes = ExtendedStorage.SortMode.values();
                    int ordinal = message.getIndex();
                    if (ordinal >= 0 && ordinal < modes.length) {
                        storage.setSortMode(modes[ordinal]);
                    }
                    break;
                }
                case INSERT:
                    storage.add(message.getStack());
                    break;
                case REMOVE: {
                    ItemStack taken = storage.removeOne(message.getIndex());
                    if (taken != null && !taken.isEmpty()) {
                        player.inventory.addItemStackToInventory(taken);
                    }
                    break;
                }
                default:
                    break;
            }
            InfinitInvNet.sendSnapshotTo(player);
            return null;
        }
    }
}
