package com.trading.userservice.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest
{

    @NotBlank(message = "Email is Required")
    @Email(message = "Invalid email format")
    private String email;


    @NotBlank(message = "Password is required")
    @Size(min = 8,message = "Password must be at least 6 character long")
    private String password;

    @NotBlank(message = "First name is requires")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private BigDecimal initialDeposit = BigDecimal.valueOf(10000);
}
