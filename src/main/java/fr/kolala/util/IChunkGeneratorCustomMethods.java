package fr.kolala.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface IChunkGeneratorCustomMethods {

    @Nullable
    Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateStructure(ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int radius, boolean skipReferencedStructures,
                                                                            Set<Pair<BlockPos, RegistryEntry<Structure>>> structureSet);

    @Nullable
    Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateConcentricRingsStructure(Set<RegistryEntry<Structure>> structures, ServerWorld world, StructureAccessor structureAccessor, BlockPos center, boolean skipReferencedStructures, ConcentricRingsStructurePlacement placement,
                                                                                           Set<Pair<BlockPos, RegistryEntry<Structure>>> structureSet);

    @Nullable
    Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateRandomSpreadStructure(Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, int centerChunkX, int centerChunkZ, int radius, boolean skipReferencedStructures, long seed, RandomSpreadStructurePlacement placement,
                                                                                        Set<Pair<BlockPos, RegistryEntry<Structure>>> structureSet);

    @Nullable
    Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateStructure(Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, boolean skipReferencedStructures, StructurePlacement placement, ChunkPos pos,
                                                                            Set<Pair<BlockPos, RegistryEntry<Structure>>> structureSet);

}
