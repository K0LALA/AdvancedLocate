package fr.kolala.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public interface IChunkGeneratorCustomMethods {
    @Nullable
    Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateStructure(Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, boolean skipReferencedStructures, StructurePlacement placement, ChunkPos pos, Pair<Boolean, UUID> checkIfCommandReferenced);

    boolean advancedLocate$checkCommandReferenced(StructureAccessor structureAccessor, StructureStart start, UUID uuid);
}
