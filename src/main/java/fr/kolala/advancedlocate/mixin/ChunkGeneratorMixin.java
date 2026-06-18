package fr.kolala.advancedlocate.mixin;

import com.mojang.datafixers.util.Pair;
import fr.kolala.advancedlocate.util.IChunkGeneratorCustomMethods;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin implements IChunkGeneratorCustomMethods {
    @Override
    public List<Pair<BlockPos, Holder<Structure>>> advancedLocate$locateStructure(ServerLevel world, HolderSet<Structure> structures, BlockPos center, int radius, int amount) {
        ChunkGeneratorStructureState structurePlacementCalculator = world.getChunkSource().getGeneratorState();
        Object2ObjectArrayMap<StructurePlacement, Set<Holder<Structure>>> map = new Object2ObjectArrayMap<>();
        for (Holder<Structure> registryEntry : structures) {
            for (StructurePlacement structurePlacement : structurePlacementCalculator.getPlacementsForStructure(registryEntry)) {
                map.computeIfAbsent(structurePlacement, _ -> new ObjectArraySet<>()).add(registryEntry);
            }
        }
        if (map.isEmpty()) {
            return null;
        }
        StructureManager structureAccessor = world.structureManager();
        ArrayList<Map.Entry<StructurePlacement, Set<Holder<Structure>>>> list = new ArrayList<>(map.size());
        List<Pair<BlockPos, Holder<Structure>>> structureList = new ArrayList<>();
        for (Map.Entry<StructurePlacement, Set<Holder<Structure>>> entry : map.entrySet()) {
            StructurePlacement structurePlacement2 = entry.getKey();
            if (structurePlacement2 instanceof ConcentricRingsStructurePlacement concentricRingsStructurePlacement) {
                List<Pair<BlockPos, Holder<Structure>>> foundPairs = advancedLocate$locateConcentricRingsStructure(entry.getValue(), world, structureAccessor, concentricRingsStructurePlacement);
                if (foundPairs == null || foundPairs.isEmpty()) {
                    continue;
                }
                structureList.addAll(foundPairs);
                sortStructureList(structureList, center);
                structureList = shrinkStructureList(structureList, amount);
                return structureList;
            }
            if (!(structurePlacement2 instanceof RandomSpreadStructurePlacement)) continue;
            list.add(entry);
        }
        if (!list.isEmpty()) {
            int i = SectionPos.blockToSectionCoord(center.getX());
            int j = SectionPos.blockToSectionCoord(center.getZ());
            for (int k = 0; k <= radius; ++k) {
                for (Map.Entry<StructurePlacement, Set<Holder<Structure>>> entry : list) {
                    RandomSpreadStructurePlacement randomSpreadStructurePlacement = (RandomSpreadStructurePlacement)entry.getKey();
                    List<Pair<BlockPos, Holder<Structure>>> foundPairs = advancedLocate$locateRandomSpreadStructure(entry.getValue(), world, structureAccessor, i, j, k, structurePlacementCalculator.getLevelSeed(), randomSpreadStructurePlacement, structureList, amount);
                    if (foundPairs == null || foundPairs.isEmpty()) {
                        continue;
                    }
                    structureList.addAll(foundPairs);
                    sortStructureList(structureList, center);
                    structureList = shrinkStructureList(structureList, amount);
                    if (structureList.size() >= amount) {
                        if (list.size() > 1) continue;
                        return structureList;
                    }
                }
            }
            return structureList;
        }
        return null;
    }

    /**
     * Sorts the structure list by distances to the player in ascending order
     * @param structureList The list of structures to be sorted
     * @param center The position of the player to calculate the distance
     */
    @Unique
    private void sortStructureList(List<Pair<BlockPos, Holder<Structure>>> structureList, BlockPos center) {
        structureList.sort((o1, o2) -> (int) (o1.getFirst().distSqr(center) - o2.getFirst().distSqr(center)));
    }

    /**
     * Shrinks the structure list to be only of a maximum amount
     * @param structureList The list of structures to shrink
     * @param amount The maximum amount of elements in the list
     * @return The list containing the amount (or less) of elements wanted
     */
    @Unique
    List<Pair<BlockPos, Holder<Structure>>> shrinkStructureList(List<Pair<BlockPos, Holder<Structure>>> structureList, int amount) {
        return structureList.subList(0, Math.min(structureList.size(), amount));
    }

    @Unique
    public List<Pair<BlockPos, Holder<Structure>>> advancedLocate$locateConcentricRingsStructure(Set<Holder<Structure>> structures, ServerLevel world, StructureManager structureAccessor, ConcentricRingsStructurePlacement placement) {
        List<Pair<BlockPos, Holder<Structure>>> foundStructuresList = new ArrayList<>();
        List<ChunkPos> list = world.getChunkSource().getGeneratorState().getRingPositionsFor(placement);
        if (list == null) {
            throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
        }
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (ChunkPos chunkPos : list) {
            Pair<BlockPos, Holder<Structure>> pair2;
            mutable.set(SectionPos.sectionToBlockCoord(chunkPos.x(), 8), 32, SectionPos.sectionToBlockCoord(chunkPos.z(), 8));
            if ((pair2 = advancedLocate$locateStructure(structures, world, structureAccessor, placement, chunkPos)) == null) continue;
            foundStructuresList.add(pair2);
        }
        return foundStructuresList.isEmpty() ? null : foundStructuresList;
    }

    @Unique
    public List<Pair<BlockPos, Holder<Structure>>> advancedLocate$locateRandomSpreadStructure(Set<Holder<Structure>> structures, LevelReader world, StructureManager structureAccessor, int centerChunkX, int centerChunkZ, int radius, long seed, RandomSpreadStructurePlacement placement,
                                                                                                     List<Pair<BlockPos, Holder<Structure>>> structureList, int amount) {
        List<Pair<BlockPos, Holder<Structure>>> foundStructuresList = new ArrayList<>();
        int i = placement.spacing();
        for (int j = -radius; j <= radius; j++) {
            boolean bl = j == -radius || j == radius;
            for (int k = -radius; k <= radius; k++) {
                Pair<BlockPos, Holder<Structure>> pair;
                boolean bl2 = k == -radius || k == radius;
                if (bl || bl2) {
                    pair = advancedLocate$locateStructure(structures, world, structureAccessor, placement, placement.getPotentialStructureChunk(seed, centerChunkX + i * j, centerChunkZ + i * k));
                    if (pair != null) {
                        foundStructuresList.add(pair);
                    }
                }
            }
            if (foundStructuresList.size() + structureList.size() >= amount) return foundStructuresList;
        }
        return foundStructuresList.isEmpty() ? null : foundStructuresList;
    }

    @Unique
    public Pair<BlockPos, Holder<Structure>> advancedLocate$locateStructure(Set<Holder<Structure>> structures, LevelReader world, StructureManager structureAccessor, StructurePlacement placement, ChunkPos pos) {
        for (Holder<Structure> registryEntry : structures) {
            StructureCheckResult structurePresence = structureAccessor.checkStructurePresence(pos, registryEntry.value(), placement, false);
            if (structurePresence != StructureCheckResult.START_NOT_PRESENT) {
                if (structurePresence == StructureCheckResult.START_PRESENT) {
                    return Pair.of(placement.getLocatePos(pos), registryEntry);
                }

                ChunkAccess chunk = world.getChunk(pos.x(), pos.z(), ChunkStatus.STRUCTURE_STARTS);
                StructureStart structureStart = structureAccessor.getStartForStructure(SectionPos.bottomOf(chunk), registryEntry.value(), chunk);
                if (structureStart != null && structureStart.isValid()) {
                    return Pair.of(placement.getLocatePos(structureStart.getChunkPos()), registryEntry);
                }
            }
        }
        return null;
    }

}