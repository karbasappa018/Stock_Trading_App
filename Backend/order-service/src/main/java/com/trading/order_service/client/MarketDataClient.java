package com.trading.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.Objects;

@FeignClient(name = "market-data-service", url= "${market.service.url}")
public interface MarketDataClient
{
    @GetMapping("api/v1/market/stocks/{symbol}")
    Map<String , Object> getStockPrice(
            @PathVariable String symbol
    );
}
