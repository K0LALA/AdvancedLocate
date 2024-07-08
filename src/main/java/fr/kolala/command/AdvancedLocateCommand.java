package fr.kolala.command;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import fr.kolala.AdvancedLocate;
import fr.kolala.config.ConfigHelper;
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
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.structure.Structure;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AdvancedLocateCommand {
    private static int defaultAmount;
    private static int maxAmount;
    private static int maxDelay;
    private static int maxRadius;
    private static int maxNeighbourRadius;
    private static final DynamicCommandExceptionType STRUCTURE_NOT_FOUND_EXCEPTION = new DynamicCommandExceptionType(id -> Text.stringifiedTranslatable("commands.locate.structure.not_found", id));
    private static final DynamicCommandExceptionType STRUCTURE_INVALID_EXCEPTION = new DynamicCommandExceptionType(id -> Text.stringifiedTranslatable("commands.locate.structure.invalid", id));


    private static void updateConfigValues () {
        defaultAmount = ConfigHelper.getInt("default_amount");
        maxAmount = ConfigHelper.getInt("max_amount");
        maxDelay = ConfigHelper.getInt("max_delay");
        maxRadius = ConfigHelper.getInt("max_radius");
        maxNeighbourRadius = ConfigHelper.getInt("max_neighbour_radius");
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        updateConfigValues();

        // TODO: Fix value not getting updated, here the value is still 3 while the command set it to 5 :/
        AdvancedLocate.LOGGER.info("default_amount is updated to " + defaultAmount);

        dispatcher.register(CommandManager.literal("loc").requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("structure")
                        .then(CommandManager.literal("nearest")
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, maxAmount))
                                        .then(CommandManager.argument("structure", RegistryPredicateArgumentType.registryPredicate(RegistryKeys.STRUCTURE))
                                                .executes(context -> executeLocateNearestStructure(context.getSource(), RegistryPredicateArgumentType.getPredicate(context, "structure", RegistryKeys.STRUCTURE, STRUCTURE_INVALID_EXCEPTION), IntegerArgumentType.getInteger(context, "amount")))))
                                .then(CommandManager.argument("structure", RegistryPredicateArgumentType.registryPredicate(RegistryKeys.STRUCTURE))
                                        .executes(context -> executeLocateNearestStructure(context.getSource(), RegistryPredicateArgumentType.getPredicate(context, "structure", RegistryKeys.STRUCTURE, STRUCTURE_INVALID_EXCEPTION), defaultAmount))))));
        dispatcher.register(CommandManager.literal("slime").requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("nearest")
                        .executes(context -> executeLocateNearestSlimeChunk(context.getSource())))
                .then(CommandManager.literal("density")
                        .then(CommandManager.argument("radius", IntegerArgumentType.integer(2, maxRadius))
                                .then(CommandManager.argument("neighbour_radius", IntegerArgumentType.integer(1, maxNeighbourRadius))
                                        .executes(context -> executeLocateHighestSlimeDensity(context.getSource(), IntegerArgumentType.getInteger(context, "radius"), IntegerArgumentType.getInteger(context, "neighbour_radius")))))));
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
        for (int i = 0; i < amount; i++) {
            if (stopwatch.elapsed(TimeUnit.SECONDS) >= maxDelay)
                break;
            structures.add(((IChunkGeneratorCustomMethods) serverWorld.getChunkManager().getChunkGenerator()).advancedLocate$locateStructure(serverWorld, registryEntryList, blockPos, 100, false, structures));
        }
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


    private static boolean isSlimeChunk(long seed, int xPos, int zPos) {
        Random random = new Random(seed +
                ((long) xPos * xPos * 0x4c1906) +
                (xPos * 0x5ac0dbL) +
                ((long) zPos * zPos) * 0x4307a7L +
                (zPos * 0x5f24fL) ^ 0x3ad8025fL);
        return random.nextInt(10) == 0;
    }

    private static void locatedSlimeChunk (ServerCommandSource source, int xPos, int zPos) {
        source.sendFeedback(() -> Text.translatable("command.advanced_locate.slime.nearest", xPos, zPos), false);
    }

    private static int executeLocateNearestSlimeChunk(ServerCommandSource source) {
        long seed = source.getWorld().getSeed();
        if (source.getPlayer() == null) {
            AdvancedLocate.LOGGER.error("You can't run this command from the server!");
            return 1;
        }
        if (source.getPlayer().getServerWorld().getDimensionKey() != DimensionTypes.OVERWORLD) {
            source.sendFeedback(() -> Text.translatable("command.advanced_locate.slime.wrong_dimension").styled(style -> style.withColor(Formatting.RED)), false);
            return 1;
        }
        int xPos = source.getPlayer().getChunkPos().x;
        int zPos = source.getPlayer().getChunkPos().z;

        if (isSlimeChunk(seed, xPos, zPos)) {
            int finalXPos = xPos;
            int finalZPos = zPos;
            source.sendFeedback(() -> Text.translatable("command.advanced_locate.slime.yes", finalXPos, finalZPos), false);
            return 0;
        }

        int alternator = 1;
        for (int travelLength = 1; travelLength <= 5; travelLength++) {
            if (travelLength % 2 == 0) alternator *= -1;
            for (int i = 0; i < travelLength; i++) {
                xPos += alternator;
                if (isSlimeChunk(seed, xPos, zPos)) {
                    locatedSlimeChunk(source, xPos, zPos);
                    return 0;
                }
            }
            for (int i = 0; i < travelLength; i++) {
                zPos -= alternator;
                if (isSlimeChunk(seed, xPos, zPos)) {
                    locatedSlimeChunk(source, xPos, zPos);
                    return 0;
                }
            }
        }

        return 0;
    }

    private static int executeLocateHighestSlimeDensity(ServerCommandSource source, int radius, int neighbour_radius) {
        long seed = source.getWorld().getSeed();
        if (source.getPlayer() == null) {
            AdvancedLocate.LOGGER.error("You can't run this command from the server!");
            return 1;
        }
        if (source.getPlayer().getServerWorld().getDimensionKey() != DimensionTypes.OVERWORLD) {
            source.sendFeedback(() -> Text.translatable("command.advanced_locate.slime.wrong_dimension").styled(style -> style.withColor(Formatting.RED)), false);
            return 1;
        }
        if (neighbour_radius > radius) {
            source.sendFeedback(() -> Text.translatable("command.advanced_locate.slime.neighbour_greater").styled(style -> style.withColor(Formatting.RED)), false);
            return 1;
        }


        int centerX = source.getPlayer().getChunkPos().x;
        int centerZ = source.getPlayer().getChunkPos().z;

        final int size = radius * 2 + 1;
        boolean[][] slimeGrid = new boolean[size][size];
        for (int xPos = -radius; xPos <= radius; xPos++) {
            for (int zPos = -radius; zPos <= radius; zPos++) {
                slimeGrid[xPos + radius][zPos + radius] = isSlimeChunk(seed, xPos + centerX, zPos + centerZ);
            }
        }

        int xOffset = centerX - radius;
        int zOffset = centerZ - radius;

        Pair<Integer, Pair<Integer, Integer>> highestDensityPoint = new Pair<>(0, new Pair<>(0, 0));
        for (int xGrid = centerX - radius + neighbour_radius; xGrid <= centerX + radius - neighbour_radius; xGrid++) {
            for (int zGrid = centerZ - radius + neighbour_radius; zGrid <= centerZ + radius - neighbour_radius; zGrid++) {
                int slimeChunks = 0;
                for (int xPos = xGrid - neighbour_radius; xPos <= xGrid + neighbour_radius; xPos++) {
                    for (int zPos = zGrid - neighbour_radius; zPos <= zGrid + neighbour_radius; zPos++) {
                        if (slimeGrid[xPos - xOffset][zPos - zOffset]) {
                            slimeChunks++;
                        }
                    }
                }

                if (slimeChunks > highestDensityPoint.getFirst()) {
                    highestDensityPoint = Pair.of(slimeChunks, Pair.of(xGrid, zGrid));
                }
            }
        }

        Pair<Integer, Pair<Integer, Integer>> finalHighestDensityPoint = highestDensityPoint;
        source.sendFeedback(() -> Text.translatable("command.advanced_locate.slime.density", radius,
                finalHighestDensityPoint.getSecond().getFirst(), finalHighestDensityPoint.getSecond().getSecond(),
                finalHighestDensityPoint.getFirst(), neighbour_radius), false);

        return 0;
    }
}
