package fr.kolala.advancedlocate.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import fr.kolala.advancedlocate.AdvancedLocate;
import fr.kolala.advancedlocate.config.ConfigHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ConfiguratorCommand {

    public static void register (CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String field : ConfigHelper.listFields()) {
            dispatcher.register(Commands.literal("advancedlocate").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.literal("config")
                    .then(Commands.literal("get")
                            .then(Commands.literal(field).executes(context -> getIntValue(context.getSource(), field))))
                    .then(Commands.literal("set")
                            .then(Commands.literal(field).then(Commands.argument("value", IntegerArgumentType.integer())
                                    .executes(context -> setIntValue(context.getSource(), field, IntegerArgumentType.getInteger(context, "value"))))))));
        }
    }

    private static int getIntValue(CommandSourceStack source, String field) {
        source.sendSuccess(() -> Component.translatable("command.advancedlocate.config.get", field, String.valueOf(ConfigHelper.getIntOrDefault(field))), false);

        return 1;
    }

    private static int setIntValue(CommandSourceStack source, String field, int value) {
        JsonObject json = ConfigHelper.read();

        if (json == null) {
            source.sendSuccess(() -> Component.translatable("command.advancedlocate.config.fail", field, value), false);
            AdvancedLocate.LOGGER.error("Couldn't get json config.");
            return 0;
        }

        json.addProperty(field, value);
        if (ConfigHelper.write(json)) {
            source.sendSuccess(() -> Component.translatable("command.advancedlocate.config.success", field, value), false);
            AdvancedLocate.LOGGER.info("Successfully changed config file.");

            // Reload structure commands
            AdvancedLocate.registerCommands();

            return 1;
        }
        else {
            source.sendSuccess(() -> Component.translatable("command.advancedlocate.config.fail", field, value), false);
            AdvancedLocate.LOGGER.info("Couldn't change config.");

            return 0;
        }
    }

}