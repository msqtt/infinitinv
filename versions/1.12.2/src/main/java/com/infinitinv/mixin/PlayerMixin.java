package com.infinitinv.mixin;

import com.infinitinv.storage.ExtendedStorage;
import com.infinitinv.storage.IExtendedStorageHolder;
import com.infinitinv.storage.StorageIO;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Attaches an {@link ExtendedStorage} to every player and persists it in the
 * player's NBT, so the infinite inventory survives logout, death and server
 * restarts.
 */
@Mixin(EntityPlayer.class)
public abstract class PlayerMixin implements IExtendedStorageHolder {

    private static final String KEY_STORAGE = "infinitinv:storage";
    private static final String KEY_MIGRATED = "infinitinv:migrated";

    @Unique
    private ExtendedStorage infinitinv$storage;

    @Unique
    private boolean infinitinv$migrated;

    @Override
    public ExtendedStorage infinitinv$getExtendedStorage() {
        if (infinitinv$storage == null) {
            infinitinv$storage = new ExtendedStorage();
        }
        return infinitinv$storage;
    }

    @Inject(method = "writeEntityToNBT", at = @At("TAIL"))
    private void infinitinv$writeStorage(NBTTagCompound compound, CallbackInfo ci) {
        NBTTagCompound storageTag = new NBTTagCompound();
        StorageIO.write(infinitinv$getExtendedStorage(), storageTag);
        compound.setTag(KEY_STORAGE, storageTag);
        compound.setBoolean(KEY_MIGRATED, true);
    }

    @Inject(method = "readEntityFromNBT", at = @At("TAIL"))
    private void infinitinv$readStorage(NBTTagCompound compound, CallbackInfo ci) {
        if (compound.hasKey(KEY_STORAGE, 10)) {
            StorageIO.read(infinitinv$getExtendedStorage(), compound.getCompoundTag(KEY_STORAGE));
        }
        infinitinv$migrated = compound.getBoolean(KEY_MIGRATED);

        // First time this player is seen by the mod: move whatever sits in the
        // vanilla 27 main-inventory slots into the extended storage so those
        // items are not stranded once the slots are re-purposed.
        if (!infinitinv$migrated) {
            infinitinv$migrated = true;
            migrateVanillaInventory();
        }
    }

    @Unique
    private void migrateVanillaInventory() {
        EntityPlayer self = (EntityPlayer) (Object) this;
        if (self.inventory == null) {
            return;
        }
        // Slots 0-8 are the hotbar and must stay vanilla; slots 9-35 are the
        // main inventory rows that the extended storage takes over.
        for (int i = 9; i < 36; i++) {
            ItemStack stack = self.inventory.getStackInSlot(i);
            if (stack != null && !stack.isEmpty()) {
                infinitinv$getExtendedStorage().add(stack.copy());
                self.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
            }
        }
    }
}
