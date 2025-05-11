package bot.tornado.cachedaudioprovider.util;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

public class FuzzyMatch {
    private static int distance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();

        int[][] d = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0) {
                    d[i][j] = j;
                }
                else if (j == 0) {
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

    public static <T> List<Tuple<T>> search(List<T> list, String search) {
        return list.stream()
                .map(o -> new Tuple<>(o, distance(o.toString(), search)))
                .sorted(Comparator.comparingInt(Tuple::score))
                .toList()
                .reversed();
    }

    public static <T> int getIndexOfHighestMatch(List<T> list, String search) throws NoSuchElementException {
        Tuple<T> entriesWithScore = list.stream()
                .map(o -> new Tuple<>(o, distance(o.toString(), search)))
                .max(Comparator.comparingInt(Tuple::score)).orElseThrow();
        return list.indexOf(entriesWithScore.entry);
    }

    public record Tuple<T>(T entry, int score) {}
}
