package com.infinitinv.mixin;

import com.infinitinv.network.ActionMessage;
import com.infinitinv.network.InfinitInvNet;
import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.IExtendedStorageHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds a page indicator and two page buttons to the vanilla inventory
 * screen.
 *
 * <p>Nothing else about the screen is changed: the armour slots, the
 * crafting area and the hotbar look exactly as they always did.
 *
 * <p>Only methods that {@code InventoryScreen} declares itself are
 * injected into. The refmap records the mixin target as the owner of every
 * injected method, so an inherited method - {@code mouseScrolled} comes from
 * the {@code GuiEventListener} interface - cannot be found at runtime. The
 * panel origin is not shadowed either, for the same reason: a shadowed
 * inherited field is renamed by the obfuscation mapping and then does not
 * resolve.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    /** Size of the vanilla player inventory panel. */
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 166;

    /** Panel relative bounds of the two page buttons. */
    private static final int PREV_X = 146;
    private static final int NEXT_X = 160;
    private static final int BUTTON_Y = 68;
    private static final int BUTTON_SIZE = 10;

    @Inject(method = "renderLabels", at = @At("TAIL"))
    private void infinitinv$drawPageInfo(GuiGraphics graphics, int mouseX, int mouseY,
                                         CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !(player instanceof IExtendedStorageHolder)) {
            return;
        }
        ExtendedStorage store =
            ((IExtendedStorageHolder) player).infinitinv$getExtendedStorage();
        if (store.size() == 0) {
            return;
        }
        String text = (store.getPage() + 1) + "/" + store.pageCount();
        // renderLabels runs inside the translated panel, so these coordinates
        // are relative to the top left corner of the inventory panel.
        graphics.drawString(mc.font, text,
            PANEL_WIDTH - 30 - mc.font.width(text), 70, 0x404040);
        graphics.drawString(mc.font, "<", PREV_X + 2, BUTTON_Y + 1, 0x404040);
        graphics.drawString(mc.font, ">", NEXT_X + 2, BUTTON_Y + 1, 0x404040);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void infinitinv$clickPageButton(double mouseX, double mouseY, int button,
                                             CallbackInfoReturnable<Boolean> ci) {
        if (button != 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !(player instanceof IExtendedStorageHolder)) {
            return;
        }
        ExtendedStorage store =
            ((IExtendedStorageHolder) player).infinitinv$getExtendedStorage();
        if (store.size() == 0) {
            return;
        }
        // The panel is centred on the screen.
        int leftPos = (mc.getWindow().getGuiScaledWidth() - PANEL_WIDTH) / 2;
        int topPos = (mc.getWindow().getGuiScaledHeight() - PANEL_HEIGHT) / 2;
        double gx = mouseX - leftPos;
        double gy = mouseY - topPos;
        if (gy < BUTTON_Y || gy >= BUTTON_Y + BUTTON_SIZE) {
            return;
        }
        int page = store.getPage();
        if (gx >= PREV_X && gx < PREV_X + BUTTON_SIZE) {
            if (page > 0) {
                InfinitInvNet.toServer(new ActionMessage(
                    ActionMessage.Action.SET_PAGE, page - 1, "", null, mc.player.level().registryAccess()));
                ci.cancel();
            }
            return;
        }
        if (gx >= NEXT_X && gx < NEXT_X + BUTTON_SIZE) {
            if (page + 1 < store.pageCount()) {
                InfinitInvNet.toServer(new ActionMessage(
                    ActionMessage.Action.SET_PAGE, page + 1, "", null, mc.player.level().registryAccess()));
                ci.cancel();
            }
        }
    }
}
