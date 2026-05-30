package com.fintech.lob.model;

public record Trade(
    UUID buyOrderId,
    UUID sellOrderId,
    String symbol,
    long price,        // Match price in micro-dollars
    long quantity,
    long timestamp     // Execution timestamp
) {
    public Trade {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (price <= 0) throw new IllegalArgumentException("Price must be positive");
    }
}
