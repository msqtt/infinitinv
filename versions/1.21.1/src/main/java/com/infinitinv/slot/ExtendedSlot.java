package com.infinitinv.slot;

import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.IExtendedStorageHolder;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A slot that is backed by the unbounded {@link ExtendedStorage} instead of
 * the vanilla inventory.
 *
 * <p>The slot keeps the vanilla inventory as its "owner" so that all the
 * surrounding container logic (drag andamp; drop, hotbar swaps, the cursor
 * stack, ...) keeps working unchanged. Only the actual content is redirected
 * to the extended store.
 */
public class ExtendedSlot extends Slot {

    private final IExtendedStorageHolder holder;

    /** Position inside the visible 3x9 page, 0..26. */
    private final int displayIndex;

    public ExtendedSlot(Container owner, IExtendedStorageHolder holder, int displayIndex, int x, int y) {
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

    @Override
    public ItemStack getItem() {
        int absolute = getAbsoluteIndex();
        if (absolute < 0) {
            return ItemStack.EMPTY;
        }
        return storage().getStack(absolute);
    }

    @Override
    public void set(ItemStack stack) {
        int absolute = getAbsoluteIndex();
        if (absolute < 0) {
            return;
        }
        storage().setStack(absolute, stack == null ? ItemStack.EMPTY : stack);
    }

    @Override
    public ItemStack remove(int amount) {
        int absolute = getAbsoluteIndex();
        if (absolute < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = storage().getStack(absolute);
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(amount, stack.getCount());
        ItemStack result = stack.split(taken);
        if (stack.isEmpty()) {
            storage().setStack(absolute, ItemStack.EMPTY);
        } else {
            storage().setStack(absolute, stack);
        }
        return result;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return true;
    }

    @Override
    public boolean mayPickup(Player player) {
        return true;
    }
}
