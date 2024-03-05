package fr.kolala.mixin;

import com.mojang.datafixers.util.Pair;
import fr.kolala.AdvancedLocate;
import fr.kolala.util.IChunkGeneratorCustomMethods;
import fr.kolala.util.IChunkGeneratorInvoker;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Set;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin implements IChunkGeneratorInvoker, IChunkGeneratorCustomMethods {
    @Shadow @Nullable protected abstract Pair<BlockPos, RegistryEntry<Structure>> locateConcentricRingsStructure(Set<RegistryEntry<Structure>> structures, ServerWorld world, StructureAccessor structureAccessor, BlockPos center, boolean skipReferencedStructures, ConcentricRingsStructurePlacement placement);

    @Override
    public Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateStructuresInRadius(ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int radius, boolean skipReferencedStructures) {
        AdvancedLocate.LOGGER.info("Locating structures.");

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

        AdvancedLocate.LOGGER.info("Created map.");

        Pair<BlockPos, RegistryEntry<Structure>> pair = null;
        double d = Double.MAX_VALUE;
        StructureAccessor structureAccessor = world.getStructureAccessor();
        for (Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry : map.entrySet()) {
            StructurePlacement structurePlacement = (StructurePlacement) entry.getKey();
            if (structurePlacement instanceof ConcentricRingsStructurePlacement concentricRingsStructurePlacement) {

                AdvancedLocate.LOGGER.info("Looking for structures generated in concentric rings.");

                BlockPos blockPos;
                double e;
                Pair<BlockPos, RegistryEntry<Structure>> pair_ = locateConcentricRingsStructure(entry.getValue(), world, structureAccessor, center, skipReferencedStructures, concentricRingsStructurePlacement);
                if (pair_ == null || !((e = center.getSquaredDistance(blockPos = pair_.getFirst())) < d)) continue;
                d = e;
                pair = pair_;
                continue;
            }
            /*if (!(structurePlacement instanceof RandomSpreadStructurePlacement)) {
                map.remove(entry);
                continue;
            }*/
        }
        if (!map.isEmpty()) {
            int i = ChunkSectionPos.getSectionCoord(center.getX());
            int j = ChunkSectionPos.getSectionCoord(center.getZ());
            for (int k = 0; k <= radius; k++) {
                boolean bl = false;
                for (Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry : map.entrySet()) {

                    for (RegistryEntry<Structure> entry1 : entry.getValue()) {
                        AdvancedLocate.LOGGER.info(entry1.getKey().toString());
                    }

                    AdvancedLocate.LOGGER.info("Looking for structures generated in random spreads.");

                    RandomSpreadStructurePlacement randomSpreadStructurePlacement = (RandomSpreadStructurePlacement) entry.getKey();
                    Pair<BlockPos, RegistryEntry<Structure>> pair_ = IChunkGeneratorInvoker.invokeLocateRandomSpreadStructure(entry.getValue(), world, structureAccessor, i, j, k, skipReferencedStructures, structurePlacementCalculator.getStructureSeed(), randomSpreadStructurePlacement);
                    if (pair_ == null) continue;
                    bl = true;
                    double f = center.getSquaredDistance(pair_.getFirst());
                    if (!(f < d)) continue;
                    d = f;
                    pair = pair_;
                }
                if (!bl) continue;
                return pair;
            }
        }
        return pair;
    }

    @Override
    public Pair<BlockPos, RegistryEntry<Structure>> advancedLocate$locateNearestStructures (ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int structureAmount, int radius, boolean skipReferencedStructures) {
        return null;
    }
}