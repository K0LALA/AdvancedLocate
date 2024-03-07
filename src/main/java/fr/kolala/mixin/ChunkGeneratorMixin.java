package fr.kolala.mixin;

import com.mojang.datafixers.util.Pair;
import fr.kolala.util.IChunkGeneratorCustomMethods;
import fr.kolala.util.IChunkGeneratorInvoker;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.StructurePresence;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Set;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin implements IChunkGeneratorInvoker, IChunkGeneratorCustomMethods {

    @Override
    public Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateStructure(Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, boolean skipReferencedStructures, StructurePlacement placement, ChunkPos pos, Set<Pair<BlockPos, RegistryEntry<Structure>>> alreadyFoundStructures) {
        for (RegistryEntry<Structure> registryEntry : structures) {
            StructurePresence structurePresence = structureAccessor.getStructurePresence(pos, registryEntry.value(), skipReferencedStructures);
            if (structurePresence == StructurePresence.START_NOT_PRESENT) continue;
            if (!skipReferencedStructures && structurePresence == StructurePresence.START_PRESENT) {
                return Pair.of(placement.getLocatePos(pos), registryEntry);
            }
            Chunk chunk = world.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS);
            StructureStart structureStart = structureAccessor.getStructureStart(ChunkSectionPos.from(chunk), registryEntry.value(), chunk);

            if (structureStart == null ||
                !structureStart.hasChildren()) continue;
            Pair<BlockPos, RegistryEntry<Structure>> pair = Pair.of(placement.getLocatePos(structureStart.getPos()), registryEntry);
            if ((skipReferencedStructures && !IChunkGeneratorInvoker.invokeCheckNotReferenced(structureAccessor, structureStart))
                || (alreadyFoundStructures != null && !alreadyFoundStructures.contains(pair))) continue;
            return pair;
        }
        return null;
    }

    @Unique
    private Pair<BlockPos, RegistryEntry<Structure>> locateStructure(Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, boolean skipReferencedStructures, StructurePlacement placement, ChunkPos pos) {
        return advancedLocate$locateStructure(structures, world, structureAccessor, skipReferencedStructures, placement, pos, null);
    }
}