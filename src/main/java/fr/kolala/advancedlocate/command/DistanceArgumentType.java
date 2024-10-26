package fr.kolala.advancedlocate.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class DistanceArgumentType implements ArgumentType<Integer> {
    private static final Collection<String> EXAMPLES = Arrays.asList("1r", "32c", "512b", "512");
    private static final SimpleCommandExceptionType INVALID_UNIT_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.advancedlocate.structure.invalid_unit"));

    private static final Object2DoubleMap<String> UNITS = new Object2DoubleOpenHashMap<>();

    public static DistanceArgumentType distanceArgumentType() {
        return new DistanceArgumentType();
    }

    @Override
    public Integer parse(StringReader reader) throws CommandSyntaxException {
        int distanceNoUnit = reader.readInt();
        String distanceString = reader.readUnquotedString();
        double unitFactor = UNITS.getOrDefault(distanceString, 0.0);

        if (unitFactor == 0) {
            throw INVALID_UNIT_EXCEPTION.createWithContext(reader);
        } else {
            int distance = (int) (distanceNoUnit * unitFactor);
            if (distance < 0) {
                reader.setCursor(reader.getCursor());
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().createWithContext(reader, distance, 0);
            } else {
                return distance;
            }
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        StringReader reader = new StringReader(builder.getRemaining());

        try {
            reader.readInt();
        } catch (CommandSyntaxException e) {
            return builder.buildFuture();
        }

        return CommandSource.suggestMatching(UNITS.keySet(), builder.createOffset(builder.getStart() + reader.getCursor()));
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    static {
        UNITS.put("r", 32D);
        UNITS.put("c", 1D);
        UNITS.put("b", 1/16D);
        UNITS.put("", 1/16D);
    }
}