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
    // TODO: Ajuster le locate our les tags de structures, car ne prends en compte que la première (affichage et peut-être locates)
    @Override
    public List<Pair<BlockPos, RegistryEntry<Structure>>> advancedLocate$locateStructure(ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int radius, int amount) {
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
        StructureAccessor structureAccessor = world.getStructureAccessor();
        ArrayList<Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>>> list = new ArrayList<>(map.size());
        List<Pair<BlockPos, RegistryEntry<Structure>>> structureList = new ArrayList<>();
        for (Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry : map.entrySet()) {
            StructurePlacement structurePlacement2 = entry.getKey();
            if (structurePlacement2 instanceof ConcentricRingsStructurePlacement concentricRingsStructurePlacement) {
                List<Pair<BlockPos, RegistryEntry<Structure>>> foundPairs = advancedLocate$locateConcentricRingsStructure(entry.getValue(), world, structureAccessor, concentricRingsStructurePlacement);
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
            int i = ChunkSectionPos.getSectionCoord(center.getX());
            int j = ChunkSectionPos.getSectionCoord(center.getZ());
            for (int k = 0; k <= radius; ++k) {
                for (Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry : list) {
                    RandomSpreadStructurePlacement randomSpreadStructurePlacement = (RandomSpreadStructurePlacement)entry.getKey();
                    List<Pair<BlockPos, RegistryEntry<Structure>>> foundPairs = advancedLocate$locateRandomSpreadStructure(entry.getValue(), world, structureAccessor, i, j, k, structurePlacementCalculator.getStructureSeed(), randomSpreadStructurePlacement, structureList, amount);
                    if (foundPairs == null || foundPairs.isEmpty()) {
                        continue;
                    }
                    structureList.addAll(foundPairs);
                    sortStructureList(structureList, center);
                    structureList = shrinkStructureList(structureList, amount);
                    if (structureList.size() >= amount) return structureList;
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
    private void sortStructureList(List<Pair<BlockPos, RegistryEntry<Structure>>> structureList, BlockPos center) {
        structureList.sort((o1, o2) -> (int) (o1.getFirst().getSquaredDistance(center) - o2.getFirst().getSquaredDistance(center)));
    }

    /**
     * Shrinks the structure list to be only of a maximum amount
     * @param structureList The list of structures to shrink
     * @param amount The maximum amount of elements in the list
     * @return The list containing the amount (or less) of elements wanted
     */
    @Unique
    List<Pair<BlockPos, RegistryEntry<Structure>>> shrinkStructureList(List<Pair<BlockPos, RegistryEntry<Structure>>> structureList, int amount) {
        return structureList.subList(0, Math.min(structureList.size(), amount));
    }

    @Unique
    public List<Pair<BlockPos, RegistryEntry<Structure>>> advancedLocate$locateConcentricRingsStructure(Set<RegistryEntry<Structure>> structures, ServerWorld world, StructureAccessor structureAccessor, ConcentricRingsStructurePlacement placement) {
        List<Pair<BlockPos, RegistryEntry<Structure>>> foundStructuresList = new ArrayList<>();
        List<ChunkPos> list = world.getChunkManager().getStructurePlacementCalculator().getPlacementPositions(placement);
        if (list == null) {
            throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
        }
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (ChunkPos chunkPos : list) {
            Pair<BlockPos, RegistryEntry<Structure>> pair2;
            mutable.set(ChunkSectionPos.getOffsetPos(chunkPos.x, 8), 32, ChunkSectionPos.getOffsetPos(chunkPos.z, 8));
            if ((pair2 = advancedLocate$locateStructure(structures, world, structureAccessor, placement, chunkPos)) == null) continue;
            foundStructuresList.add(pair2);
        }
        return foundStructuresList.isEmpty() ? null : foundStructuresList;
    }

    @Unique
    public List<Pair<BlockPos, RegistryEntry<Structure>>> advancedLocate$locateRandomSpreadStructure(Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, int centerChunkX, int centerChunkZ, int radius, long seed, RandomSpreadStructurePlacement placement,
                                                                                                     List<Pair<BlockPos, RegistryEntry<Structure>>> structureList, int amount) {
        List<Pair<BlockPos, RegistryEntry<Structure>>> foundStructuresList = new ArrayList<>();
        int i = placement.getSpacing();
        for (int j = -radius; j <= radius; j++) {
            boolean bl = j == -radius || j == radius;
            for (int k = -radius; k <= radius; k++) {
                Pair<BlockPos, RegistryEntry<Structure>> pair;
                boolean bl2 = k == -radius || k == radius;
                if (bl || bl2) {
                    pair = advancedLocate$locateStructure(structures, world, structureAccessor, placement, placement.getStartChunk(seed, centerChunkX + i * j, centerChunkZ + i * k));
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
    public Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateStructure(Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, StructurePlacement placement, ChunkPos pos) {
        for (RegistryEntry<Structure> registryEntry : structures) {
            StructurePresence structurePresence = structureAccessor.getStructurePresence(pos, registryEntry.value(), placement, false);
            if (structurePresence != StructurePresence.START_NOT_PRESENT) {
                if (structurePresence == StructurePresence.START_PRESENT) {
                    return Pair.of(placement.getLocatePos(pos), registryEntry);
                }

                Chunk chunk = world.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS);
                StructureStart structureStart = structureAccessor.getStructureStart(ChunkSectionPos.from(chunk), registryEntry.value(), chunk);
                if (structureStart != null && structureStart.hasChildren()) {
                    return Pair.of(placement.getLocatePos(structureStart.getPos()), registryEntry);
                }
            }
        }
        return null;
    }

}