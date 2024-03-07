package fr.kolala.command;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import fr.kolala.AdvancedLocate;
import fr.kolala.mixin.ChunkGeneratorMixin;
import fr.kolala.util.IChunkGeneratorCustomMethods;
import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Shadow;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class AdvancedLocateCommand {
    private static final DynamicCommandExceptionType STRUCTURE_NOT_FOUND_EXCEPTION = new DynamicCommandExceptionType(id -> Text.stringifiedTranslatable("commands.locate.structure.not_found", id));
    private static final DynamicCommandExceptionType STRUCTURE_INVALID_EXCEPTION = new DynamicCommandExceptionType(id -> Text.stringifiedTranslatable("commands.locate.structure.invalid", id));


    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("loc")
                .then(CommandManager.literal("structure")
                        .then(CommandManager.argument("structure", RegistryPredicateArgumentType.registryPredicate(RegistryKeys.STRUCTURE))
                                .executes(context -> executeLocateStructure(context.getSource(), RegistryPredicateArgumentType.getPredicate(context, "structure", RegistryKeys.STRUCTURE, STRUCTURE_INVALID_EXCEPTION))))));
    }

    private static Optional<? extends RegistryEntryList.ListBacked<Structure>> getStructureListForPredicate(RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate, Registry<Structure> structureRegistry) {
        return predicate.getKey().map(key -> structureRegistry.getEntry(key).map(RegistryEntryList::of), structureRegistry::getEntryList);
    }

    private static Set<RegistryEntry<Structure>> getStructureSet(RegistryEntryList<Structure> structures) {
        Set<RegistryEntry<Structure>> structureSet = new HashSet<>();
        for (RegistryEntry<Structure> structure : structures) {
            structureSet.add(structure);
        }
        return structureSet;
    }

    private static int executeLocateStructure(ServerCommandSource source, RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate) throws CommandSyntaxException {
        Set<Pair<BlockPos, RegistryEntry<Structure>>> structures = new HashSet<>();
        // TODO: Adapt this method to handle the structureSet
        // Changing the used method to the one used by the locate command might be the solution, still using the same method to check for already found structures,
        // but needs to overwrite methods all the way down with mixins, which may be a lot,
        // may be simpler with injects, maybe we can look for an inject on method call, but not sure
        // we could counter this by injecting at first return and overwriting the returned element based on the methods we want to call

        Registry<Structure> registry = source.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE);
        RegistryEntryList<Structure> registryEntryList = getStructureListForPredicate(predicate, registry).orElseThrow(() -> STRUCTURE_INVALID_EXCEPTION.create(predicate.asString()));
        BlockPos blockPos = BlockPos.ofFloored(source.getPosition());
        ServerWorld serverWorld = source.getWorld();
        Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
        Pair<BlockPos, RegistryEntry<Structure>> pair = ((IChunkGeneratorCustomMethods) serverWorld.getChunkManager().getChunkGenerator()).advancedLocate$locateStructure(getStructureSet(registryEntryList), serverWorld, serverWorld.getStructureAccessor(), false, serverWorld.getChunkManager().getStructurePlacementCalculator().getPlacements(registryEntryList.get(0)).getFirst(), ChunkPos.fromRegionCenter(0, 0), structures);
        stopwatch.stop();
        if (pair == null) {
            AdvancedLocate.LOGGER.info("Did not find anything.");
            throw STRUCTURE_NOT_FOUND_EXCEPTION.create(predicate.asString());
        }
        AdvancedLocate.LOGGER.info("Found something :");
        AdvancedLocate.LOGGER.info(pair.getFirst().toString());
        AdvancedLocate.LOGGER.info(pair.getSecond().toString());
        return sendCoordinates(source, predicate, blockPos, pair, "command.locate.structure.success", false, stopwatch.elapsed());
    }

    private static String getKeyString(Pair<BlockPos, ? extends RegistryEntry<?>> result) {
        return result.getSecond().getKey().map(key -> key.getValue().toString()).orElse("[unregistered]");
    }

    private static float getDistance(int x1, int y1, int x2, int y2) {
        int i = x2 - x1;
        int j = y2 - y1;
        return MathHelper.sqrt(i * i + j * j);
    }

    public static int sendCoordinates(ServerCommandSource source, RegistryPredicateArgumentType.RegistryPredicate<?> structure, BlockPos currentPos, Pair<BlockPos, ? extends RegistryEntry<?>> result, String successMessage, boolean includeY, Duration timeTaken) {
        String string = structure.getKey().map(key -> key.getValue().toString(), key -> "#" + key.id() + " (" + getKeyString(result) + ")");
        return sendCoordinates(source, currentPos, result, successMessage, includeY, string, timeTaken);
    }

    private static int sendCoordinates(ServerCommandSource source, BlockPos currentPos, Pair<BlockPos, ? extends RegistryEntry<?>> result, String successMessage, boolean includeY, String entryString, Duration timeTaken) {
        BlockPos blockPos = result.getFirst();
        int i = includeY ? MathHelper.floor(MathHelper.sqrt((float)currentPos.getSquaredDistance(blockPos))) : MathHelper.floor(getDistance(currentPos.getX(), currentPos.getZ(), blockPos.getX(), blockPos.getZ()));
        String string = includeY ? String.valueOf(blockPos.getY()) : "~";
        MutableText text = Texts.bracketed(Text.translatable("chat.coordinates", blockPos.getX(), string, blockPos.getZ())).styled(style -> style.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + blockPos.getX() + " " + string + " " + blockPos.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("chat.coordinates.tooltip"))));
        source.sendFeedback(() -> Text.translatable("commands.locate.structure.success", entryString, text, i), false);
        AdvancedLocate.LOGGER.info("Locating element " + entryString + " took " + timeTaken.toMillis() + " ms at " + text);
        return i;
    }


}
