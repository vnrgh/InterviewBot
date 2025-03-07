package org.example.dto.eleven_labs;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ElevenLabsRequest {
    private String text;
    private String model_id;
}
