package fr.kolala.advancedlocate.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import fr.kolala.advancedlocate.AdvancedLocate;
import fr.kolala.advancedlocate.config.ConfigHelper;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ConfiguratorCommand {

    public static void register (CommandDispatcher<ServerCommandSource> dispatcher) {
        for (String field : ConfigHelper.listFields()) {
            dispatcher.register(CommandManager.literal("advancedlocate").requires(source -> source.hasPermissionLevel(2)).then(CommandManager.literal("config")
                    .then(CommandManager.literal("get")
                            .then(CommandManager.literal(field).executes(context -> getIntValue(context.getSource(), field))))
                    .then(CommandManager.literal("set")
                            .then(CommandManager.literal(field).then(CommandManager.argument("value", IntegerArgumentType.integer())
                                    .executes(context -> setIntValue(context.getSource(), field, IntegerArgumentType.getInteger(context, "value"))))))));
        }
    }

    private static int getIntValue(ServerCommandSource source, String field) {
        source.sendFeedback(() -> Text.translatable("command.advancedlocate.config.get", field, ConfigHelper.getIntOrDefault(field)), false);

        return 1;
    }

    private static int setIntValue(ServerCommandSource source, String field, int value) {
        JsonObject json = ConfigHelper.read();

        if (json == null) {
            source.sendFeedback(() -> Text.translatable("command.advancedlocate.config.fail", field, value), false);
            AdvancedLocate.LOGGER.error("Couldn't get json config.");
            return 0;
        }

        json.addProperty(field, value);
        if (ConfigHelper.write(json)) {
            source.sendFeedback(() -> Text.translatable("command.advancedlocate.config.success", field, value), false);
            AdvancedLocate.LOGGER.info("Successfully changed config file.");

            // Reload structure commands
            AdvancedLocate.registerCommands();

            return 1;
        }
        else {
            source.sendFeedback(() -> Text.translatable("command.advancedlocate.config.fail", field, value), false);
            AdvancedLocate.LOGGER.info("Couldn't change config.");

            return 0;
        }
    }

}