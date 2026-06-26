package fr.kolala.advancedlocate.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class LocatorScreen extends Screen {

    private final MapRenderState mapRenderState = new MapRenderState();

    protected LocatorScreen(Component title) {
        super(title);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (Keybindings.openLocatorKey.matches(event)) {
            this.minecraft.setScreen(null);
            this.minecraft.mouseHandler.grabMouse();
            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        assert this.minecraft.player != null;
        ItemStack map = this.minecraft.player.getInventory().getItem(0);
        if (!map.is(Items.FILLED_MAP)) {
            return;
        }
        MapId mapId = map.get(DataComponents.MAP_ID);
        MapItemSavedData mapData = null;
        if (mapId != null) {
            mapData = MapItem.getSavedData(mapId, this.minecraft.level);
        }
        this.extractMap(graphics, mapId, mapData);
        graphics.nextStratum();
    }

    private void extractMap(final GuiGraphicsExtractor graphics, final @Nullable MapId id, final @Nullable MapItemSavedData data) {
        if (id != null && data != null) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(100, 100);
            graphics.pose().scale(1, 1);
            this.minecraft.getMapRenderer().extractRenderState(id, data, this.mapRenderState);
            graphics.map(this.mapRenderState);
            graphics.pose().popMatrix();
        }
    }
}