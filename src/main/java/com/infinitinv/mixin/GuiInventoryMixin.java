package com.infinitinv.mixin;

import com.infinitinv.network.ActionMessage;
import com.infinitinv.network.InfinitInvNet;
import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.IExtendedStorageHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a small page indicator and two page buttons to the vanilla inventory
 * screen, so the 27 main-inventory slots can be walked through the whole
 * store without leaving the screen.
 *
 * <p>Nothing else about the screen is changed: the armour slots, the
 * crafting area and the hotbar look exactly as they always did.
 */
@Mixin(GuiInventory.class)
public abstract class GuiInventoryMixin {

    @Inject(method = "drawGuiContainerForegroundLayer", at = @At("TAIL"))
    private void infinitinv$drawPageInfo(int mouseX, int mouseY, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null || !(mc.player instanceof IExtendedStorageHolder)) {
            return;
        }
        ExtendedStorage store =
            ((IExtendedStorageHolder) mc.player).infinitinv$getExtendedStorage();
        if (store.size() == 0) {
            return;
        }
        FontRenderer font = mc.fontRenderer;
        String text = (store.getPage() + 1) + "/" + store.pageCount();
        // The coordinates inside this method are relative to the panel origin.
        font.drawStringWithShadow(text, 150 - font.getStringWidth(text), 74, 0x404040);
        drawArrow(font, 8, "<");
        drawArrow(font, 20, ">");
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void infinitinv$pageButtons(int mouseX, int mouseY, int mouseButton,
                                        CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null || !(mc.player instanceof IExtendedStorageHolder)) {
            return;
        }
        ExtendedStorage store =
            ((IExtendedStorageHolder) mc.player).infinitinv$getExtendedStorage();
        if (store.size() == 0) {
            return;
        }
        GuiInventory self = (GuiInventory) (Object) this;
        int left = self.getGuiLeft();
        int top = self.getGuiTop();
        int delta = 0;
        if (mouseX >= left + 8 && mouseX < left + 18 && mouseY >= top + 68 && mouseY < top + 80) {
            delta = -1;
        } else if (mouseX >= left + 20 && mouseX < left + 30
            && mouseY >= top + 68 && mouseY < top + 80) {
            delta = 1;
        } else {
            return;
        }
        int target = store.getPage() + delta;
        if (target < 0 || target >= store.pageCount()) {
            return;
        }
        InfinitInvNet.CHANNEL.sendToServer(new ActionMessage(
            ActionMessage.Action.SET_PAGE, target, "", null));
        ci.cancel();
    }

    private void drawArrow(FontRenderer font, int x, String label) {
        font.drawStringWithShadow(label, x, 70, 0x404040);
    }
}
