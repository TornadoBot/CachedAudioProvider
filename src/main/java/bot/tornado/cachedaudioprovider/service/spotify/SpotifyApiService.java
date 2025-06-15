package bot.tornado.cachedaudioprovider.service.spotify;

import bot.tornado.cachedaudioprovider.aop.EnsureSpotifyToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyApiService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    private String accessToken;
    private long expiresIn;

    private void refreshAccessToken() {
        String auth = "%s:%s".formatted(this.clientId, this.clientSecret);
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> request = new HttpEntity<>("grant_type=client_credentials", headers);

        ResponseEntity<String> response = this.restTemplate.postForEntity(
                "https://accounts.spotify.com/api/token", request, String.class
        );

        try {
            JsonNode node = this.objectMapper.readTree(response.getBody());
            this.accessToken = node.get("access_token").asText();
            this.expiresIn = System.currentTimeMillis() + (node.get("expires_in").asLong() * 1000);
            log.info("Spotify access token refreshed");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected void ensureToken() {
        if (this.accessToken == null || System.currentTimeMillis() > this.expiresIn) {
            refreshAccessToken();
        }
    }

    @EnsureSpotifyToken
    public JsonNode getTrack(String spotifyId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(this.accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        String url = "https://api.spotify.com/v1/tracks/%s".formatted(spotifyId);
        ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.GET, request, String.class);

        try {
            return objectMapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
