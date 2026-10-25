package com.infinitinv.mixin;

import com.infinitinv.slot.ExtendedSlot;
import com.infinitinv.storage.IExtendedStorageHolder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Replaces the 27 main-inventory slots of the player container with slots
 * backed by the unbounded {@link com.infinitinv.storage.ExtendedStorage}.
 *
 * <p>The crafting grid, the crafting result, the armour slots and the hotbar
 * keep their vanilla slots, so their appearance and behaviour do not change
 * at all.
 */
@Mixin(ContainerPlayer.class)
public abstract class ContainerPlayerMixin {

    /**
     * Runs at the end of the container constructor, which is where 1.12.2
     * creates all of its slots.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void infinitinv$replaceMainSlots(InventoryPlayer inventory, boolean isLocalWorld,
                                              EntityPlayer player, CallbackInfo ci) {
        if (player == null) {
            return;
        }
        ContainerPlayer self = (ContainerPlayer) (Object) this;
        List<Slot> slots = self.inventorySlots;
        if (slots == null) {
            return;
        }
        IExtendedStorageHolder holder = (IExtendedStorageHolder) player;
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot == null || slot instanceof ExtendedSlot) {
                continue;
            }
            int index = slot.getSlotIndex();
            // The vanilla main inventory occupies the slot indices 9..35 of
            // the player inventory. Everything else (crafting 0..3, result
            // 40, armour 36..39, hotbar 0..8) is left alone.
            if (index < 9 || index > 35) {
                continue;
            }
            ExtendedSlot replacement = new ExtendedSlot(
                slot.inventory, holder, index - 9, slot.xPos, slot.yPos);
            replacement.slotNumber = slot.slotNumber;
            slots.set(i, replacement);
        }
    }
}
