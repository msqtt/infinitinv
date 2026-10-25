package com.infinitinv.mixin;

import com.infinitinv.network.ActionMessage;
import com.infinitinv.network.InfinitInvNet;
import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.IExtendedStorageHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a small page indicator to the vanilla inventory screen and lets the
 * mouse wheel walk through the pages of the store.
 *
 * <p>Nothing else about the screen is changed: the armour slots, the
 * crafting area and the hotbar look exactly as they always did.
 *
 * <p>The panel origin is derived from the window size instead of being
 * shadowed from {@code AbstractContainerScreen}: a shadowed inherited field
 * does not resolve at runtime, because the refmap only carries the method
 * mappings and the field is renamed by the obfuscation mapping.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    /** Size of the vanilla player inventory panel. */
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 166;

    @Inject(method = "renderLabels", at = @At("TAIL"))
    private void infinitinv$drawPageInfo(GuiGraphics graphics, int mouseX, int mouseY,
                                         CallbackInfo ci) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
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
            PANEL_WIDTH - 26 - mc.font.width(text), 74, 0x404040);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void infinitinv$scrollPage(double mouseX, double mouseY, double delta,
                                       CallbackInfo ci) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !(player instanceof IExtendedStorageHolder)) {
            return;
        }
        ExtendedStorage store =
            ((IExtendedStorageHolder) player).infinitinv$getExtendedStorage();
        if (store.size() == 0 || delta == 0) {
            return;
        }
        // The panel is centred on the screen.
        int leftPos = (mc.getWindow().getGuiScaledWidth() - PANEL_WIDTH) / 2;
        int topPos = (mc.getWindow().getGuiScaledHeight() - PANEL_HEIGHT) / 2;
        // The main inventory grid occupies this area of the panel.
        double gx = mouseX - leftPos;
        double gy = mouseY - topPos;
        if (gx < 8 || gx > 152 || gy < 84 || gy > 137) {
            return;
        }
        int target = store.getPage() + (delta > 0 ? -1 : 1);
        if (target < 0 || target >= store.pageCount()) {
            return;
        }
        InfinitInvNet.toServer(new ActionMessage(
            ActionMessage.Action.SET_PAGE, target, "", null, mc.player.level().registryAccess()));
        ci.cancel();
    }
}
