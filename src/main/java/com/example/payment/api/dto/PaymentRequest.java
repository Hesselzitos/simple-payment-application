package com.example.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PaymentRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Size(min = 5, max = 10)
    @Pattern(regexp = "^[0-9- ]+$", message = "zipCode must contain digits, space, or hyphen")
    private String zipCode;

    @NotBlank
    @Pattern(regexp = "^[0-9]{12,19}$", message = "cardNumber must be 12-19 digits")
    private String cardNumber;

}
