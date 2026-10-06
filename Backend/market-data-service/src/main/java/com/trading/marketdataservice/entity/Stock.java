package com.trading.marketdataservice.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="Stocks")
public class Stock
{
    @Id
    @Column(nullable = false, unique = true)
    private String symbol;
    @Column(nullable = false)
    private String companyName;
    @Column(nullable = false,precision=15,scale=2)
    private BigDecimal initialPrice;
    @Column(nullable = false)
    private String exchange;   // NSE, NASDAQ
    @Column(nullable = false)
    private String currency;   // INR, USD, GBP
    private boolean active = true;

}
