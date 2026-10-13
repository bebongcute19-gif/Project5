package com.omnichannel.identity.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @Pattern(regexp = "ACTIVE|LOCKED|INACTIVE", message = "Status must be ACTIVE, LOCKED, or INACTIVE")
    private String status;

    private Boolean enabled;
}
