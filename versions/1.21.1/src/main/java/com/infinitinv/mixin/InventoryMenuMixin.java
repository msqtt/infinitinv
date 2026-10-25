package com.infinitinv.mixin;

import com.infinitinv.slot.ExtendedSlot;
import com.infinitinv.storage.IExtendedStorageHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the 27 main-inventory slots of the player menu with slots backed
 * by the unbounded {@link com.infinitinv.storage.ExtendedStorage}.
 *
 * <p>The crafting grid, the crafting result, the armour slots and the hotbar
 * keep their vanilla slots, so their appearance and behaviour do not change.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    /**
     * Runs at the end of the menu constructor, which is where the slots are
     * created.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void infinitinv$replaceMainSlots(Inventory inventory, boolean isLocalWorld,
                                              Player player, CallbackInfo ci) {
        if (player == null) {
            return;
        }
        InventoryMenu self = (InventoryMenu) (Object) this;
        IExtendedStorageHolder holder = (IExtendedStorageHolder) player;
        for (int i = 0; i < self.slots.size(); i++) {
            Slot slot = self.slots.get(i);
            if (slot == null || slot instanceof ExtendedSlot) {
                continue;
            }
            // The vanilla main inventory occupies the slot indices 9..35 of the
            // player inventory. Everything else (crafting, result, armour,
            // hotbar) is left alone.
            if (slot.container != inventory) {
                continue;
            }
            if (slot.index < 9 || slot.index > 35) {
                continue;
            }
            ExtendedSlot replacement = new ExtendedSlot(
                slot.container, holder, slot.index - 9, slot.x, slot.y);
            replacement.index = slot.index;
            self.slots.set(i, replacement);
        }
    }
}
