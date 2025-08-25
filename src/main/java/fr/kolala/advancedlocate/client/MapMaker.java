package fr.kolala.advancedlocate.client;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.map.MapState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public class MapMaker {

    private final World world;
    private final int x;
    private final int z;
    private final int width;
    private final int height;
    private final byte scale;

    // tmp
    public MapState state;
    public MapIdComponent id;

    public MapMaker(World world, int x, int z, int width, int height, byte scale) {
        this.world = world;
        this.x = x;
        this.z = z;
        this.width = width;
        this.height = height;
        this.scale = scale;

        // TODO: Calculate the scale needed depending on the width and the height, which determines how much it's zoomed in
        // TODO: Create necessary maps, maps' IDs
        MapIdComponent mapIdComponent = allocateMapId(world, x, z, scale);
        MapState mapState = world.getMapState(mapIdComponent);
        assert mapState != null;
        updateMapState(mapState, 0, 0);
        this.state = mapState;
        this.id = mapIdComponent;
    }

    private void updateMapState(MapState mapState, int X, int Z) {
        if (world.getRegistryKey() == mapState.dimension) {
            int i = 1 << mapState.scale;
            int j = mapState.centerX;
            int k = mapState.centerZ;
            int l = MathHelper.floor(X - (double)j) / i + 64;
            int m = MathHelper.floor(Z - (double)k) / i + 64;
            int n = 128 / i;
            if (world.getDimension().hasCeiling()) {
                n /= 2;
            }

            MapState.PlayerUpdateTracker playerUpdateTracker = mapState.getPlayerSyncData(world.getPlayers().getFirst());
            playerUpdateTracker.field_131++;
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            BlockPos.Mutable mutable2 = new BlockPos.Mutable();
            boolean bl = false;

            for (int o = l - n + 1; o < l + n; o++) {
                if ((o & 15) == (playerUpdateTracker.field_131 & 15) || bl) {
                    bl = false;
                    double d = 0.0;

                    for (int p = m - n - 1; p < m + n; p++) {
                        if (o >= 0 && p >= -1 && o < 128 && p < 128) {
                            int q = MathHelper.square(o - X) + MathHelper.square(p - m);
                            boolean bl2 = q > (n - 2) * (n - 2);
                            int r = (j / i + o - 64) * i;
                            int s = (k / i + p - 64) * i;
                            Multiset<MapColor> multiset = LinkedHashMultiset.create();
                            WorldChunk worldChunk = world.getChunk(ChunkSectionPos.getSectionCoord(r), ChunkSectionPos.getSectionCoord(s));
                            if (!worldChunk.isEmpty()) {
                                int t = 0;
                                double e = 0.0;
                                if (world.getDimension().hasCeiling()) {
                                    int u = r + s * 231871;
                                    u = u * u * 31287121 + u * 11;
                                    if ((u >> 20 & 1) == 0) {
                                        multiset.add(Blocks.DIRT.getDefaultState().getMapColor(world, BlockPos.ORIGIN), 10);
                                    } else {
                                        multiset.add(Blocks.STONE.getDefaultState().getMapColor(world, BlockPos.ORIGIN), 100);
                                    }

                                    e = 100.0;
                                } else {
                                    for (int u = 0; u < i; u++) {
                                        for (int v = 0; v < i; v++) {
                                            mutable.set(r + u, 0, s + v);
                                            int w = worldChunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, mutable.getX(), mutable.getZ()) + 1;
                                            BlockState blockState;
                                            if (w <= world.getBottomY() + 1) {
                                                blockState = Blocks.BEDROCK.getDefaultState();
                                            } else {
                                                do {
                                                    mutable.setY(--w);
                                                    blockState = worldChunk.getBlockState(mutable);
                                                } while (blockState.getMapColor(world, mutable) == MapColor.CLEAR && w > world.getBottomY());

                                                if (w > world.getBottomY() && !blockState.getFluidState().isEmpty()) {
                                                    int x = w - 1;
                                                    mutable2.set(mutable);

                                                    BlockState blockState2;
                                                    do {
                                                        mutable2.setY(x--);
                                                        blockState2 = worldChunk.getBlockState(mutable2);
                                                        t++;
                                                    } while (x > world.getBottomY() && !blockState2.getFluidState().isEmpty());

                                                    blockState = this.getFluidStateIfVisible(world, blockState, mutable);
                                                }
                                            }

                                            mapState.removeBanner(world, mutable.getX(), mutable.getZ());
                                            e += (double)w / (double)(i * i);
                                            multiset.add(blockState.getMapColor(world, mutable));
                                        }
                                    }
                                }

                                t /= i * i;
                                MapColor mapColor = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.CLEAR);
                                MapColor.Brightness brightness;
                                if (mapColor == MapColor.WATER_BLUE) {
                                    double f = (double)t * 0.1 + (double)(o + p & 1) * 0.2;
                                    if (f < 0.5) {
                                        brightness = MapColor.Brightness.HIGH;
                                    } else if (f > 0.9) {
                                        brightness = MapColor.Brightness.LOW;
                                    } else {
                                        brightness = MapColor.Brightness.NORMAL;
                                    }
                                } else {
                                    double f = (e - d) * 4.0 / (double)(i + 4) + ((double)(o + p & 1) - 0.5) * 0.4;
                                    if (f > 0.6) {
                                        brightness = MapColor.Brightness.HIGH;
                                    } else if (f < -0.6) {
                                        brightness = MapColor.Brightness.LOW;
                                    } else {
                                        brightness = MapColor.Brightness.NORMAL;
                                    }
                                }

                                d = e;
                                if (p >= 0 && q < n * n && (!bl2 || (o + p & 1) != 0)) {
                                    bl |= mapState.putColor(o, p, mapColor.getRenderColorByte(brightness));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private BlockState getFluidStateIfVisible(World world, BlockState state, BlockPos pos) {
        FluidState fluidState = state.getFluidState();
        return !fluidState.isEmpty() && !state.isSideSolidFullSquare(world, pos, Direction.UP) ? fluidState.getBlockState() : state;
    }

    private MapIdComponent allocateMapId(World world, int x, int z, byte scale) {
        MapState mapState = MapState.of((double) x, (double) z, (byte) scale, true, false, world.getRegistryKey());
        MapIdComponent mapIdComponent = world.increaseAndGetMapId();
        world.putMapState(mapIdComponent, mapState);
        return mapIdComponent;
    }
}