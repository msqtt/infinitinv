package com.infinitinv.slot;

import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.IExtendedStorageHolder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

/**
 * A slot that is backed by the unbounded {@link ExtendedStorage} instead of
 * the vanilla inventory.
 *
 * <p>The slot keeps the vanilla inventory as its "owner" so that all the
 * surrounding container logic (drag &amp; drop, hotbar swaps, the cursor
 * stack, ...) keeps working unchanged. Only the actual content is redirected
 * to the extended store.
 *
 * <p>{@link #displayIndex} is the position inside the visible page; the
 * absolute index inside the store is resolved on demand from the store's
 * current filter and page.
 */
public class ExtendedSlot extends Slot {

    private final IExtendedStorageHolder holder;

    /** Position inside the visible 3x9 page, 0..26. */
    private final int displayIndex;

    public ExtendedSlot(IInventory owner, IExtendedStorageHolder holder, int displayIndex, int x, int y) {
        // The slot index is kept inside the vanilla main-inventory range so
        // that generic container code keeps treating this as a normal
        // inventory slot.
        super(owner, 9 + displayIndex, x, y);
        this.holder = holder;
        this.displayIndex = displayIndex;
    }

    public int getDisplayIndex() {
        return displayIndex;
    }

    /** Absolute index inside the store that this slot currently shows. */
    public int getAbsoluteIndex() {
        return storage().absoluteIndexForDisplay(displayIndex);
    }

    private ExtendedStorage storage() {
        return holder.infinitinv$getExtendedStorage();
    }

    private static boolean isClient() {
        return FMLCommonHandler.instance().getSide() == Side.CLIENT;
    }

    @Override
    public ItemStack getStack() {
        int absolute = getAbsoluteIndex();
        if (absolute < 0) {
            return ItemStack.EMPTY;
        }
        return storage().getStack(absolute);
    }

    @Override
    public void putStack(ItemStack stack) {
        if (isClient()) {
            // The client mirror is only ever written by a full snapshot from
            // the server; accepting vanilla's per-slot sync would corrupt it.
            return;
        }
        int absolute = getAbsoluteIndex();
        if (absolute < 0) {
            return;
        }
        storage().setStack(absolute, stack == null ? ItemStack.EMPTY : stack);
        sync();
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        int absolute = getAbsoluteIndex();
        if (absolute < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = storage().getStack(absolute);
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(amount, stack.getCount());
        ItemStack result = stack.splitStack(taken);
        if (stack.isEmpty()) {
            storage().setStack(absolute, ItemStack.EMPTY);
        } else {
            storage().setStack(absolute, stack);
        }
        sync();
        return result;
    }

    @Override
    public int getSlotStackLimit() {
        return 64;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return true;
    }

    /** Pushes the authoritative store back to the owning player. */
    private void sync() {
        if (isClient() || !(holder instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) holder;
        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            com.infinitinv.network.InfinitInvNet.sendSnapshotTo(
                (net.minecraft.entity.player.EntityPlayerMP) player);
        }
    }
}
