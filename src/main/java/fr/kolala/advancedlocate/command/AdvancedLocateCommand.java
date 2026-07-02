package fr.kolala.advancedlocate.command;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import fr.kolala.advancedlocate.AdvancedLocate;
import fr.kolala.advancedlocate.config.ConfigHelper;
import fr.kolala.advancedlocate.util.IChunkGeneratorCustomMethods;
import fr.kolala.advancedlocate.util.MapViewSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import java.time.Duration;
import java.util.*;

public class AdvancedLocateCommand {
    private static final int MAX_AMOUNT = ConfigHelper.getIntOrDefault("max_amount");
    private static final int MAX_RADIUS = ConfigHelper.getIntOrDefault("max_radius");
    private static final int MAX_NEIGHBOUR_RADIUS = ConfigHelper.getIntOrDefault("max_neighbour_radius");
    private static final DynamicCommandExceptionType STRUCTURE_NOT_FOUND_EXCEPTION = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.locate.structure.not_found", id)
    );
    private static final DynamicCommandExceptionType STRUCTURE_INVALID_EXCEPTION = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.locate.structure.invalid", id)
    );
    private static void sideError() {
        AdvancedLocate.LOGGER.error("You can't run this command from the server!");
    }


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("loc").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("structure")
                        .then(Commands.literal("nearest")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, MAX_AMOUNT))
                                        .then(Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                                                .executes(context -> executeLocateNearestStructureAmount(context.getSource(),
                                                        ResourceOrTagKeyArgument.getResourceOrTagKey(context, "structure", Registries.STRUCTURE, STRUCTURE_INVALID_EXCEPTION),
                                                        IntegerArgumentType.getInteger(context, "amount")))

                                                .then(Commands.argument("max_distance", DistanceArgumentType.distanceArgumentType())
                                                        .executes(context -> executeLocateNearestStructure(context.getSource(),
                                                        ResourceOrTagKeyArgument.getResourceOrTagKey(context, "structure", Registries.STRUCTURE, STRUCTURE_INVALID_EXCEPTION),
                                                                IntegerArgumentType.getInteger(context, "amount"),
                                                                IntegerArgumentType.getInteger(context, "max_distance"))))))

                                .then(Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                                        .executes(context -> executeLocateNearestStructureDefault(context.getSource(),
                                                ResourceOrTagKeyArgument.getResourceOrTagKey(context, "structure", Registries.STRUCTURE, STRUCTURE_INVALID_EXCEPTION)))

                                        .then(Commands.argument("max_distance", DistanceArgumentType.distanceArgumentType())
                                                .executes(context -> executeLocateNearestStructureMaxDistance(context.getSource(),
                                                        ResourceOrTagKeyArgument.getResourceOrTagKey(context, "structure", Registries.STRUCTURE, STRUCTURE_INVALID_EXCEPTION),
                                                        IntegerArgumentType.getInteger(context, "max_distance"))))))));


        dispatcher.register(Commands.literal("slime").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("nearest")
                        .executes(context -> executeLocateNearestSlimeChunk(context.getSource())))
                .then(Commands.literal("density")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(2, MAX_RADIUS))
                                .then(Commands.argument("neighbour_radius", IntegerArgumentType.integer(1, MAX_NEIGHBOUR_RADIUS))
                                        .executes(context -> executeLocateHighestSlimeDensity(context.getSource(), IntegerArgumentType.getInteger(context, "radius"), IntegerArgumentType.getInteger(context, "neighbour_radius")))))));
    }

    private static Optional<? extends HolderSet.ListBacked<Structure>> getStructureListForPredicate(ResourceOrTagKeyArgument.Result<Structure> predicate, Registry<Structure> structureRegistry) {
        return predicate.unwrap().map(key -> structureRegistry.get(key).map(HolderSet::direct), structureRegistry::get);
    }

    // `/loc structure nearest (amount) [STRUCTURE] (max_distance)`,
    // amount being the amount of structures you want to search for (default: 5),
    // STRUCTURE being the structure type(s) you want to search for,
    // max_distance being the maximum distance (in blocks) of the structures (default: 1600)
    private static int executeLocateNearestStructureDefault(CommandSourceStack source, ResourceOrTagKeyArgument.Result<Structure> predicate) throws CommandSyntaxException {
        return executeLocateNearestStructure(source, predicate, ConfigHelper.getIntOrDefault("default_amount"), ConfigHelper.getIntOrDefault("default_max_distance") / 16);
    }

    private static int executeLocateNearestStructureAmount(CommandSourceStack source, ResourceOrTagKeyArgument.Result<Structure> predicate, int amount) throws CommandSyntaxException {
        return executeLocateNearestStructure(source, predicate, amount, ConfigHelper.getIntOrDefault("default_max_distance") / 16);
    }

    private static int executeLocateNearestStructureMaxDistance(CommandSourceStack source, ResourceOrTagKeyArgument.Result<Structure> predicate, int maxDistance) throws CommandSyntaxException {
        return executeLocateNearestStructure(source, predicate, ConfigHelper.getIntOrDefault("default_amount"), maxDistance);
    }

    private static int executeLocateNearestStructure(CommandSourceStack source, ResourceOrTagKeyArgument.Result<Structure> predicate, int amount, int maxDistance) throws CommandSyntaxException {
        List<Pair<BlockPos, Holder<Structure>>> structures;
        Registry<Structure> registry = source.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        HolderSet<Structure> registryEntryList = getStructureListForPredicate(predicate, registry).orElseThrow(() -> STRUCTURE_INVALID_EXCEPTION.create(predicate.asPrintable()));
        BlockPos blockPos = BlockPos.containing(source.getPosition());
        ServerLevel serverWorld = source.getLevel();
        Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
        structures = ((IChunkGeneratorCustomMethods) serverWorld.getChunkSource().getGenerator()).advancedLocate$locateStructure(serverWorld, registryEntryList, blockPos, maxDistance, amount);
        stopwatch.stop();
        if (structures == null || structures.isEmpty()) {
            throw STRUCTURE_NOT_FOUND_EXCEPTION.create(predicate.asPrintable());
        }
        structures.removeIf(structure -> {
            BlockPos structurePosition = structure.getFirst();
            float distance = getDistance(structurePosition.getX(), structurePosition.getZ(), blockPos.getX(), blockPos.getZ());
            return distance > maxDistance * 16;
        });
        return sendCoordinatesForAllNearest(source, predicate, blockPos, structures, stopwatch.elapsed(), maxDistance, registryEntryList.size() > 1);
    }

    private static int sendCoordinatesForAllNearest(CommandSourceStack source, ResourceOrTagKeyArgument.Result<?> structure, BlockPos currentPos, List<Pair<BlockPos, Holder<Structure>>> results, Duration timeTaken, int maxDistance, boolean tag) {
        int returns = 0;
        String string = structure.unwrap().map(key -> key.identifier().toString(), key -> "#" + key.location());
        if (results.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.advancedlocate.structure.not_found", string, maxDistance), false);
        }
        source.sendSuccess(() -> Component.translatable("command.advancedlocate.structure.nearest", results.size(), string, timeTaken.toMillis()), false);
        for (Pair<BlockPos, Holder<Structure>> result : results) {
            returns += sendCoordinates(source, currentPos, result, tag);
        }
        return returns - results.size() + 1;
    }

    private static String getKeyString(Pair<BlockPos, ? extends Holder<?>> result) {
        return result.getSecond().unwrapKey().map(key -> key.identifier().toString()).orElse("[unregistered]");
    }

    private static float getDistance(int x1, int y1, int x2, int y2) {
        int i = x2 - x1;
        int j = y2 - y1;
        return Mth.sqrt(i * i + j * j);
    }

    private static int sendCoordinates(CommandSourceStack source, BlockPos currentPos, Pair<BlockPos, ? extends Holder<?>> result, boolean tag) {
        BlockPos blockPos = result.getFirst();
        int i = Mth.floor(getDistance(currentPos.getX(), currentPos.getZ(), blockPos.getX(), blockPos.getZ()));
        MutableComponent text = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", blockPos.getX(), "~", blockPos.getZ())).withStyle(style -> style.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent.SuggestCommand("/tp @s " + blockPos.getX() + " " + "~" + " " + blockPos.getZ())).withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip"))));
        String string = tag ? " (" + getKeyString(result) + ")" : "";
        source.sendSuccess(() -> Component.translatable("command.advancedlocate.structure.individual", text, i, string), false);
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

    private static void locatedSlimeChunk (CommandSourceStack source, int xPos, int zPos) {
        source.sendSuccess(() -> Component.translatable("command.advancedlocate.slime.nearest", xPos, zPos), false);
    }

    private static int executeLocateNearestSlimeChunk(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        MapViewSavedData mapData = MapViewSavedData.getMapViewData(server);
        AdvancedLocate.LOGGER.info(mapData.toString());

        long seed = source.getLevel().getSeed();
        if (source.getPlayer() == null) {
            sideError();
            return 1;
        }
        if (!source.getLevel().dimension().equals(Level.OVERWORLD)) {
            source.sendSuccess(() -> Component.translatable("command.advancedlocate.slime.wrong_dimension").withStyle(style -> style.withColor(ChatFormatting.RED)), false);
            return 1;
        }
        int xPos = source.getPlayer().chunkPosition().x();
        int zPos = source.getPlayer().chunkPosition().z();

        if (isSlimeChunk(seed, xPos, zPos)) {
            int finalXPos = xPos;
            int finalZPos = zPos;
            source.sendSuccess(() -> Component.translatable("command.advancedlocate.slime.yes", finalXPos, finalZPos), false);
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

    private static int executeLocateHighestSlimeDensity(CommandSourceStack source, int radius, int neighbour_radius) {
        long seed = source.getLevel().getSeed();
        if (source.getPlayer() == null) {
            sideError();
            return 1;
        }
        if (!source.getLevel().dimension().equals(Level.OVERWORLD)) {
            source.sendSuccess(() -> Component.translatable("command.advancedlocate.slime.wrong_dimension").withStyle(style -> style.withColor(ChatFormatting.RED)), false);
            return 1;
        }
        if (neighbour_radius > radius) {
            source.sendSuccess(() -> Component.translatable("command.advancedlocate.slime.neighbour_greater").withStyle(style -> style.withColor(ChatFormatting.RED)), false);
            return 1;
        }


        int centerX = source.getPlayer().chunkPosition().x();
        int centerZ = source.getPlayer().chunkPosition().z();

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
        source.sendSuccess(() -> Component.translatable("command.advancedlocate.slime.density", radius,
                finalHighestDensityPoint.getSecond().getFirst(), finalHighestDensityPoint.getSecond().getSecond(),
                finalHighestDensityPoint.getFirst(), neighbour_radius), false);

        return 0;
    }
}