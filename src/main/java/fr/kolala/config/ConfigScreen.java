package fr.kolala.config;

import com.google.gson.JsonObject;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigScreen {
    private final Screen screen;

    private JsonObject content;

    public ConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("title.advancedlocate.config"));
        builder.setSavingRunnable(() -> ConfigHelper.write(content));

        ConfigCategory general = builder.getOrCreateCategory(Text.of(""));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        content = ConfigHelper.getDefaultJson();
        for (String entry : ConfigHelper.listFields()) {
            general.addEntry(entryBuilder.startIntField(Text.translatable("option.advancedlocate." + entry), ConfigHelper.getInt(entry))
                    .setDefaultValue(ConfigHelper.getDefaultValue(entry))
                    .setTooltip(Text.translatable("tooltip.advancedlocate." + "entry"))
                    .setSaveConsumer(newValue -> content.addProperty(entry, newValue))
                    .build());
        }

        screen = builder.build();
    }

    public Screen getScreen() {
        return this.screen;
    }
}