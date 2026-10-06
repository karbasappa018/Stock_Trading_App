package com.trading.marketdataservice.service;

import com.trading.marketdataservice.dto.StockPrice;
import com.trading.marketdataservice.entity.Stock;
import com.trading.marketdataservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataService
{
    private final StockRepository stockRepository;

    /*
    * ConcurrentHashMap - for thread Safety
    * */

    private final Map<String, StockPrice> stockPrices = new ConcurrentHashMap<>();

    /*
    * Initialize Stock Prizes
    *
    * @Postconstruct -> runs when the context is ready
    * */

    public void initializePrices()
    {
        log.info("Loading stock data....");
        List<Stock> stocks = stockRepository.findByActiveTrue();

        if(stocks.isEmpty())
        {
            log.warn("No active stocks found");
            return;
        }

        stocks.forEach(stock ->{
            StockPrice stockPrice = new StockPrice();
            stockPrice.setSymbol(stock.getSymbol());
            stockPrice.setCompanyName(stock.getCompanyName());
            stockPrice.setPrice(stock.getInitialPrice());
            stockPrice.setOpen(stock.getInitialPrice());
            stockPrice.setHigh(stock.getInitialPrice());
            stockPrice.setLow(stock.getInitialPrice());
            stockPrice.setChange(BigDecimal.ZERO);
            stockPrice.setChangePercent(BigDecimal.ZERO);
            stockPrice.setVolume(0L);
            stockPrice.setTimestamp(LocalDateTime.now());
            stockPrices.put(stock.getSymbol(), stockPrice);

            log.info("Loaded stock {}",stock.getSymbol(), stock.getInitialPrice());
        });

        log.info("Initializes {} stocks",stockPrices.size());
    }

    /*
    * Scheduled price update*/
}
