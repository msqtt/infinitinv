package com.infinitinv.client;

import com.infinitinv.client.gui.GuiInfiniteInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/** Client only glue: the key binding that opens the infinite inventory. */
public class ClientHooks {

    public static final String CATEGORY = "Infinite Inventory";

    public static KeyBinding openKey;

    public static void init() {
        if (openKey == null) {
            openKey = new KeyBinding("Open infinite inventory", Keyboard.KEY_K, CATEGORY);
            ClientRegistry.registerKeyBinding(openKey);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new ClientHooks());
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null || mc.currentScreen != null) {
            return;
        }
        if (openKey != null && openKey.isPressed()) {
            mc.displayGuiScreen(new GuiInfiniteInventory(mc.player));
        }
    }
}
