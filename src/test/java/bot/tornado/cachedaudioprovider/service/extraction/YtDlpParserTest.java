package bot.tornado.cachedaudioprovider.service.extraction;

import bot.tornado.cachedaudioprovider.dto.YtDlpEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class YtDlpParserTest {

    @Autowired
    private YtDlpParser ytDlpParser;

    @Test
    void parseSingleEntry_shouldParseJsonCorrectly() throws IOException {
        String json = Files.readString(Paths.get("src/main/resources/sample/ytdlp-entry-json-example-0.json"));

        Optional<YtDlpEntry> result = this.ytDlpParser.parseSingleEntry(json);

        assertTrue(result.isPresent());

        YtDlpEntry entry = result.get();
        assertEquals("R_BO8C05XLA", entry.getId());
        assertEquals("Radioactive", entry.getTitle());
        assertEquals("ImagineDragons", entry.getUploader());
        assertEquals("https://www.youtube.com/channel/UC0aXrjVxG5pZr99v77wZdPQ", entry.getChannelUrl());
        assertEquals(1451865, entry.getLikeCount());
        assertEquals(206575836, entry.getViewCount());
        assertEquals(1544616712, entry.getUploadTimestamp());
        assertEquals("Imagine Dragons", entry.getArtist());
        assertEquals("Radioactive", entry.getTrack());
        assertEquals("Night Visions", entry.getAlbum());
    }
}
