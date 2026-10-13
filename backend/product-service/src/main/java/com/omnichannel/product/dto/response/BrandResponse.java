package com.omnichannel.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponse {

    private String id;
    private String name;
    private String slug;
    private String description;
    private String logoUrl;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
