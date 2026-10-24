package com.infinitinv.storage;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The unbounded, server-authoritative item store that replaces the 27 main
 * inventory slots.
 *
 * <p>Items live in a plain {@link ArrayList}; the list is allowed to grow
 * without an upper bound. The GUI only ever shows a window of
 * {@link #PAGE_SIZE} entries, which is why the store is also addressable by
 * an <em>absolute</em> index that never depends on the visible page.
 *
 * <p>The same class is used on both sides of the connection. On the server it
 * holds the authoritative data; on the client it is a mirror that is replaced
 * wholesale by every {@link com.infinitinv.network.SyncStorageMessage}.
 */
public class ExtendedStorage {

    /** Number of entries shown on one page — matches the vanilla 3x9 grid. */
    public static final int PAGE_SIZE = 27;

    /** How the store is ordered. */
    public enum SortMode {
        /** Registry id, e.g. {@code minecraft:diamond_sword}. */
        ID("ID"),
        /** Localised display name. */
        NAME("Name"),
        /** Item class, e.g. all swords grouped together. */
        TYPE("Type");

        private final String label;

        SortMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public SortMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private final List<ItemStack> items = new ArrayList<ItemStack>();

    /** Absolute indices of the entries that match {@link #query}. */
    private final List<Integer> filtered = new ArrayList<Integer>();

    private String query = "";
    private int page = 0;
    private SortMode sortMode = SortMode.ID;

    /** Set whenever the content changed and the client needs a fresh copy. */
    private boolean dirty = true;

    // ------------------------------------------------------------------ size

    public int size() {
        return items.size();
    }

    public int filteredSize() {
        return filtered.size();
    }

    public int pageCount() {
        int pages = (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        return pages <= 0 ? 1 : pages;
    }

    public int getPage() {
        return page;
    }

    public String getQuery() {
        return query;
    }

    public SortMode getSortMode() {
        return sortMode;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    public void markDirty() {
        dirty = true;
    }

    public List<Integer> getFiltered() {
        return filtered;
    }

    // --------------------------------------------------------------- access

    public ItemStack getStack(int absoluteIndex) {
        if (absoluteIndex < 0 || absoluteIndex >= items.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(absoluteIndex);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public void setStack(int absoluteIndex, ItemStack stack) {
        if (absoluteIndex < 0 || absoluteIndex >= items.size()) {
            return;
        }
        items.set(absoluteIndex, stack == null ? ItemStack.EMPTY : stack);
        recomputeFilter();
        dirty = true;
    }

    /** Appends a stack, merging it into an existing identical entry if there is one. */
    public void add(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            ItemStack existing = items.get(i);
            if (existing != null && !existing.isEmpty() && areSame(existing, stack)) {
                int max = Math.max(existing.getMaxStackSize(), stack.getMaxStackSize());
                int room = max - existing.getCount();
                if (room > 0) {
                    int move = Math.min(room, stack.getCount());
                    existing.setCount(existing.getCount() + move);
                    stack.shrink(move);
                    if (stack.isEmpty()) {
                        recomputeFilter();
                        dirty = true;
                        return;
                    }
                }
            }
        }
        items.add(stack.copy());
        recomputeFilter();
        dirty = true;
    }

    /** Removes a single item from the given absolute index and returns it. */
    public ItemStack removeOne(int absoluteIndex) {
        if (absoluteIndex < 0 || absoluteIndex >= items.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(absoluteIndex);
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = stack.splitStack(1);
        if (stack.isEmpty()) {
            items.remove(absoluteIndex);
        }
        recomputeFilter();
        dirty = true;
        return taken;
    }

    private static boolean areSame(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem()
            && a.getItemDamage() == b.getItemDamage()
            && a.getTagCompound() == null && b.getTagCompound() == null;
    }

    // --------------------------------------------------------------- filter

    public void setQuery(String newQuery) {
        this.query = newQuery == null ? "" : newQuery;
        recomputeFilter();
        this.page = 0;
        dirty = true;
    }

    public void setPage(int newPage) {
        int max = pageCount() - 1;
        this.page = Math.max(0, Math.min(newPage, max));
        dirty = true;
    }

    /** Sets the sort mode without re-sorting or resetting the page. */
    public void setSortModeQuietly(SortMode mode) {
        this.sortMode = mode == null ? SortMode.ID : mode;
    }

    public void setSortMode(SortMode mode) {
        this.sortMode = mode == null ? SortMode.ID : mode;
        applySort();
        this.page = 0;
        dirty = true;
    }

    /**
     * Recomputes the list of matching absolute indices.
     *
     * <p>Matching is done against the registry id and the unlocalised name so
     * that the server and the client always agree regardless of the locale
     * they run in.
     */
    public void recomputeFilter() {
        filtered.clear();
        String needle = query.trim().toLowerCase();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (needle.isEmpty() || matches(stack, needle)) {
                filtered.add(i);
            }
        }
    }

    private static boolean matches(ItemStack stack, String needle) {
        Item item = stack.getItem();
        if (item == null) {
            return false;
        }
        if (item.getRegistryName() != null
            && item.getRegistryName().toString().toLowerCase().contains(needle)) {
            return true;
        }
        String unlocalized = item.getItemStackDisplayName(stack);
        return unlocalized != null && unlocalized.toLowerCase().contains(needle);
    }

    // ----------------------------------------------------------------- sort

    private void applySort() {
        final Comparator<ItemStack> comparator = new Comparator<ItemStack>() {
            @Override
            public int compare(ItemStack a, ItemStack b) {
                int result = sortKey(a).compareTo(sortKey(b));
                if (result != 0) {
                    return result;
                }
                return Integer.compare(a.getItemDamage(), b.getItemDamage());
            }
        };
        Collections.sort(items, comparator);
        recomputeFilter();
    }

    private String sortKey(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) {
            return "";
        }
        switch (sortMode) {
            case NAME:
                return stack.getDisplayName();
            case TYPE:
                return stack.getItem().getClass().getSimpleName() + " "
                    + (stack.getItem().getRegistryName() == null
                        ? "" : stack.getItem().getRegistryName().toString());
            case ID:
            default:
                return stack.getItem().getRegistryName() == null
                    ? "" : stack.getItem().getRegistryName().toString();
        }
    }

    // ------------------------------------------------------------- indexing

    /**
     * Maps a position inside the visible page to the absolute index it shows,
     * or {@code -1} when the position is empty.
     */
    public int absoluteIndexForDisplay(int displayIndex) {
        int listPos = page * PAGE_SIZE + displayIndex;
        if (listPos < 0 || listPos >= filtered.size()) {
            return -1;
        }
        return filtered.get(listPos);
    }

    /**
     * Maps an absolute index back to the position it occupies on the current
     * page, or {@code -1} when the entry is filtered out.
     */
    public int displayIndexForAbsolute(int absoluteIndex) {
        int listPos = filtered.indexOf(absoluteIndex);
        if (listPos < 0) {
            return -1;
        }
        return listPos - page * PAGE_SIZE;
    }

    // -------------------------------------------------------------- mirror

    /** Replaces the whole content — used by the client when a snapshot arrives. */
    public void replaceAll(List<ItemStack> newItems, List<Integer> newFiltered,
                           String newQuery, int newPage, SortMode newSort) {
        items.clear();
        if (newItems != null) {
            for (ItemStack stack : newItems) {
                items.add(stack == null ? ItemStack.EMPTY : stack);
            }
        }
        this.query = newQuery == null ? "" : newQuery;
        this.sortMode = newSort == null ? SortMode.ID : newSort;
        this.page = newPage;
        filtered.clear();
        if (newFiltered != null) {
            filtered.addAll(newFiltered);
        } else {
            recomputeFilter();
        }
        dirty = false;
    }

    /** Copies page/query/sort state (but not the content) from another store. */
    public void copyViewState(ExtendedStorage other) {
        if (other == null) {
            return;
        }
        this.query = other.query;
        this.page = other.page;
        this.sortMode = other.sortMode;
        this.filtered.clear();
        this.filtered.addAll(other.filtered);
    }
}
