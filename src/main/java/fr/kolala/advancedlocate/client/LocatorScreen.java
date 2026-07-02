package fr.kolala.advancedlocate.client;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.mojang.blaze3d.platform.NativeImage;
import fr.kolala.advancedlocate.AdvancedLocate;
import fr.kolala.advancedlocate.util.MapViewSavedData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;

@Environment(EnvType.CLIENT)
public class LocatorScreen extends Screen {

    public static final byte SCALE = 1;

    public static final int OFFSET_X = 150;
    public static final int OFFSET_Y = 100;
    public static final int WIDTH = 128;
    public static final int HEIGHT = 128;

    private MapViewSavedData data;
    private Identifier location;

    protected LocatorScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        assert this.minecraft.player != null;
        int scale = 1 << SCALE;
        int centerX = (int) this.minecraft.player.getX();
        int centerZ = (int) this.minecraft.player.getZ();
        assert this.minecraft.level != null;
        ClientLevel level = this.minecraft.level;

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
        this.data = new MapViewSavedData(centerX, centerZ, (byte) 1, ClientLevel.OVERWORLD);

        for (int x = 0; x < WIDTH; x++) {
            double previousAverageAreaHeight = 0.0;

            for (int y = 0; y < HEIGHT; y++) {
                int averagingAreaMinX = (centerX / scale + x - 64) * scale;
                int averagingAreaMinZ = (centerZ / scale + y - 64) * scale;
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
                        for (int averagingDeltaX = 0; averagingDeltaX < scale; averagingDeltaX++) {
                            for (int averagingDeltaZ = 0; averagingDeltaZ < scale; averagingDeltaZ++) {
                                blockPos.set(averagingAreaMinX + averagingDeltaX, 0, averagingAreaMinZ + averagingDeltaZ);
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

                                averageAreaHeight += (double)columnY / (scale * scale);
                                colorCount.add(state.getMapColor(level, blockPos));
                            }
                        }
                    }

                    waterDepth /= scale * scale;
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
                        double diff = (averageAreaHeight - previousAverageAreaHeight) * 4.0 / (scale + 4) + ((x + y & 1) - 0.5) * 0.4;
                        if (diff > 0.6) {
                            brightness = MapColor.Brightness.HIGH;
                        } else if (diff < -0.6) {
                            brightness = MapColor.Brightness.LOW;
                        } else {
                            brightness = MapColor.Brightness.NORMAL;
                        }
                    }

                    previousAverageAreaHeight = averageAreaHeight;
                    // TODO: Use value from method call (see foundConsecutiveChanges@MapItem#update)
                    this.data.updateColor(x, y, color.getPackedId(brightness));
                }
            }
        }

        this.location = createMapTexture(this.data);
    }

    private Identifier createMapTexture(MapViewSavedData mapData) {
        Identifier location = Identifier.fromNamespaceAndPath(AdvancedLocate.MOD_ID, "mapview/0");
        DynamicTexture texture = new DynamicTexture("MapView0", 128, 128, true);
        this.minecraft.getTextureManager().register(location, texture);

        NativeImage pixels = texture.getPixels();
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                pixels.setPixel(x, y, MapColor.getColorFromPackedId(mapData.getColor(x, y)));
            }
        }

        texture.upload();

        return location;
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

        graphics.blit(RenderPipelines.GUI_TEXTURED, this.location, OFFSET_X, OFFSET_Y, 0.0F, 0.0F, 128, 128, 128, 128);
    }

    private BlockState getCorrectStateForFluidBlock(final ClientLevel level, final BlockState state, final BlockPos pos) {
        FluidState fluidState = state.getFluidState();
        return !fluidState.isEmpty() && !state.isFaceSturdy(level, pos, Direction.UP) ? fluidState.createLegacyBlock() : state;
    }
}