package com.infinitinv.storage;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes an {@link ExtendedStorage} to and from NBT.
 *
 * <p>The item list is written index aligned: every slot of the store gets an
 * entry, empty ones are written as an empty compound. That keeps the absolute
 * indices identical on both sides of the connection, which is what the
 * network protocol relies on.
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
    public static void write(ExtendedStorage storage, NBTTagCompound out) {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < storage.size(); i++) {
            ItemStack stack = storage.getStack(i);
            if (stack == null || stack.isEmpty()) {
                list.appendTag(new NBTTagCompound());
            } else {
                list.appendTag(stack.writeToNBT(new NBTTagCompound()));
            }
        }
        out.setTag(KEY_ITEMS, list);

        int[] filtered = new int[storage.getFiltered().size()];
        for (int i = 0; i < filtered.length; i++) {
            filtered[i] = storage.getFiltered().get(i);
        }
        out.setIntArray(KEY_FILTERED, filtered);
        out.setInteger(KEY_PAGE, storage.getPage());
        out.setString(KEY_QUERY, storage.getQuery());
        out.setByte(KEY_SORT, (byte) storage.getSortMode().ordinal());
    }

    /** Reads the full state of the store from {@code in}. */
    public static void read(ExtendedStorage storage, NBTTagCompound in) {
        List<ItemStack> items = new ArrayList<ItemStack>();
        NBTTagList list = in.getTagList(KEY_ITEMS, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            ItemStack stack = ItemStack.EMPTY;
            if (entry != null && entry.hasKey("id")) {
                try {
                    ItemStack loaded = new ItemStack(entry);
                    if (loaded.getItem() != null) {
                        stack = loaded;
                    }
                } catch (RuntimeException ignored) {
                    // A corrupt entry must not stop the whole inventory loading.
                }
            }
            items.add(stack);
        }

        List<Integer> filtered = new ArrayList<Integer>();
        int[] filteredArray = in.getIntArray(KEY_FILTERED);
        for (int index : filteredArray) {
            filtered.add(index);
        }

        ExtendedStorage.SortMode mode = ExtendedStorage.SortMode.ID;
        if (in.hasKey(KEY_SORT)) {
            byte raw = in.getByte(KEY_SORT);
            ExtendedStorage.SortMode[] modes = ExtendedStorage.SortMode.values();
            if (raw >= 0 && raw < modes.length) {
                mode = modes[raw];
            }
        }

        storage.replaceAll(items, filtered, in.getString(KEY_QUERY),
            in.getInteger(KEY_PAGE), mode);
    }
}
