package com.trading.marketdataservice.service;

import com.trading.marketdataservice.dto.StockPrice;
import com.trading.marketdataservice.entity.Stock;
import com.trading.marketdataservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataService
{
    private final StockRepository stockRepository;
    private final Random random= new Random();
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PRICE_KEY_PREFIX = "stock:price";
    private static  final String PRICE_UPDATED_TOPIC = "stock.price.updated";


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
    * Scheduled price update in  every two seconds
    * */
    @Scheduled(fixedRateString =
            "${market.price-update-interval}")
    public void updatePrices()
    {
        if(stockPrices.isEmpty())
        {
            log.warn("No stocks loaded");
        }

        stockPrices.forEach((symbol,currentPrice)->
        {
            BigDecimal oldPrice = currentPrice.getPrice();

            //+- 0.5 price movement
            double priceMovement = (random.nextDouble());

            BigDecimal priceChange = oldPrice
                    .multiply(BigDecimal.valueOf( priceMovement/ 100))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal newPrice = oldPrice
                    .add(priceChange)
                    .setScale(2, RoundingMode.HALF_UP);

            // prevent negative price
            if(newPrice.compareTo(BigDecimal.valueOf(1)) < 0)
            {
                newPrice = BigDecimal.valueOf(1);
            }

            BigDecimal changeAmount = newPrice
                    .subtract(currentPrice.getOpen())
                    .setScale(1, RoundingMode.HALF_UP);

            BigDecimal changePercent = changeAmount
                    .divide(currentPrice.getOpen(),
                            RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            // update high /low
            BigDecimal newHigh = newPrice
                    .compareTo(currentPrice.getHigh())>0
                    ? newPrice : currentPrice.getHigh();

            BigDecimal newLow = newPrice
                    .compareTo(currentPrice.getLow())<0
                    ? newPrice : currentPrice.getLow();

            currentPrice.setPrice(newPrice);
            currentPrice.setChange(changeAmount);
            currentPrice.setChangePercent(changePercent);
            currentPrice.setHigh(newHigh);
            currentPrice.setLow(newLow);
            currentPrice.setTimestamp(LocalDateTime.now());

            // 1. Cache in Redis

            redisTemplate.opsForValue().set(
                    PRICE_KEY_PREFIX + symbol, currentPrice);

            // 2. BroadCast via websocket -> All Clients

            simpMessagingTemplate.convertAndSend(
                    "/topic/prices" + symbol, currentPrice);

            // 3. Publish to kafka
            Map<String, Object> priceUpdatedEvent = new HashMap<>();
            priceUpdatedEvent.put("symbol", symbol);
            priceUpdatedEvent.put("price", newPrice);
            priceUpdatedEvent.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(PRICE_UPDATED_TOPIC, symbol, priceUpdatedEvent);

        });

        log.debug("prices updated for {}",stockPrices.size());
    }

    /*Get current price from Redis

     */
    public StockPrice getPrice(String symbol)
    {
        Object cached = redisTemplate.opsForValue().get(
                PRICE_KEY_PREFIX + symbol);
        if(cached != null)
        {
            return (StockPrice) cached;
        }

        // FallBack

        StockPrice stockPrice = stockPrices.get(symbol.toUpperCase());

        if(stockPrice == null)
        {
            throw new RuntimeException("Stock not found for symbol " + symbol);
        }

        return stockPrice;

    }

    /*
    Get all active stock prices
    @return
     */

    public List<StockPrice> getAllPrices()
    {
        return stockPrices.values().stream().toList();
    }
    /*
    Get all stocks
     */
    public List<Stock> getAllStocks()
    {
        return stockRepository.findByActiveTrue();
    }

}
