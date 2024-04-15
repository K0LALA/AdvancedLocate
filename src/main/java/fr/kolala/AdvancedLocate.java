package fr.kolala;

import fr.kolala.command.AdvancedLocateCommand;
import fr.kolala.command.ConfiguratorCommand;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedLocate implements ModInitializer {
	public static final String MOD_ID = "advancedlocate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> AdvancedLocateCommand.register(dispatcher));
		CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> ConfiguratorCommand.register(dispatcher)));
	}


}