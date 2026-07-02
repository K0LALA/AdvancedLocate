package fr.kolala.advancedlocate.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.kolala.advancedlocate.AdvancedLocate;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class MapViewSavedData extends SavedData {
    private static final int MAP_SIZE = 128;
    private static final int HALF_MAP_SIZE = 64;
    public static final int MAX_SCALE = 4;

    public static final Codec<MapViewSavedData> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Codec.INT.fieldOf("xCenter").forGetter(m -> m.centerX),
                    Codec.INT.fieldOf("zCenter").forGetter(m -> m.centerZ),
                    Codec.BYTE.fieldOf("scale").forGetter(m -> m.scale),
                    Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(m -> m.dimension)
            ).apply(i, MapViewSavedData::new)
    );

    public final int centerX;
    public final int centerZ;
    public final byte scale;
    public final ResourceKey<Level> dimension;
    public byte[] colors = new byte[MAP_SIZE * MAP_SIZE];
    // TODO: Locations, represented through decorations

    private static final SavedDataType<MapViewSavedData> TYPE = new SavedDataType<MapViewSavedData>(
            Identifier.fromNamespaceAndPath(AdvancedLocate.MOD_ID, "saved_map_view"),
            MapViewSavedData::new,
            CODEC,
            null
    );

    public MapViewSavedData(final int centerX, final int centerZ, final byte scale, final ResourceKey<Level> dimension) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.scale = scale;
        this.dimension = dimension;
    }

    public MapViewSavedData() {
        this(0, 0, (byte) 1, ServerLevel.OVERWORLD);
    }

    public static MapViewSavedData getMapViewData(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);

        if (level == null) {
            return new MapViewSavedData();
        }

        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean updateColor(final int x, final int y, final byte newColor) {
        byte oldColor = this.colors[x + y * MAP_SIZE];
        if (oldColor != newColor) {
            this.setColor(x, y, newColor);
            return true;
        } else {
            return false;
        }
    }

    public void setColor(final int x, final int y, final byte newColor) {
        this.colors[x + y * MAP_SIZE] = newColor;
        this.setDirty();
    }

    public byte getColor(final int x, final int y) {
        return this.colors[x + y * MAP_SIZE];
    }

    public String toString() {
        return this.centerX + ";" + this.centerZ + "<<" + this.scale + "@" + this.dimension.toString();
    }
}