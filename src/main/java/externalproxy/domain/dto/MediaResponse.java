package externalproxy.domain.dto;

import externalproxy.domain.enumeration.MediaKind;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaResponse {
    private Long id;
    private MediaKind kind;
    private String contentType;
    private long sizeBytes;
    private String filename;
    private int sortOrder;
    private String base64;
}

