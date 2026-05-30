package com.fintech.lob.bench;

import com.fintech.lob.engine.OrderBook;
import com.fintech.lob.model.Order;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class OrderBookBenchmark {
    private static final String[] SYMBOLS = {"EUR/USD", "AAPL.OQ", "BTC-USD"};
    private static final int WARMUP_ITER = 50_000;
    private static final int MEASURE_ITER = 500_000;

    public static void main(String[] args) {
        System.out.println("Warming up...");
        for (int i = 0; i < WARMUP_ITER; i++) runSingleIteration();

        System.out.println("Measuring...");
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_ITER; i++) runSingleIteration();
        long duration = System.nanoTime() - start;

        double avgNs = (double) duration / MEASURE_ITER;
        System.out.printf("Avg latency per operation: %.1f ns%n", avgNs);
        System.out.printf("Throughput: %.0f ops/sec%n", 1_000_000_000.0 / avgNs);
    }

    private static void runSingleIteration() {
        OrderBook book = new OrderBook(SYMBOLS[ThreadLocalRandom.current().nextInt(SYMBOLS.length)]);
        for (int i = 0; i < 100; i++) {
            boolean isBuy = ThreadLocalRandom.current().nextBoolean();
            Order order = new Order(
                UUID.randomUUID(),
                book.getSymbol(),
                isBuy ? Order.Side.BUY : Order.Side.SELL,
                ThreadLocalRandom.current().nextLong(1, 1000),
                ThreadLocalRandom.current().nextLong(99_00_0000L, 101_00_0000L),
                System.nanoTime()
            );
            book.processOrder(order);
        }
    }
}
