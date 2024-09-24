package fr.kolala.advancedlocate.client.screen;

import fr.kolala.advancedlocate.AdvancedLocate;
import fr.kolala.advancedlocate.client.AdvancedLocateClient;
import fr.kolala.advancedlocate.client.widget.LegacyTexturedButtonWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class LocatorScreen extends Screen {

    private static final Identifier FILTER_BUTTON_TEXTURE = Identifier.of(AdvancedLocate.MOD_ID, "textures/gui/filter_button.png");

    public static int baseX = 0;
    public static int baseY = 0;

    private final PlayerEntity player;

    public LocatorScreen(PlayerEntity player) {
        super(Text.literal("Locator Screen"));
        this.player = player;
    }

    private TextFieldWidget searchField;
    private ClickableWidget filterButton;

    @Override
    protected void init() {

        searchField = new TextFieldWidget(textRenderer, baseX + 10, baseY + 10, this.width - 50, 20, Text.of("Search..."));
        filterButton = LegacyTexturedButtonWidget.legacyTexturedBuilder(Text.of(""), button -> {
                    // TODO: Define what to do on button press
                })
                .position(baseX + width - 30, baseY + 10)
                .size(20, 20)
                .uv(0, 0, 20)
                .texture(FILTER_BUTTON_TEXTURE, 20, 60)
                .build();

        addDrawableChild(searchField);
        addDrawableChild(filterButton);

        // Draw the map from MapItem class, or create another function to use biome color instead of blocks
        // Which would probably be less costly because one block doesn't depend on the others to determine the brightness

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int playerX = player.getBlockX();
        int playerZ = player.getBlockZ();
        byte scale = 1;
        //context.drawCenteredTextWithShadow(textRenderer, Text.literal(String.format("Player position: X%d;Z%d", player.getBlockX(), player.getBlockZ())), width / 2, height / 2, 0xffffff);
        drawMap(context, playerX, playerZ, scale);
    }

    private void drawMap(DrawContext context, int xPos, int zPos, byte scale) {
        assert client != null;

        MatrixStack matrices = context.getMatrices();

        matrices.push();
        float offsetX = 50.0F;
        float offsetY = 50.0F;
        matrices.translate(offsetX, offsetY, 1.0F);
        matrices.scale(scale, scale, -1);
        MapIdComponent mapId = player.getInventory().getStack(0).get(DataComponentTypes.MAP_ID);

        // To find id and state of map, it's needed to create a real map so we can access from it
        client.gameRenderer.getMapRenderer()
                .draw(
                        matrices,
                        context.getVertexConsumers(),
                        mapId,
                        FilledMapItem.getMapState(mapId, client.world),
                        true,
                        0xF000F0
                );

        context.draw();
        matrices.pop();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (AdvancedLocateClient.locatorKeybinding.matchesKey(keyCode, scanCode)) {
            assert this.client != null;
            this.client.setScreen(null);
            this.client.mouse.lockCursor();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}