package com.infinitinv.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Serialises an item stack to NBT.
 *
 * <p>Since 1.21 an item stack carries data components instead of a plain
 * NBT tag, so serialising it needs the registry access of the level the
 * player is in.
 */
public final class ItemNbt {

    private ItemNbt() {
    }

    /** Serialises a stack, or an empty compound for an empty stack. */
    public static CompoundTag save(ItemStack stack, HolderLookup.Provider provider) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        return (CompoundTag) stack.save(provider, new CompoundTag());
    }

    /** Reads a stack back, or an empty stack for a broken entry. */
    public static ItemStack load(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try {
            ItemStack stack = ItemStack.parseOptional(provider, tag);
            return stack == null ? ItemStack.EMPTY : stack;
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }
}
