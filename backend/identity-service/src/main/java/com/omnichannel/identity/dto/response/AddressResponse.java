package com.omnichannel.identity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private UUID id;
    private String recipientName;
    private String phone;
    private String streetAddress;
    private String ward;
    private String district;
    private String city;
    private Boolean isDefault;
    private Instant createdAt;
}
