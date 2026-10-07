package com.trading.order_service.repository;

import com.trading.order_service.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String>
{
    List<Order> findByUserIdCreatedAtDesc(String userId);
}
