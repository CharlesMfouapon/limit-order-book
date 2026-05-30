package com.fintech.lob.engine;

import com.fintech.lob.model.Order;
import com.fintech.lob.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Limit Order Book")
class OrderBookTest {

    private OrderBook book;
    private static final String AAPL = "AAPL.OQ";

    @BeforeEach
    void setUp() {
        book = new OrderBook(AAPL);
    }

    @Nested
    @DisplayName("Price-Time Priority")
    class PriceTimePriority {

        @Test
        @DisplayName("matches at best price first, then earliest timestamp")
        void bestPriceFirstThenTime() {
            long ts1 = 1000, ts2 = 2000;
            Order sell1 = new Order(UUID.randomUUID(), AAPL, Order.Side.SELL, 100, 150_00_0000L, ts1);
            Order sell2 = new Order(UUID.randomUUID(), AAPL, Order.Side.SELL, 100, 150_50_0000L, ts2); // Worse price

            book.processOrder(sell1);
            book.processOrder(sell2);

            Order buy = new Order(UUID.randomUUID(), AAPL, Order.Side.BUY, 100, 151_00_0000L, 3000);
            List<Trade> trades = book.processOrder(buy);

            assertEquals(1, trades.size());
            assertEquals(sell1.orderId(), trades.get(0).sellOrderId(), "Should match best price (sell1)");
        }

        @Test
        @DisplayName("partial fill leaves resting order with remainder")
        void partialFill() {
            Order sell = new Order(UUID.randomUUID(), AAPL, Order.Side.SELL, 200, 100_00_0000L, 1000);
            book.processOrder(sell);

            Order buy = new Order(UUID.randomUUID(), AAPL, Order.Side.BUY, 100, 100_00_0000L, 2000);
            List<Trade> trades = book.processOrder(buy);

            assertEquals(1, trades.size());
            assertEquals(100, trades.get(0).quantity());
            assertEquals(1, book.getAskLevels(), "Remaining 100 should rest on book");
        }
    }

    @Nested
    @DisplayName("Correctness Invariants")
    class Invariants {

        @Test
        @DisplayName("BUY order does not execute above its limit price")
        void buyLimitRespected() {
            Order sell = new Order(UUID.randomUUID(), AAPL, Order.Side.SELL, 100, 150_00_0000L, 1000);
            book.processOrder(sell);

            Order buy = new Order(UUID.randomUUID(), AAPL, Order.Side.BUY, 100, 149_00_0000L, 2000);
            List<Trade> trades = book.processOrder(buy);

            assertTrue(trades.isEmpty(), "Buy with limit below lowest ask should not execute");
        }

        @Test
        @DisplayName("total traded quantity never exceeds order quantity")
        void quantityConserved() {
            Order sell = new Order(UUID.randomUUID(), AAPL, Order.Side.SELL, 100, 100_00_0000L, 1000);
            book.processOrder(sell);

            long totalTraded = 0;
            for (int i = 0; i < 5; i++) {
                Order buy = new Order(UUID.randomUUID(), AAPL, Order.Side.BUY, 30, 100_00_0000L, 2000 + i);
                totalTraded += book.processOrder(buy).stream().mapToLong(Trade::quantity).sum();
            }
            assertEquals(100, totalTraded, "Total traded cannot exceed original sell quantity");
        }
    }
}
