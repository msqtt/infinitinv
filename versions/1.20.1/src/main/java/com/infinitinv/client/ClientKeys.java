//? if>=1.20 {
package com.infinitinv.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Client only: the key binding that opens the infinite inventory. */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = com.infinitinv.InfiniteInv.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientKeys {

    public static final String CATEGORY = "key.categories.infinitinv";

    public static final KeyMapping OPEN_KEY = new KeyMapping(
        "key.infinitinv.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KEY);
    }
}
//? }
