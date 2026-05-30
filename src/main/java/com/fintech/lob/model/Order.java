package com.fintech.lob.model;

import java.util.Objects;
import java.util.UUID;

/**
 * An immutable order within the limit order book.
 * Uses synthetic identifiers consistent with FIX protocol conventions.
 */
public final class Order {
    public enum Side { BUY, SELL }

    private final UUID orderId;        // Unique order identifier
    private final String symbol;       // e.g., "AAPL.OQ" (Reuters Instrument Code style)
    private final Side side;
    private final long quantity;       // Using long to avoid floating-point
    private final long price;          // Price in micro-dollars (cents × 10,000)
    private final long timestamp;      // Entry time (nanoseconds from epoch)

    public Order(UUID orderId, String symbol, Side side, long quantity, long price, long timestamp) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        if (price <= 0) throw new IllegalArgumentException("Price must be positive: " + price);
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.symbol = Objects.requireNonNull(symbol, "symbol");
        this.side = Objects.requireNonNull(side, "side");
        this.quantity = quantity;
        this.price = price;
        this.timestamp = timestamp;
    }

    // Getters (no setters — immutability is key for correctness)
    public UUID orderId() { return orderId; }
    public String symbol() { return symbol; }
    public Side side() { return side; }
    public long quantity() { return quantity; }
    public long price() { return price; }
    public long timestamp() { return timestamp; }

    public Order withReducedQuantity(long newQuantity) {
        return new Order(orderId, symbol, side, newQuantity, price, timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;
        return orderId.equals(order.orderId);
    }

    @Override
    public int hashCode() {
        return orderId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Order[id=%s, %s %d @ %d.%04d, qty=%d]",
            orderId.toString().substring(0, 8),
            side, price / 10_000, price % 10_000, quantity);
    }
}
