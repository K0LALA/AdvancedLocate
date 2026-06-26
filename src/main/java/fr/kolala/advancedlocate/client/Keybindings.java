package fr.kolala.advancedlocate.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.kolala.advancedlocate.AdvancedLocate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class Keybindings {

    public static KeyMapping.Category ADVANCED_LOCATE_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(AdvancedLocate.MOD_ID, "advanced_locate_key_mappings")
    );

    public static KeyMapping openLocatorKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.advancedlocate.open_locator_screen",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_N,
                    ADVANCED_LOCATE_CATEGORY
            )
    );

    public static void registerKeys() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openLocatorKey.consumeClick()) {
                if (client.player != null) {
                    client.setScreen(new LocatorScreen(Component.literal("Locator")));
                }
            }
        });
    }
}