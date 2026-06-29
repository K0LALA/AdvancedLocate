package fr.kolala.advancedlocate.client;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class LocatorScreen extends Screen {

    public static final int OFFSET_X = 50;
    public static final int OFFSET_Y = 100;
    public static final int WIDTH = 128;
    public static final int HEIGHT = 128;

    private MapId id;
    private MapItemSavedData data;


    private final MapRenderState mapRenderState = new MapRenderState();

    protected LocatorScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        assert this.minecraft.player != null;
        int centerX = (int) this.minecraft.player.getX();
        int centerZ = (int) this.minecraft.player.getZ();
        assert this.minecraft.level != null;
        ClientLevel level = this.minecraft.level;

        this.id = new MapId(0);

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
        this.data = MapItemSavedData.createFresh(centerX, centerZ, (byte) 1, true, true, level.dimension());

        for (int x = 0; x < WIDTH; x++) {
            double previousAverageAreaHeight = 0.0;

            for (int y = 0; y < HEIGHT; y++) {
                int averagingAreaMinX = (centerX + x - 64);
                int averagingAreaMinZ = (centerZ + y - 64);
                Multiset<MapColor> colorCount = LinkedHashMultiset.create();
                LevelChunk chunk = level.getChunk(SectionPos.blockToSectionCoord(averagingAreaMinX), SectionPos.blockToSectionCoord(averagingAreaMinZ));
                if (!chunk.isEmpty()) {
                    int waterDepth = 0;
                    double averageAreaHeight = 0.0;
                    if (level.dimensionType().hasCeiling()) {
                        int ceilingNoise = averagingAreaMinX + averagingAreaMinZ * 231871;
                        ceilingNoise = ceilingNoise * ceilingNoise * 31287121 + ceilingNoise * 11;
                        if ((ceilingNoise >> 20 & 1) == 0) {
                            colorCount.add(Blocks.DIRT.defaultBlockState().getMapColor(level, BlockPos.ZERO), 10);
                        } else {
                            colorCount.add(Blocks.STONE.defaultBlockState().getMapColor(level, BlockPos.ZERO), 100);
                        }

                        averageAreaHeight = 100.0;
                    } else {
                        blockPos.set(averagingAreaMinX, 0, averagingAreaMinZ);
                        int columnY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, blockPos.getX(), blockPos.getZ()) + 1;
                        BlockState state;
                        if (columnY <= level.getMinY()) {
                            state = Blocks.BEDROCK.defaultBlockState();
                        } else {
                            do {
                                blockPos.setY(--columnY);
                                state = chunk.getBlockState(blockPos);
                            } while (state.getMapColor(level, blockPos) == MapColor.NONE && columnY > level.getMinY());

                            if (columnY > level.getMinY() && !state.getFluidState().isEmpty()) {
                                int solidY = columnY - 1;
                                belowPos.set(blockPos);

                                BlockState belowBlock;
                                do {
                                    belowPos.setY(--solidY);
                                    belowBlock = chunk.getBlockState(belowPos);
                                    waterDepth++;
                                } while (solidY > level.getMinY() && !belowBlock.getFluidState().isEmpty());

                                state = this.getCorrectStateForFluidBlock(level, state, blockPos);
                            }
                        }

                        colorCount.add(state.getMapColor(level, blockPos));
                    }

                    MapColor color = Iterables.getFirst(Multisets.copyHighestCountFirst(colorCount), MapColor.NONE);
                    MapColor.Brightness brightness;
                    if (color == MapColor.WATER) {
                        double diff = waterDepth * 0.1 + (x + y & 1) * 0.2;
                        if (diff < 0.5) {
                            brightness = MapColor.Brightness.HIGH;
                        } else if (diff > 0.9) {
                            brightness = MapColor.Brightness.LOW;
                        } else {
                            brightness = MapColor.Brightness.NORMAL;
                        }
                    } else {
                        double diff = (averageAreaHeight - previousAverageAreaHeight) * 4.0 / 5 + ((x + y & 1) - 0.5) * 0.4;
                        if (diff > 0.6) {
                            brightness = MapColor.Brightness.HIGH;
                        } else if (diff < -0.6) {
                            brightness = MapColor.Brightness.LOW;
                        } else {
                            brightness = MapColor.Brightness.NORMAL;
                        }
                    }

                    previousAverageAreaHeight = averageAreaHeight;
                    this.data.updateColor(x, y, color.getPackedId(brightness));
                }
            }
        }
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

        this.extractMap(graphics, this.id, this.data);
    }

    private BlockState getCorrectStateForFluidBlock(final ClientLevel level, final BlockState state, final BlockPos pos) {
        FluidState fluidState = state.getFluidState();
        return !fluidState.isEmpty() && !state.isFaceSturdy(level, pos, Direction.UP) ? fluidState.createLegacyBlock() : state;
    }

    private void extractMap(final GuiGraphicsExtractor graphics, final @Nullable MapId id, final @Nullable MapItemSavedData data) {
        if (id != null && data != null) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(OFFSET_X, OFFSET_Y);
            graphics.pose().scale(1, 1);
            this.minecraft.getMapRenderer().extractRenderState(id, data, this.mapRenderState);
            graphics.map(this.mapRenderState);
            graphics.pose().popMatrix();
        }
    }
}