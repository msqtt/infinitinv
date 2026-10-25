//? if>=1.20 {
package com.infinitinv.client.gui;

import com.infinitinv.network.ActionMessage;
import com.infinitinv.network.InfinitInvNet;
import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.IExtendedStorageHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The infinite inventory screen.
 *
 * <p>It can show two things:
 * <ul>
 *   <li><b>Stored</b> — every item currently in the player's unbounded store,
 *       filtered by the search box.</li>
 *   <li><b>All items</b> — every item registered in the game, so the whole
 *       content can be browsed page by page. Clicking one puts it into the
 *       store.</li>
 * </ul>
 */
public class StoreScreen extends Screen {

    private static final int GRID_COLUMNS = 9;
    private static final int GRID_ROWS = 5;
    private static final int CELL = 18;
    private static final int GRID_SIZE = GRID_COLUMNS * GRID_ROWS;

    private static final int PANEL_WIDTH = 256;
    private static final int PANEL_HEIGHT = 200;

    private static final int COLOR_BACKGROUND = 0xFFC6C6C6;
    private static final int COLOR_PANEL = 0xFF8B8B8B;
    private static final int COLOR_TEXT = 0x404040;
    private static final int COLOR_TEXT_LIGHT = 0xFFFFFF;

    private final Player player;

    private String query = "";
    private boolean showAllItems = false;
    private int page = 0;

    private int left;
    private int top;

    private EditBox search;
    private final List<ItemStack> allItems = new ArrayList<ItemStack>();

    public StoreScreen(Player player) {
        super(Component.literal("Infinite Inventory"));
        this.player = player;
    }

    private ExtendedStorage storage() {
        if (player == null || !(player instanceof IExtendedStorageHolder)) {
            return null;
        }
        return ((IExtendedStorageHolder) player).infinitinv$getExtendedStorage();
    }

    @Override
    protected void init() {
        left = (this.width - PANEL_WIDTH) / 2;
        top = (this.height - PANEL_HEIGHT) / 2;

        ExtendedStorage store = storage();
        if (store != null) {
            query = store.getQuery();
        }

        search = new EditBox(this.font, left + 8, top + 20, 150, 16,
            Component.literal("Search"));
        search.setMaxLength(64);
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            page = 0;
            sendQuery();
        });
        addRenderableWidget(search);

        addRenderableWidget(Button.builder(Component.literal("Sort"), button -> {
            ExtendedStorage s = storage();
            if (s != null) {
                ExtendedStorage.SortMode next = s.getSortMode().next();
                InfinitInvNet.toServer(new ActionMessage(
                    ActionMessage.Action.SET_SORT, next.ordinal(), "", null));
            }
        }).bounds(left + 162, top + 20, 86, 16).build());

        addRenderableWidget(Button.builder(Component.literal("Stored"), button -> {
            showAllItems = !showAllItems;
            page = 0;
        }).bounds(left + 8, top + 40, 86, 20).build());

        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page = Math.max(0, page - 1);
        }).bounds(left + 100, top + 40, 24, 20).build());

        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            page = Math.min(pageCount(currentList().size()) - 1, page + 1);
        }).bounds(left + 128, top + 40, 24, 20).build());

        buildAllItems();
    }

    private void buildAllItems() {
        allItems.clear();
        try {
            for (ResourceLocation key : BuiltInRegistries.ITEM.keySet()) {
                Item item = BuiltInRegistries.ITEM.get(key);
                if (item == null) {
                    continue;
                }
                allItems.add(new ItemStack(item));
            }
        } catch (RuntimeException e) {
            // An empty registry must not break the screen.
        }
        Collections.sort(allItems, new Comparator<ItemStack>() {
            @Override
            public int compare(ItemStack a, ItemStack b) {
                return ExtendedStorage.registryName(a.getItem())
                    .compareTo(ExtendedStorage.registryName(b.getItem()));
            }
        });
    }

    private void sendQuery() {
        InfinitInvNet.toServer(new ActionMessage(
            ActionMessage.Action.SET_QUERY, 0, query, null));
    }

    private List<ItemStack> currentList() {
        List<ItemStack> result = new ArrayList<ItemStack>();
        String needle = query.trim().toLowerCase();
        if (showAllItems) {
            for (ItemStack stack : allItems) {
                if (needle.isEmpty()
                    || ExtendedStorage.registryName(stack.getItem()).toLowerCase().contains(needle)) {
                    result.add(stack);
                }
            }
        } else {
            ExtendedStorage store = storage();
            if (store == null) {
                return result;
            }
            for (Integer index : store.getFiltered()) {
                ItemStack stack = store.getStack(index);
                if (!stack.isEmpty()) {
                    result.add(stack);
                }
            }
        }
        return result;
    }

    private int pageCount(int listSize) {
        int pages = (listSize + GRID_SIZE - 1) / GRID_SIZE;
        return pages <= 0 ? 1 : pages;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int gridX = left + 8;
        int gridY = top + 64;
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (mx >= gridX && mx < gridX + GRID_COLUMNS * CELL
            && my >= gridY && my < gridY + GRID_ROWS * CELL) {
            int column = (mx - gridX) / CELL;
            int row = (my - gridY) / CELL;
            clickItem(page * GRID_SIZE + row * GRID_COLUMNS + column);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickItem(int listIndex) {
        if (showAllItems) {
            List<ItemStack> list = currentList();
            if (listIndex < 0 || listIndex >= list.size()) {
                return;
            }
            ItemStack stack = list.get(listIndex).copy();
            stack.setCount(1);
            InfinitInvNet.toServer(new ActionMessage(
                ActionMessage.Action.INSERT, 0, "", stack));
            return;
        }
        ExtendedStorage store = storage();
        if (store == null) {
            return;
        }
        List<Integer> filtered = store.getFiltered();
        if (listIndex < 0 || listIndex >= filtered.size()) {
            return;
        }
        InfinitInvNet.toServer(new ActionMessage(
            ActionMessage.Action.REMOVE, filtered.get(listIndex), "", null));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_BACKGROUND);
        graphics.fill(left + 2, top + 2, left + PANEL_WIDTH - 2, top + PANEL_HEIGHT - 2, COLOR_PANEL);

        graphics.drawString(this.font, "Infinite Inventory", left + 8, top + 7, COLOR_TEXT_LIGHT);
        graphics.drawString(this.font, showAllItems ? "All items" : "Stored items",
            left + 14, top + 46, COLOR_TEXT_LIGHT);

        List<ItemStack> list = currentList();
        int pages = pageCount(list.size());
        if (page >= pages) {
            page = pages - 1;
        }

        int gridX = left + 8;
        int gridY = top + 64;
        ItemStack hovered = null;
        for (int i = 0; i < GRID_SIZE; i++) {
            int listIndex = page * GRID_SIZE + i;
            int column = i % GRID_COLUMNS;
            int row = i / GRID_COLUMNS;
            int x = gridX + column * CELL;
            int y = gridY + row * CELL;
            graphics.fill(x, y, x + CELL - 1, y + CELL - 1, 0xFF8B8B8B);
            graphics.fill(x + 1, y + 1, x + CELL - 2, y + CELL - 2, 0xFF373737);
            if (listIndex >= list.size()) {
                continue;
            }
            ItemStack stack = list.get(listIndex);
            if (stack.isEmpty()) {
                continue;
            }
            graphics.renderItem(stack, x + 1, y + 1);
            if (stack.getCount() > 1) {
                graphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
            }
            if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
                hovered = stack;
            }
        }

        String pageText = (page + 1) + " / " + pages + "   (" + list.size() + " items)";
        graphics.drawString(this.font, pageText, left + 8, top + 172, COLOR_TEXT_LIGHT);

        if (hovered != null) {
            graphics.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }
}
//? }
