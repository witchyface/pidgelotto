package in.usera.cmdibuzz.config;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class SuggestionProvider {

    private SuggestionProvider() {}

    public static CompletableFuture<Suggestions> getSuggestionsBuilder(SuggestionsBuilder builder, Collection<String> suggestions) {
        String cmdpools = builder.getRemainingLowerCase();

        if (suggestions.isEmpty()) {
            return Suggestions.empty();
        }

        for (String str : suggestions ) {
            if (str.toLowerCase(Locale.ROOT).startsWith(cmdpools)) {
                builder.suggest(str);
            }
        }
        return builder.buildFuture();
    }
}
