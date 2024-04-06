package fr.kolala;

import fr.kolala.command.AdvancedLocateCommand;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class AdvancedLocate implements ModInitializer {
	public static final String MOD_ID = "advancedlocate";

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, registryAccess2) -> AdvancedLocateCommand.register(dispatcher));
	}
}