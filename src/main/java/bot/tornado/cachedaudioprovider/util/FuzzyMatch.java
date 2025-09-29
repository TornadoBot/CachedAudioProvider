package bot.tornado.cachedaudioprovider.util;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

public class FuzzyMatch {

    /**
     * Represents a fuzzy match result with original entry and similarity score.
     *
     * @param entry the original object
     * @param similarity value between 0 (no match) and 1 (perfect match)
     */
    public record Match<T>(T entry, double similarity) {}


    /**
     * Searches and ranks entries by similarity to the search string.
     */
    public static <T> List<Match<T>> search(List<T> entries, String search, Function<T, String> mapper) {
        return entries.stream()
                .map(entry -> {
                    String candidate = mapper.apply(entry);
                    int score = levenshtein(candidate.toLowerCase(), search.toLowerCase());
                    double similarity = computeSimilarity(candidate, search, score);
                    return new Match<>(entry, similarity);
                })
                .sorted(Comparator.comparingDouble(Match::similarity))
                .collect(Collectors.toList());
    }

    /**
     * Computes the Levenshtein distance between two strings.
     */
    private static int levenshtein(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] d = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0) {
                    d[i][j] = j;
                } else if (j == 0) {
                    d[i][j] = i;
                } else {
                    d[i][j] = Math.min(
                            d[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1)
                    );
                }
            }
        }
        return d[m][n];
    }

    /**
     * Returns the best match for a given search string if similarity is above a threshold.
     */
    public static <T> Optional<Match<T>> bestMatch(List<T> entries, String search, Function<T, String> mapper, double minSimilarity) {
        return search(entries, search, mapper).stream()
                .filter(m -> m.similarity >= minSimilarity)
                .findFirst();
    }

    /**
     * Returns the best match for a pre-scored list of entries.
     */
    public static <T> Optional<Match<T>> bestMatch(List<T> entries, ToDoubleFunction<T> scorer, double minSimilarity) {
        return entries.stream()
                .map(entry -> new Match<>(entry, scorer.applyAsDouble(entry)))
                .filter(match -> match.similarity() >= minSimilarity)
                .max(Comparator.comparingDouble(Match::similarity));
    }

    private static double computeSimilarity(String a, String b, int score) {
        int maxLen = Math.max(a.length(), b.length());
        return maxLen == 0 ? 1.0 : 1.0 - ((double) score / maxLen);
    }

    public static double similarity(String a, String b) {
        int distance = levenshtein(a.toLowerCase(), b.toLowerCase());
        return computeSimilarity(a, b, distance);
    }

}
