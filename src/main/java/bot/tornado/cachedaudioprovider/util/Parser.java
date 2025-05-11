package bot.tornado.cachedaudioprovider.util;

import java.time.Instant;

public class Parser {
    public static int parseIntFailSave(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static Instant parseUnixTimestamp(String s) {
        try {
            return Instant.ofEpochSecond(Long.parseLong(s));
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
