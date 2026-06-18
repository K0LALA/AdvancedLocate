package fr.kolala.advancedlocate.util;

import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

public interface IChunkGeneratorCustomMethods {

    @Nullable
    List<Pair<BlockPos, Holder<Structure>>> advancedLocate$locateStructure(ServerLevel world, HolderSet<Structure> structures, BlockPos center, int radius, int amount);

}