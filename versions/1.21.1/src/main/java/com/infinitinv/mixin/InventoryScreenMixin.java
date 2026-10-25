package com.infinitinv.mixin;

import com.infinitinv.network.ActionMessage;
import com.infinitinv.network.InfinitInvNet;
import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.IExtendedStorageHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a small page indicator to the vanilla inventory screen and lets the
 * mouse wheel walk through the pages of the store.
 *
 * <p>Nothing else about the screen is changed: the armour slots, the
 * crafting area and the hotbar look exactly as they always did.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Inject(method = "renderLabels", at = @At("TAIL"))
    private void infinitinv$drawPageInfo(GuiGraphics graphics, int mouseX, int mouseY,
                                         CallbackInfo ci) {
        LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null || !(player instanceof IExtendedStorageHolder)) {
            return;
        }
        ExtendedStorage store =
            ((IExtendedStorageHolder) player).infinitinv$getExtendedStorage();
        if (store.size() == 0) {
            return;
        }
        String text = (store.getPage() + 1) + "/" + store.pageCount();
        // The coordinates inside this method are relative to the panel origin.
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        graphics.drawString(mc.font, text, 150 - mc.font.width(text), 74, 0x404040);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void infinitinv$scrollPage(double mouseX, double mouseY, double delta,
                                       CallbackInfo ci) {
        LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null || !(player instanceof IExtendedStorageHolder)) {
            return;
        }
        ExtendedStorage store =
            ((IExtendedStorageHolder) player).infinitinv$getExtendedStorage();
        if (store.size() == 0 || delta == 0) {
            return;
        }
        InventoryScreen self = (InventoryScreen) (Object) this;
        // The main inventory grid occupies this area of the panel.
        double gx = mouseX - this.leftPos;
        double gy = mouseY - this.topPos;
        if (gx < 8 || gx > 152 || gy < 84 || gy > 137) {
            return;
        }
        int target = store.getPage() + (delta > 0 ? -1 : 1);
        if (target < 0 || target >= store.pageCount()) {
            return;
        }
        InfinitInvNet.toServer(new ActionMessage(
            ActionMessage.Action.SET_PAGE, target, "", null,
            player.level().registryAccess()));
        ci.cancel();
    }
}
