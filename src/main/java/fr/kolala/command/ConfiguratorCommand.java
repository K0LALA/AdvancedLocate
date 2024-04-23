package fr.kolala.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import fr.kolala.AdvancedLocate;
import fr.kolala.config.ConfigHelper;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ConfiguratorCommand {

    public static void register (CommandDispatcher<ServerCommandSource> dispatcher) {
        ConfigHelper.createConfigFileIfNotExisting();

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
        source.sendFeedback(() -> Text.literal(String.format("The value of %s %s.", field, ConfigHelper.getInt(field))), false);

        return 0;
    }

    private static int setIntValue(ServerCommandSource source, String field, int value) {
        JsonObject json = ConfigHelper.read();

        if (json == null) {
            source.sendFeedback(() -> Text.literal(String.format("Couldn't change the value of %s to %d.", field, value)), false);
            AdvancedLocate.LOGGER.error("Couldn't get json config.");
            return 1;
        }

        json.addProperty(field, value);
        source.sendFeedback(() -> Text.translatable(String.format("Successfully changed %s value to %d", field, value)), false);
        AdvancedLocate.LOGGER.info("Successfully changed config file.");
        
        return 0;
    }

}
