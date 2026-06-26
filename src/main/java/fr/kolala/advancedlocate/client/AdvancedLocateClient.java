package fr.kolala.advancedlocate.client;

import net.fabricmc.api.ClientModInitializer;

public class AdvancedLocateClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Keybindings.registerKeys();
    }
}