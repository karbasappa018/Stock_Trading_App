package com.trading.order_service.dto;

import com.trading.order_service.entity.OrderStatus;
import com.trading.order_service.entity.OrderType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest
{
    private String userId;

    @NotBlank(message = "Stock symbol is required")
    private String symbol;
    @NotBlank(message = "Order type is required - Buy/ Sell")
    private OrderType type;

    @Min(value = 1, message = "Quantiy must be at least one")
    private int quantity;
}
