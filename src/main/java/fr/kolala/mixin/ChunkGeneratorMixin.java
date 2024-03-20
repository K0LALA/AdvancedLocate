package fr.kolala.mixin;

import com.mojang.datafixers.util.Pair;
import fr.kolala.util.IChunkGeneratorCustomMethods;
import fr.kolala.util.IChunkGeneratorInvoker;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
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
import net.minecraft.world.gen.chunk.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin implements IChunkGeneratorInvoker, IChunkGeneratorCustomMethods {
    @Override
    public Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateStructure(ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int radius, boolean skipReferencedStructures,
                                                                                       Set<Pair<BlockPos, RegistryEntry<Structure>>> structureSet) {
        StructurePlacementCalculator structurePlacementCalculator = world.getChunkManager().getStructurePlacementCalculator();
        Object2ObjectArrayMap<StructurePlacement, Set<RegistryEntry<Structure>>> map = new Object2ObjectArrayMap<>();
        for (RegistryEntry<Structure> registryEntry : structures) {
            for (StructurePlacement structurePlacement : structurePlacementCalculator.getPlacements(registryEntry)) {
                map.computeIfAbsent(structurePlacement, placement -> new ObjectArraySet<>()).add(registryEntry);
            }
        }
        if (map.isEmpty()) {
            return null;
        }
        Pair<BlockPos, RegistryEntry<Structure>> pair = null;
        double d = Double.MAX_VALUE;
        StructureAccessor structureAccessor = world.getStructureAccessor();
        ArrayList<Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>>> list = new ArrayList<>(map.size());
        for (Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry : map.entrySet()) {
            StructurePlacement structurePlacement2 = entry.getKey();
            if (structurePlacement2 instanceof ConcentricRingsStructurePlacement concentricRingsStructurePlacement) {
                BlockPos blockPos;
                double e;
                Pair<BlockPos, RegistryEntry<Structure>> pair2 = advancedLocate$locateConcentricRingsStructure(entry.getValue(), world, structureAccessor, center, skipReferencedStructures, concentricRingsStructurePlacement, structureSet);
                if (pair2 == null || !((e = center.getSquaredDistance(blockPos = pair2.getFirst())) < d)) continue;
                d = e;
                pair = pair2;
                continue;
            }
            if (!(structurePlacement2 instanceof RandomSpreadStructurePlacement)) continue;
            list.add(entry);
        }
        if (!list.isEmpty()) {
            int i = ChunkSectionPos.getSectionCoord(center.getX());
            int j = ChunkSectionPos.getSectionCoord(center.getZ());
            for (int k = 0; k <= radius; ++k) {
                boolean bl = false;
                for (Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry : list) {
                    RandomSpreadStructurePlacement randomSpreadStructurePlacement = (RandomSpreadStructurePlacement)entry.getKey();
                    Pair<BlockPos, RegistryEntry<Structure>> pair3 = advancedLocate$locateRandomSpreadStructure(entry.getValue(), world, structureAccessor, i, j, k, skipReferencedStructures, structurePlacementCalculator.getStructureSeed(), randomSpreadStructurePlacement, structureSet);
                    if (pair3 == null) continue;
                    bl = true;
                    double f = center.getSquaredDistance(pair3.getFirst());
                    if (!(f < d)) continue;
                    d = f;
                    pair = pair3;
                }
                if (!bl) continue;
                return pair;
            }
        }
        return pair;
    }

    @Override
    public Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateConcentricRingsStructure(Set<RegistryEntry<Structure>> structures, ServerWorld world, StructureAccessor structureAccessor, BlockPos center, boolean skipReferencedStructures, ConcentricRingsStructurePlacement placement,
                                                                                                  Set<Pair<BlockPos, RegistryEntry<Structure>>> structureSet) {
        List<ChunkPos> list = world.getChunkManager().getStructurePlacementCalculator().getPlacementPositions(placement);
        if (list == null) {
            throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
        }
        Pair<BlockPos, RegistryEntry<Structure>> pair = null;
        double d = Double.MAX_VALUE;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (ChunkPos chunkPos : list) {
            Pair<BlockPos, RegistryEntry<Structure>> pair2;
            mutable.set(ChunkSectionPos.getOffsetPos(chunkPos.x, 8), 32, ChunkSectionPos.getOffsetPos(chunkPos.z, 8));
            double e = mutable.getSquaredDistance(center);
            boolean bl = pair == null || e < d;
            if (!bl || (pair2 = advancedLocate$locateStructure(structures, world, structureAccessor, skipReferencedStructures, placement, chunkPos, structureSet)) == null) continue;
            pair = pair2;
            d = e;
        }
        return pair;
    }

    @Override
    public Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateRandomSpreadStructure(Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, int centerChunkX, int centerChunkZ, int radius, boolean skipReferencedStructures, long seed, RandomSpreadStructurePlacement placement,
                                                                                               Set<Pair<BlockPos, RegistryEntry<Structure>>> structureSet) {
        int i = placement.getSpacing();
        for (int j = -radius; j <= radius; j++) {
            boolean bl = j == -radius || j == radius;
            for (int k = -radius; k <= radius; k++) {
                int m;
                int l;
                ChunkPos chunkPos;
                Pair<BlockPos, RegistryEntry<Structure>> pair;
                boolean bl2;
                boolean bl3 = bl2 = k == -radius || k == radius;
                if (!bl && !bl2 || (pair = advancedLocate$locateStructure(structures, world, structureAccessor, skipReferencedStructures, placement, chunkPos = placement.getStartChunk(seed, l = centerChunkX + i * j, m = centerChunkZ + i * k), structureSet)) == null) continue;
                return pair;
            }
        }
        return null;
    }

    @Override
    public Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateStructure(Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, boolean skipReferencedStructures, StructurePlacement placement, ChunkPos pos,
                                                                                   Set<Pair<BlockPos, RegistryEntry<Structure>>> structureSet) {
        for (RegistryEntry<Structure> registryEntry : structures) {
            StructurePresence structurePresence = structureAccessor.getStructurePresence(pos, registryEntry.value(), skipReferencedStructures);
            if (structurePresence == StructurePresence.START_NOT_PRESENT) continue;
            if (!skipReferencedStructures && structurePresence == StructurePresence.START_PRESENT) {
                return Pair.of(placement.getLocatePos(pos), registryEntry);
            }
            Chunk chunk = world.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS);
            StructureStart structureStart = structureAccessor.getStructureStart(ChunkSectionPos.from(chunk), registryEntry.value(), chunk);
            Pair<BlockPos, RegistryEntry<Structure>> pair = Pair.of(placement.getLocatePos(structureStart.getPos()), registryEntry);
            if (structureStart == null || !structureStart.hasChildren() || skipReferencedStructures && !IChunkGeneratorInvoker.invokeCheckNotReferenced(structureAccessor, structureStart) /*structureSet.contains(pair)*/) continue;
            return pair;
        }
        return null;
    }

}