package fr.kolala;

import fr.kolala.command.AdvancedLocateCommand;
import fr.kolala.command.ConfiguratorCommand;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

@Environment(value = EnvType.SERVER)
public class AdvancedLocateServer implements DedicatedServerModInitializer {


    @Override
    public void onInitializeServer() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> AdvancedLocateCommand.register(dispatcher));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ConfiguratorCommand.register(dispatcher));
    }
}
