package com.trading.order_service.service;

import com.trading.order_service.client.MarketDataClient;
import com.trading.order_service.client.UserServiceClient;
import com.trading.order_service.dto.OrderRequest;
import com.trading.order_service.entity.Order;
import com.trading.order_service.entity.OrderStatus;
import com.trading.order_service.entity.OrderType;
import com.trading.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService
{
    private OrderRepository orderRepository;
    private final MarketDataClient marketDataClient;
    private  final UserServiceClient userServiceClient;
    private static final String ORDER_EXECUTED_TOPIC = "order-executed";
    private static final String ORDER_FAILED_TOPIC = "order-failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    /*

        Place buy or sell the order
        FLOW:
        1. Get Current price from market
        2. Calculate Total amount
        3. Execute
        4. Publish Order event to kafka
     */

    public Order placeOrder(OrderRequest orderRequest)
    {
        log.info("Placing {} order :{} x {} for user: {}",
                orderRequest.getType(),orderRequest.getQuantity(),
                orderRequest.getSymbol(),orderRequest.getUserId());
        // Step: 1
        Map<String , Object> priceData = marketDataClient.getStockPrice(orderRequest.getSymbol());
        BigDecimal currentPrice = new BigDecimal(
                priceData.get("price").toString());

        BigDecimal totalAmount = currentPrice.multiply(
                BigDecimal.valueOf(orderRequest.getQuantity()));

                // create order

        Order order = new Order();
        order.setUserId(orderRequest.getUserId());
        order.setSymbol(orderRequest.getSymbol());
        order.setType(orderRequest.getType());
        order.setQuantity(orderRequest.getQuantity());
        order.setPrice(currentPrice);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);
        log.info("Order created :{}",savedOrder.getId());

        try
        {
            if(orderRequest.getType() == OrderType.BUY)
            {
                userServiceClient.deductFunds(
                        orderRequest.getUserId(),totalAmount
                );
            }
            else
            {
                userServiceClient.creditFunds(
                        orderRequest.getUserId(),totalAmount
                );
            }
            savedOrder.setStatus(OrderStatus.EXECUTED);
            savedOrder.setExecutedAt(LocalDateTime.now());
            orderRepository.save(savedOrder);

            log.info("Order executed :{} {} x {} at {}", orderRequest.getType(),orderRequest.getQuantity(), orderRequest.getSymbol(), currentPrice);

            publishOrderEvent(ORDER_EXECUTED_TOPIC, savedOrder, null);
        }
        catch (Exception e)
        {
            savedOrder.setStatus(OrderStatus.FAILED);
            savedOrder.setFailureReason(e.getMessage());
            orderRepository.save(savedOrder);

            log.error("Order Failed : {} reason: {}",savedOrder.getId(),e.getMessage());
        }

        return savedOrder;
    }

    public Order getOrder(String orderId)
    {
        return orderRepository.findById(orderId)
                .orElseThrow(()-> new RuntimeException("Order not found :"+orderId));
    }

    public List<Order> getUserOrders(String userId)
    {
        return orderRepository.findByUserIdCreatedAtDesc(userId);
    }

    private void publishOrderEvent(String topic, Order order, String reason)
    {
        Map<String, Object> orderEvent = new HashMap<>();
        orderEvent.put("orderId", order.getId());
        orderEvent.put("userId", order.getUserId());
        orderEvent.put("symbol", order.getSymbol());
        orderEvent.put("type", order.getType());
        orderEvent.put("quantity", order.getQuantity());
        orderEvent.put("price", order.getPrice());
        orderEvent.put("totalAmount", order.getTotalAmount());
        orderEvent.put("status", order.getStatus());

        if(reason != null)orderEvent.put("failureReason", reason);

        kafkaTemplate.send(topic, order.getId(),orderEvent);

        log.info("Order event published :{} for order: {}",topic,order.getId());



    }


}
