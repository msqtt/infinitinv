package com.infinitinv.client.gui;

import com.infinitinv.network.ActionMessage;
import com.infinitinv.network.InfinitInvNet;
import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.IExtendedStorageHolder;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
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
 *
 * <p>The search box filters the list, the sort button changes the order and
 * the page buttons walk through the result. Everything is resolved on the
 * server, so the two sides never disagree about what is where.
 */
public class GuiInfiniteInventory extends GuiScreen {

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
    private static final int COLOR_FIELD = 0xFF000000;

    private final EntityPlayer player;

    /** Panel origin, computed from the screen size. */
    private int guiLeft;
    private int guiTop;

    /** Search text, kept in sync with the server. */
    private String query = "";
    private boolean queryDirty = false;

    /** Which list is shown. */
    private boolean showAllItems = false;

    /** Page of the grid. */
    private int page = 0;

    /** The item under the mouse, for the tooltip. */
    private ItemStack hovered = null;

    private final List<ItemStack> allItems = new ArrayList<ItemStack>();

    public GuiInfiniteInventory(EntityPlayer player) {
        this.player = player;
        this.mc = net.minecraft.client.Minecraft.getMinecraft();
        buildAllItems();
    }

    // ------------------------------------------------------------- lifecycle

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.mc = net.minecraft.client.Minecraft.getMinecraft();
        this.guiLeft = (this.mc.displayWidth - PANEL_WIDTH) / 2;
        this.guiTop = (this.mc.displayHeight - PANEL_HEIGHT) / 2;
        if (storage() != null) {
            this.query = storage().getQuery();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private ExtendedStorage storage() {
        if (player == null || !(player instanceof IExtendedStorageHolder)) {
            return null;
        }
        return ((IExtendedStorageHolder) player).infinitinv$getExtendedStorage();
    }

    // ------------------------------------------------------------- the lists

    private void buildAllItems() {
        allItems.clear();
        try {
            for (ResourceLocation key : Item.REGISTRY.getKeys()) {
                Item item = Item.REGISTRY.getObject(key);
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
                return key(a).compareTo(key(b));
            }
        });
    }

    private static String key(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.getItem().getRegistryName() == null) {
            return "";
        }
        return stack.getItem().getRegistryName().toString();
    }

    /** The entries that are currently shown, already filtered. */
    private List<ItemStack> currentList() {
        List<ItemStack> result = new ArrayList<ItemStack>();
        String needle = query.trim().toLowerCase();
        if (showAllItems) {
            for (ItemStack stack : allItems) {
                if (needle.isEmpty() || matches(stack, needle)) {
                    result.add(stack);
                }
            }
        } else {
            ExtendedStorage store = storage();
            if (store == null) {
                return result;
            }
            List<Integer> filtered = store.getFiltered();
            for (Integer index : filtered) {
                ItemStack stack = store.getStack(index);
                if (stack != null && !stack.isEmpty()) {
                    result.add(stack);
                }
            }
        }
        return result;
    }

    private static boolean matches(ItemStack stack, String needle) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        if (key(stack).toLowerCase().contains(needle)) {
            return true;
        }
        String unlocalized = stack.getItem().getItemStackDisplayName(stack);
        return unlocalized != null && unlocalized.toLowerCase().contains(needle);
    }

    private int pageCount(int listSize) {
        int pages = (listSize + GRID_SIZE - 1) / GRID_SIZE;
        return pages <= 0 ? 1 : pages;
    }

    // ----------------------------------------------------------------- input

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == Keyboard.KEY_BACK) {
            if (!query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                queryDirty = true;
                page = 0;
            }
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN) {
            flushQuery();
            return;
        }
        if (typedChar >= ' ' && typedChar != 127 && query.length() < 64) {
            query = query + typedChar;
            queryDirty = true;
            page = 0;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int left = this.guiLeft;
        int top = this.guiTop;

        // sort button
        if (inside(mouseX, mouseY, left + 160, top + 28, 88, 20)) {
            ExtendedStorage store = storage();
            if (store != null) {
                ExtendedStorage.SortMode next = store.getSortMode().next();
                InfinitInvNet.CHANNEL.sendToServer(new ActionMessage(
                    ActionMessage.Action.SET_SORT, next.ordinal(), "", null));
            }
            return;
        }
        // mode button
        if (inside(mouseX, mouseY, left + 8, top + 28, 88, 20)) {
            showAllItems = !showAllItems;
            page = 0;
            return;
        }
        // previous page
        if (inside(mouseX, mouseY, left + 8, top + 172, 20, 20)) {
            page = Math.max(0, page - 1);
            return;
        }
        // next page
        if (inside(mouseX, mouseY, left + 228, top + 172, 20, 20)) {
            page = Math.min(pageCount(currentList().size()) - 1, page + 1);
            return;
        }
        // item grid
        int gridX = left + 8;
        int gridY = top + 56;
        if (mouseX >= gridX && mouseX < gridX + GRID_COLUMNS * CELL
            && mouseY >= gridY && mouseY < gridY + GRID_ROWS * CELL) {
            int column = (mouseX - gridX) / CELL;
            int row = (mouseY - gridY) / CELL;
            int index = page * GRID_SIZE + row * GRID_COLUMNS + column;
            clickItem(index);
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void clickItem(int listIndex) {
        if (showAllItems) {
            List<ItemStack> list = currentList();
            if (listIndex < 0 || listIndex >= list.size()) {
                return;
            }
            ItemStack stack = list.get(listIndex).copy();
            stack.setCount(1);
            InfinitInvNet.CHANNEL.sendToServer(new ActionMessage(
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
        InfinitInvNet.CHANNEL.sendToServer(new ActionMessage(
            ActionMessage.Action.REMOVE, filtered.get(listIndex), "", null));
    }

    private void flushQuery() {
        if (queryDirty) {
            queryDirty = false;
            InfinitInvNet.CHANNEL.sendToServer(new ActionMessage(
                ActionMessage.Action.SET_QUERY, 0, query, null));
        }
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    // --------------------------------------------------------------- drawing

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        flushQuery();

        this.drawDefaultBackground();

        int left = this.guiLeft;
        int top = this.guiTop;

        drawRect(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_BACKGROUND);
        drawRect(left + 2, top + 2, left + PANEL_WIDTH - 2, top + PANEL_HEIGHT - 2, COLOR_PANEL);

        net.minecraft.client.gui.FontRenderer font = this.mc.fontRenderer;

        font.drawStringWithShadow("Infinite Inventory", left + 8, top + 7, COLOR_TEXT_LIGHT);
        font.drawString("Search:", left + 8, top + 20, COLOR_TEXT_LIGHT);
        font.drawString(showAllItems ? "All items" : "Stored items",
            left + 14, top + 34, COLOR_TEXT_LIGHT);

        // search field
        int fieldX = left + 52;
        int fieldY = top + 17;
        int fieldW = 100;
        int fieldH = 14;
        drawRect(fieldX - 1, fieldY - 1, fieldX + fieldW + 1, fieldY + fieldH + 1, 0xFF000000);
        drawRect(fieldX, fieldY, fieldX + fieldW, fieldY + fieldH, COLOR_FIELD);
        font.drawString(query, fieldX + 3, fieldY + 3, 0xE0E0E0);
        int cursorX = fieldX + 3 + font.getStringWidth(query);
        drawRect(cursorX, fieldY + 2, cursorX + 1, fieldY + fieldH - 2, 0xE0E0E0);

        // sort button
        ExtendedStorage store = storage();
        String sortLabel = "Sort: " + (store == null ? "ID" : store.getSortMode().label());
        drawButton(left + 160, top + 28, 88, 20, sortLabel, mouseX, mouseY);
        drawButton(left + 8, top + 28, 88, 20,
            showAllItems ? "Stored items" : "All items", mouseX, mouseY);

        // grid
        int gridX = left + 8;
        int gridY = top + 56;
        List<ItemStack> list = currentList();
        int pages = pageCount(list.size());
        if (page >= pages) {
            page = pages - 1;
        }

        hovered = null;
        for (int i = 0; i < GRID_SIZE; i++) {
            int listIndex = page * GRID_SIZE + i;
            int column = i % GRID_COLUMNS;
            int row = i / GRID_COLUMNS;
            int x = gridX + column * CELL;
            int y = gridY + row * CELL;
            drawRect(x, y, x + CELL - 1, y + CELL - 1, 0xFF8B8B8B);
            drawRect(x + 1, y + 1, x + CELL - 2, y + CELL - 2, 0xFF373737);
            if (listIndex >= list.size()) {
                continue;
            }
            ItemStack stack = list.get(listIndex);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            this.mc.getRenderItem().renderItemIntoGUI(stack, x + 1, y + 1);
            if (stack.getCount() > 1) {
                this.mc.getRenderItem().renderItemOverlays(font, stack, x + 1, y + 1);
            }
            if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
                hovered = stack;
            }
        }

        // pagination
        drawButton(left + 8, top + 172, 20, 20, "<", mouseX, mouseY);
        drawButton(left + 228, top + 172, 20, 20, ">", mouseX, mouseY);
        String pageText = (page + 1) + " / " + pages + "   (" + list.size() + " items)";
        font.drawString(pageText, left + 36, top + 180, COLOR_TEXT_LIGHT);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (hovered != null) {
            drawTooltip(hovered, mouseX, mouseY);
        }
    }

    private void drawButton(int x, int y, int w, int h, String label, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        drawRect(x, y, x + w, y + h, hovered ? 0xFFA6A6A6 : 0xFF8B8B8B);
        drawRect(x, y, x + w, y + 1, 0xFFFFFFFF);
        drawRect(x, y + h - 1, x + w, y + h, 0xFF555555);
        drawRect(x, y, x + 1, y + h, 0xFFFFFFFF);
        drawRect(x + w - 1, y, x + w, y + h, 0xFF555555);
        net.minecraft.client.gui.FontRenderer font = this.mc.fontRenderer;
        font.drawString(label, x + (w - font.getStringWidth(label)) / 2, y + (h - 8) / 2 + 1,
            COLOR_TEXT_LIGHT);
    }

    private void drawTooltip(ItemStack stack, int mouseX, int mouseY) {
        net.minecraft.client.gui.FontRenderer font = this.mc.fontRenderer;
        String name = stack.getDisplayName();
        String count = "x" + stack.getCount();
        int w = Math.max(font.getStringWidth(name), font.getStringWidth(count)) + 8;
        int h = 20;
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + w > this.guiLeft + PANEL_WIDTH - 4) {
            x = mouseX - w - 4;
        }
        if (y + h > this.guiTop + PANEL_HEIGHT - 4) {
            y = this.guiTop + PANEL_HEIGHT - h - 4;
        }
        drawRect(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF100010);
        drawRect(x, y, x + w, y + h, 0xF0100010);
        font.drawString(name, x + 4, y + 3, 0xFFFFFF);
        font.drawString(count, x + 4, y + 12, 0xAAAAAA);
    }

    @Override
    public void onGuiClosed() {
        flushQuery();
    }
}
