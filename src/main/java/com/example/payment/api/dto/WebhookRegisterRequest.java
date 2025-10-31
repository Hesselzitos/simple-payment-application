package com.example.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WebhookRegisterRequest {
    @NotBlank
    private String endpointUrl;

}
