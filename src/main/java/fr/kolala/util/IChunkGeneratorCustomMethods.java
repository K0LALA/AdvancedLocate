package fr.kolala.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;


public interface IChunkGeneratorCustomMethods {
    Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateStructuresInRadius(ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int radius, boolean skipReferencedStructures);

    Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateNearestStructures (ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int structureAmount, int radius, boolean skipReferencedStructures);
}
