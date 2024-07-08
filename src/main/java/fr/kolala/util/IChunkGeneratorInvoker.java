package fr.kolala.util;

import net.minecraft.structure.StructureStart;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkGenerator.class)
public interface IChunkGeneratorInvoker {
    @Invoker("checkNotReferenced")
    static boolean invokeCheckNotReferenced(StructureAccessor structureAccessor, StructureStart start) {
        throw new AssertionError();
    }
}
