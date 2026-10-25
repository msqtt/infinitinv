//? if>=1.20 {
package com.infinitinv.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes an {@link ExtendedStorage} to and from NBT.
 *
 * <p>The item list is written index aligned: every slot of the store gets an
 * entry, empty ones are written as an empty compound. That keeps the absolute
 * indices identical on both sides of the connection.
 */
public final class StorageIO {

    private static final String KEY_ITEMS = "Items";
    private static final String KEY_FILTERED = "Filtered";
    private static final String KEY_PAGE = "Page";
    private static final String KEY_QUERY = "Query";
    private static final String KEY_SORT = "Sort";

    private StorageIO() {
    }

    /** Writes the full state of the store into {@code out}. */
    public static void write(ExtendedStorage storage, CompoundTag out) {
        ListTag list = new ListTag();
        for (int i = 0; i < storage.size(); i++) {
            ItemStack stack = storage.getStack(i);
            if (stack == null || stack.isEmpty()) {
                list.add(new CompoundTag());
            } else {
                list.add(stack.save(new CompoundTag()));
            }
        }
        out.put(KEY_ITEMS, list);

        int[] filtered = new int[storage.getFiltered().size()];
        for (int i = 0; i < filtered.length; i++) {
            filtered[i] = storage.getFiltered().get(i);
        }
        out.putIntArray(KEY_FILTERED, filtered);
        out.putInt(KEY_PAGE, storage.getPage());
        out.putString(KEY_QUERY, storage.getQuery());
        out.putByte(KEY_SORT, (byte) storage.getSortMode().ordinal());
    }

    /** Reads the full state of the store from {@code in}. */
    public static void read(ExtendedStorage storage, CompoundTag in) {
        List<ItemStack> items = new ArrayList<ItemStack>();
        ListTag list = in.getList(KEY_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ItemStack stack = ItemStack.EMPTY;
            if (!entry.isEmpty()) {
                try {
                    ItemStack loaded = ItemStack.of(entry);
                    if (!loaded.isEmpty()) {
                        stack = loaded;
                    }
                } catch (RuntimeException ignored) {
                    // A corrupt entry must not stop the whole inventory loading.
                }
            }
            items.add(stack);
        }

        List<Integer> filtered = new ArrayList<Integer>();
        for (int index : in.getIntArray(KEY_FILTERED)) {
            filtered.add(index);
        }

        ExtendedStorage.SortMode mode = ExtendedStorage.SortMode.ID;
        if (in.contains(KEY_SORT)) {
            byte raw = in.getByte(KEY_SORT);
            ExtendedStorage.SortMode[] modes = ExtendedStorage.SortMode.values();
            if (raw >= 0 && raw < modes.length) {
                mode = modes[raw];
            }
        }

        storage.replaceAll(items, filtered, in.getString(KEY_QUERY),
            in.getInt(KEY_PAGE), mode);
    }
}
//? }
