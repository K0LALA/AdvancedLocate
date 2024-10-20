package fr.kolala.advancedlocate.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface IChunkGeneratorCustomMethods {

    @Nullable
    Set<Pair<BlockPos, RegistryEntry<Structure>>> advancedLocate$locateStructure(ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int radius, int amount);

}