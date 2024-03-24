package fr.kolala.command;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import fr.kolala.util.IChunkGeneratorCustomMethods;
import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.registry.Registry;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.structure.Structure;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class AdvancedLocateCommand {
    private static final int MAX_AMOUNT = 10;
    private static final DynamicCommandExceptionType STRUCTURE_NOT_FOUND_EXCEPTION = new DynamicCommandExceptionType(id -> Text.stringifiedTranslatable("commands.locate.structure.not_found", id));
    private static final DynamicCommandExceptionType STRUCTURE_INVALID_EXCEPTION = new DynamicCommandExceptionType(id -> Text.stringifiedTranslatable("commands.locate.structure.invalid", id));


    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("loc").requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("structure")
                        .then(CommandManager.literal("nearest")
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, MAX_AMOUNT))
                                        .then(CommandManager.argument("structure", RegistryPredicateArgumentType.registryPredicate(RegistryKeys.STRUCTURE))
                                                .executes(context -> executeLocateNearestStructure(context.getSource(), RegistryPredicateArgumentType.getPredicate(context, "structure", RegistryKeys.STRUCTURE, STRUCTURE_INVALID_EXCEPTION), IntegerArgumentType.getInteger(context, "amount"))))))));
    }

    private static Optional<? extends RegistryEntryList.ListBacked<Structure>> getStructureListForPredicate(RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate, Registry<Structure> structureRegistry) {
        return predicate.getKey().map(key -> structureRegistry.getEntry(key).map(RegistryEntryList::of), structureRegistry::getEntryList);
    }

    private static int executeLocateNearestStructure(ServerCommandSource source, RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate, int amount) throws CommandSyntaxException {
        Set<Pair<BlockPos, RegistryEntry<Structure>>> structures = new HashSet<>();
        Registry<Structure> registry = source.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE);
        RegistryEntryList<Structure> registryEntryList = getStructureListForPredicate(predicate, registry).orElseThrow(() -> STRUCTURE_INVALID_EXCEPTION.create(predicate.asString()));
        BlockPos blockPos = BlockPos.ofFloored(source.getPosition());
        ServerWorld serverWorld = source.getWorld();
        Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
        for (int i = 0; i < amount; i++)
            structures.add(((IChunkGeneratorCustomMethods) serverWorld.getChunkManager().getChunkGenerator()).advancedLocate$locateStructure(serverWorld, registryEntryList, blockPos, 100, false, structures));
        stopwatch.stop();
        if (structures.isEmpty()) {
            throw STRUCTURE_NOT_FOUND_EXCEPTION.create(predicate.asString());
        }
        return sendCoordinatesForAllNearest(source, predicate, blockPos, structures, stopwatch.elapsed());
    }

    private static int sendCoordinatesForAllNearest(ServerCommandSource source, RegistryPredicateArgumentType.RegistryPredicate<?> structure, BlockPos currentPos, Set<Pair<BlockPos, RegistryEntry<Structure>>> results, Duration timeTaken) {
        int returns = 0;
        String string = structure.getKey().map(key -> key.getValue().toString(), key -> "#" + key.id() + " (" + getKeyString(results.iterator().next()) + ")");
        source.sendFeedback(() -> Text.translatable("command.advanced_locate.structure.nearest", results.size(), string, timeTaken.toMillis()), false);
        for (Pair<BlockPos, RegistryEntry<Structure>> result : results) {
            returns += sendCoordinates(source, currentPos, result);
        }
        return returns - results.size() + 1;
    }

    private static String getKeyString(Pair<BlockPos, ? extends RegistryEntry<?>> result) {
        return result.getSecond().getKey().map(key -> key.getValue().toString()).orElse("[unregistered]");
    }

    private static float getDistance(int x1, int y1, int x2, int y2) {
        int i = x2 - x1;
        int j = y2 - y1;
        return MathHelper.sqrt(i * i + j * j);
    }

    private static int sendCoordinates(ServerCommandSource source, BlockPos currentPos, Pair<BlockPos, ? extends RegistryEntry<?>> result) {
        BlockPos blockPos = result.getFirst();
        int i = MathHelper.floor(getDistance(currentPos.getX(), currentPos.getZ(), blockPos.getX(), blockPos.getZ()));
        MutableText text = Texts.bracketed(Text.translatable("chat.coordinates", blockPos.getX(), "~", blockPos.getZ())).styled(style -> style.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + blockPos.getX() + " " + "~" + " " + blockPos.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("chat.coordinates.tooltip"))));
        source.sendFeedback(() -> Text.translatable("command.advanced_locate.structure.individual", text, i), false);
        return i;
    }


}
