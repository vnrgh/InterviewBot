package org.example.client;

import lombok.RequiredArgsConstructor;
import org.example.dto.eleven_labs.ElevenLabsRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ElevenLabsClient {

    @Value("${eleven_labs.api.key}")
    private String apiKey;

    @Value("${eleven_labs.api.url}")
    private String apiUrl;

    @Value("${eleven_labs.api.model_id}")
    private String modelId;

    @Value("${eleven_labs.api.voice_id}")
    private String voiceId;

    private final RestTemplate restTemplate;

    public byte[] generateSpeech(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("xi-api-key", apiKey);

        ElevenLabsRequest body = ElevenLabsRequest.builder()
                .text(text)
                .model_id(modelId)
                .build();

        HttpEntity<ElevenLabsRequest> request = new HttpEntity<>(body, headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(
                apiUrl+voiceId,
                HttpMethod.POST,
                request,
                byte[].class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new IllegalStateException("Error from Eleven Labs API: " + response.getStatusCode());
        }
    }
}
