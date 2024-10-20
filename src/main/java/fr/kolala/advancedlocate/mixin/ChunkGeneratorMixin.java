package fr.kolala.advancedlocate.mixin;

import com.mojang.datafixers.util.Pair;
import fr.kolala.advancedlocate.util.IChunkGeneratorCustomMethods;
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
import org.spongepowered.asm.mixin.Unique;

import java.util.*;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin implements IChunkGeneratorCustomMethods {
    @Override
    public Set<Pair<BlockPos, RegistryEntry<Structure>>> advancedLocate$locateStructure(ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int radius, int amount) {
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
        double minDistanceFound = Double.MAX_VALUE;
        StructureAccessor structureAccessor = world.getStructureAccessor();
        ArrayList<Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>>> list = new ArrayList<>(map.size());
        Set<Pair<BlockPos, RegistryEntry<Structure>>> pairSet1 = new HashSet<>();
        for (Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry : map.entrySet()) {
            StructurePlacement structurePlacement2 = entry.getKey();
            /*if (structurePlacement2 instanceof ConcentricRingsStructurePlacement concentricRingsStructurePlacement) {
                double e;
                pairSet1.addAll(advancedLocate$locateConcentricRingsStructure(entry.getValue(), world, structureAccessor, center, concentricRingsStructurePlacement, pairSet1, amount));
                if (pair2 == null || !((e = center.getSquaredDistance(pair2.getFirst())) < d)) continue;
                d = e;
                //pair = pair2;
                continue;
            }*/
            if (!(structurePlacement2 instanceof RandomSpreadStructurePlacement)) continue;
            list.add(entry);
        }
        if (!list.isEmpty()) {
            int i = ChunkSectionPos.getSectionCoord(center.getX());
            int j = ChunkSectionPos.getSectionCoord(center.getZ());
            Set<Pair<BlockPos, RegistryEntry<Structure>>> pairSet2 = new HashSet<>();
            for (int k = 0; k <= radius; ++k) {
                for (Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry : list) {
                    RandomSpreadStructurePlacement randomSpreadStructurePlacement = (RandomSpreadStructurePlacement)entry.getKey();
                    Set<Pair<BlockPos, RegistryEntry<Structure>>> foundPairs = advancedLocate$locateRandomSpreadStructure(entry.getValue(), world, structureAccessor, i, j, k, structurePlacementCalculator.getStructureSeed(), randomSpreadStructurePlacement, pairSet2, amount);
                    if (foundPairs == null || foundPairs.isEmpty()) {
                        continue;
                    }
                    pairSet2.addAll(foundPairs);
                    for (var pair_ : pairSet2) {
                        if (pair_ == null) continue;
                        double f = center.getSquaredDistance(pair_.getFirst());
                        // Change Double.MAX_VALUE to the furthest structure found, if the current structure is closer, remove the further one, so we only have the closest one.
                        // Will improve the accuracy of the locator
                        if(!(f < Double.MAX_VALUE)) pairSet2.remove(pair_);
                        minDistanceFound = f;
                    }
                }
                if (pairSet2.size() >= amount) return pairSet2;
            }
        }
        return null;
    }

    @Unique
    public Set<Pair<BlockPos, RegistryEntry<Structure>>> advancedLocate$locateConcentricRingsStructure(Set<RegistryEntry<Structure>> structures, ServerWorld world, StructureAccessor structureAccessor, BlockPos center, ConcentricRingsStructurePlacement placement,
                                                                                                  Set<Pair<BlockPos, RegistryEntry<Structure>>> structureSet, int amount) {
        Set<Pair<BlockPos, RegistryEntry<Structure>>> pairSet = new HashSet<>();
        List<ChunkPos> list = world.getChunkManager().getStructurePlacementCalculator().getPlacementPositions(placement);
        if (list == null) {
            throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
        }
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (ChunkPos chunkPos : list) {
            Pair<BlockPos, RegistryEntry<Structure>> pair2;
            mutable.set(ChunkSectionPos.getOffsetPos(chunkPos.x, 8), 32, ChunkSectionPos.getOffsetPos(chunkPos.z, 8));
            if ((pair2 = advancedLocate$locateStructure(structures, world, structureAccessor, placement, chunkPos, structureSet)) == null) continue;
            pairSet.add(pair2);
            if (pairSet.size() + structureSet.size() >= amount) return pairSet;
        }
        return pairSet.isEmpty() ? null : pairSet;
    }

    @Unique
    public Set<Pair<BlockPos, RegistryEntry<Structure>>> advancedLocate$locateRandomSpreadStructure(Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, int centerChunkX, int centerChunkZ, int radius, long seed, RandomSpreadStructurePlacement placement,
                                                                                               Set<Pair<BlockPos, RegistryEntry<Structure>>> structureSet, int amount) {
        Set<Pair<BlockPos, RegistryEntry<Structure>>> pairSet = new HashSet<>();
        int i = placement.getSpacing();
        for (int j = -radius; j <= radius; j++) {
            boolean bl = j == -radius || j == radius;
            for (int k = -radius; k <= radius; k++) {
                Pair<BlockPos, RegistryEntry<Structure>> pair;
                boolean bl2 = k == -radius || k == radius;
                if (bl || bl2) {
                    pair = advancedLocate$locateStructure(structures, world, structureAccessor, placement, placement.getStartChunk(seed, centerChunkX + i * j, centerChunkZ + i * k), structureSet);
                    if (pair != null) {
                        pairSet.add(pair);
                        if (pairSet.size() + structureSet.size() >= amount) return pairSet;
                    }
                }
            }
        }
        return pairSet.isEmpty() ? null : pairSet;
    }

    @Unique
    public Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateStructure(Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, StructurePlacement placement, ChunkPos pos,
                                                                                   Set<Pair<BlockPos, RegistryEntry<Structure>>> structureSet) {
        for (RegistryEntry<Structure> registryEntry : structures) {
            StructurePresence structurePresence = structureAccessor.getStructurePresence(pos, registryEntry.value(), placement, false);
            if (structurePresence == StructurePresence.START_NOT_PRESENT) continue;
            if (structurePresence == StructurePresence.START_PRESENT && !structureSet.contains(Pair.of(placement.getLocatePos(pos), registryEntry))) {
                return Pair.of(placement.getLocatePos(pos), registryEntry);
            }
            Chunk chunk = world.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS);
            StructureStart structureStart = structureAccessor.getStructureStart(ChunkSectionPos.from(chunk), registryEntry.value(), chunk);
            Pair<BlockPos, RegistryEntry<Structure>> pair;
            if (structureStart == null || !structureStart.hasChildren() || structureSet.contains(pair = Pair.of(placement.getLocatePos(structureStart.getPos()), registryEntry))) continue;
            return pair;
        }
        return null;
    }

}