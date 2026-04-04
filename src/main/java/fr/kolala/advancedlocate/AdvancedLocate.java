package fr.kolala.advancedlocate;

import fr.kolala.advancedlocate.command.AdvancedLocateCommand;
import fr.kolala.advancedlocate.command.ConfiguratorCommand;
import fr.kolala.advancedlocate.command.DistanceArgumentType;
import fr.kolala.advancedlocate.config.ConfigHelper;
import fr.kolala.advancedlocate.network.packet.RequestMapIdPayload;
import fr.kolala.advancedlocate.network.packet.ResponseMapIdPayload;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedLocate implements ModInitializer {
	public static final String MOD_ID = "advancedlocate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Registering custom argument type.");
		ArgumentTypeRegistry.registerArgumentType(Identifier.of(MOD_ID, "distance"),
				DistanceArgumentType.class,
				ConstantArgumentSerializer.of(DistanceArgumentType::distanceArgumentType));
		LOGGER.info("Checking config file integrity.");
		if (!ConfigHelper.checkConfigFileIntegrity())
			LOGGER.error("Config file is not valid! The mod may not work when trying to execute a command, be aware!");
		registerCommands();

		PayloadTypeRegistry.playC2S().register(RequestMapIdPayload.ID, RequestMapIdPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ResponseMapIdPayload.ID, ResponseMapIdPayload.CODEC);
	}

	public static void registerCommands() {

		LOGGER.info("Registering commands.");
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> AdvancedLocateCommand.register(dispatcher));
		CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> ConfiguratorCommand.register(dispatcher)));
	}
}