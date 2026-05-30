package com.fintech.lob.engine;

import com.fintech.lob.model.Order;
import com.fintech.lob.model.Trade;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Price-time priority order book for a single symbol.
 * Lock-free reads via ConcurrentSkipListMap.
 * Matching logic always favors price, then timestamp.
 */
public class OrderBook {
    private final String symbol;
    private final NavigableMap<Long, NavigableMap<Long, Queue<Order>>> bids; // Price -> Timestamp -> Orders
    private final NavigableMap<Long, NavigableMap<Long, Queue<Order>>> asks; // Price -> Timestamp -> Orders
    private final AtomicLong sequenceNumber = new AtomicLong(0);

    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.bids = new ConcurrentSkipListMap<>(Comparator.reverseOrder()); // Highest price first
        this.asks = new ConcurrentSkipListMap<>(); // Lowest price first
    }

    /**
     * Adds an order and attempts immediate match.
     * @return list of trades that resulted from matching.
     */
    public List<Trade> processOrder(Order order) {
        Objects.requireNonNull(order, "order");
        if (!order.symbol().equals(symbol)) {
            throw new IllegalArgumentException("Symbol mismatch");
        }

        if (order.side() == Order.Side.BUY) {
            return matchOrder(order, asks, bids, true);
        } else {
            return matchOrder(order, bids, asks, false);
        }
    }

    private List<Trade> matchOrder(Order order,
                                   NavigableMap<Long, NavigableMap<Long, Queue<Order>>> contraBook,
                                   NavigableMap<Long, NavigableMap<Long, Queue<Order>>> ownBook,
                                   boolean isBuy) {
        List<Trade> trades = new ArrayList<>();
        Order remaining = order;

        Iterator<Map.Entry<Long, NavigableMap<Long, Queue<Order>>>> priceIter = contraBook.entrySet().iterator();

        while (priceIter.hasNext() && remaining.quantity() > 0) {
            Map.Entry<Long, NavigableMap<Long, Queue<Order>>> priceEntry = priceIter.next();
            long contraPrice = priceEntry.getKey();

            // Price check
            if (isBuy ? contraPrice > remaining.price() : contraPrice < remaining.price()) {
                break; // No more matching prices
            }

            NavigableMap<Long, Queue<Order>> timeBuckets = priceEntry.getValue();
            Iterator<Map.Entry<Long, Queue<Order>>> timeIter = timeBuckets.entrySet().iterator();

            while (timeIter.hasNext() && remaining.quantity() > 0) {
                Queue<Order> orderQueue = timeIter.next().getValue();

                while (!orderQueue.isEmpty() && remaining.quantity() > 0) {
                    Order matchedOrder = orderQueue.peek();
                    long tradeQty = Math.min(remaining.quantity(), matchedOrder.quantity());
                    long executionTimestamp = System.nanoTime();

                    trades.add(new Trade(
                        isBuy ? remaining.orderId() : matchedOrder.orderId(),
                        isBuy ? matchedOrder.orderId() : remaining.orderId(),
                        symbol,
                        matchedOrder.price(), // Match price is the resting order's price
                        tradeQty,
                        executionTimestamp
                    ));

                    remaining = remaining.withReducedQuantity(remaining.quantity() - tradeQty);
                    Order updatedMatch = matchedOrder.withReducedQuantity(matchedOrder.quantity() - tradeQty);
                    orderQueue.poll(); // Remove old

                    if (updatedMatch.quantity() > 0) {
                        orderQueue.add(updatedMatch); // Re-add remainder (preserves FIFO within timestamp)
                    }
                }

                if (orderQueue.isEmpty()) {
                    timeIter.remove();
                }
            }

            if (timeBuckets.isEmpty()) {
                priceIter.remove();
            }
        }

        // Add any remaining quantity as a new resting order
        if (remaining.quantity() > 0) {
            NavigableMap<Long, Queue<Order>> book = ownBook.computeIfAbsent(
                remaining.price(),
                k -> new ConcurrentSkipListMap<>()
            );
            Queue<Order> ordersAtTime = book.computeIfAbsent(
                remaining.timestamp(),
                k -> new ArrayDeque<>()
            );
            ordersAtTime.add(remaining);
        }

        return trades;
    }

    // For testing and monitoring
    public int getBidLevels() { return bids.size(); }
    public int getAskLevels() { return asks.size(); }
    public String getSymbol() { return symbol; }
}
