package bot.tornado.cachedaudioprovider.controller;

import bot.tornado.cachedaudioprovider.config.StorageProperties;
import bot.tornado.cachedaudioprovider.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Path;


@RestController
@RequestMapping("/songs")
@RequiredArgsConstructor
public class SongController {
    private final SongService songService;
    private final StorageProperties storageProperties;

    @GetMapping("/stream/{ytid}")
    public ResponseEntity<StreamingResponseBody> streamAudio(
            @PathVariable String ytid,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader
    ) {

        Path filePath = Path.of(storageProperties.getPath(), ytid + ".mp3");
        File file = filePath.toFile();

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        long fileLength = file.length();
        long start = 0;
        long end = fileLength - 1;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            try {
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isBlank()) {
                    end = Long.parseLong(ranges[1]);
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            if (start > end || end >= fileLength) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength)
                        .build();
            }
        }

        long contentLength = end - start + 1;
        HttpStatus status = (rangeHeader == null) ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "audio/mpeg");
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + ytid + ".mp3\"");
        if (status == HttpStatus.PARTIAL_CONTENT) {
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength);
        }

        StreamingResponseBody stream = getStreamingResponseBody(start, end, file);
        return new ResponseEntity<>(stream, headers, status);
    }

    private static StreamingResponseBody getStreamingResponseBody(long start, long end, File file) {
        return outputStream -> {
            byte[] buffer = new byte[8192];
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                raf.seek(start);
                long pos = start;
                while (pos <= end) {
                    int maxRead = (int) Math.min(buffer.length, end - pos + 1);
                    int read = raf.read(buffer, 0, maxRead);
                    if (read == -1) break;
                    outputStream.write(buffer, 0, read);
                    pos += read;
                }
                outputStream.flush();
            }
        };
    }
}
