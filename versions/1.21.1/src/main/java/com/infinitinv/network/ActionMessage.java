package com.infinitinv.network;

import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.IExtendedStorageHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Client to server: a mutation request against the extended storage.
 *
 * <p>All indices are <em>absolute</em> indices into the store, so the server
 * never has to know which page the client is looking at.
 */
public class ActionMessage {

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
    private CompoundTag stackTag;

    public ActionMessage() {
    }

    public ActionMessage(Action action, int index, String text, ItemStack stack,
                         HolderLookup.Provider provider) {
        this.action = action;
        this.index = index;
        this.text = text == null ? "" : text;
        if (stack != null && !stack.isEmpty()) {
            this.stackTag = (CompoundTag) stack.save(provider, new CompoundTag());
        }
    }

    public ItemStack getStack(HolderLookup.Provider provider) {
        if (stackTag == null || stackTag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try {
            ItemStack stack = ItemStack.parseOptional(provider, stackTag);
            return stack.isEmpty() ? ItemStack.EMPTY : stack;
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }

    public static void encode(ActionMessage message, FriendlyByteBuf buf) {
        buf.writeInt(message.action.ordinal());
        buf.writeInt(message.index);
        buf.writeUtf(message.text, 256);
        buf.writeNbt(message.stackTag == null ? new CompoundTag() : message.stackTag);
    }

    public static ActionMessage decode(FriendlyByteBuf buf) {
        ActionMessage message = new ActionMessage();
        int ordinal = buf.readInt();
        Action[] values = Action.values();
        message.action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : Action.SET_PAGE;
        message.index = buf.readInt();
        message.text = buf.readUtf(256);
        message.stackTag = buf.readNbt();
        return message;
    }

    public static void handle(ActionMessage message, CustomPayloadEvent.Context ctx) {
        if (!InfinitInvNet.isToServer(ctx)) {
            return;
        }
        ServerPlayer player = ctx.getSender();
        if (player == null || !(player instanceof IExtendedStorageHolder)) {
            return;
        }
        ExtendedStorage storage =
            ((IExtendedStorageHolder) player).infinitinv$getExtendedStorage();
        switch (message.action) {
            case SET_PAGE:
                storage.setPage(message.index);
                break;
            case SET_QUERY:
                storage.setQuery(message.text);
                break;
            case SET_SORT: {
                ExtendedStorage.SortMode[] modes = ExtendedStorage.SortMode.values();
                int ordinal = message.index;
                if (ordinal >= 0 && ordinal < modes.length) {
                    storage.setSortMode(modes[ordinal]);
                }
                break;
            }
            case INSERT:
                storage.add(message.getStack(ctx.getSender().level().registryAccess()));
                break;
            case REMOVE: {
                ItemStack taken = storage.removeOne(message.index);
                if (!taken.isEmpty()) {
                    player.getInventory().add(taken);
                }
                break;
            }
            default:
                break;
        }
        InfinitInvNet.sendSnapshotTo(player);
    }
}
