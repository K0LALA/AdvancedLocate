package fr.kolala.advancedlocate.client;

import fr.kolala.advancedlocate.client.screen.LocatorScreen;
import fr.kolala.advancedlocate.network.packet.ResponseMapIdPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.type.MapIdComponent;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class AdvancedLocateClient implements ClientModInitializer {
    public static KeyBinding locatorKeybinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.advanced_locate.locator",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "category.advanced_locate.locator"
    ));

    public static MapIdComponent lastMapId;

    private static void registerKeybindings() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (locatorKeybinding.wasPressed()) {
                client.setScreen(new LocatorScreen(client.player));
            }
        });
    }

    @Override
    public void onInitializeClient() {
        registerKeybindings();

        ClientPlayNetworking.registerGlobalReceiver(ResponseMapIdPayload.ID, (payload, context) -> lastMapId = payload.mapId());
    }
}