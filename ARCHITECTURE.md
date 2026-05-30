# Architecture: Limit Order Book

## Overview
Ultra-low-latency order matching engine implementing **price-time priority** with lock-free data structures.

## Core Data Structures

```mermaid
sequenceDiagram
    participant Client
    participant OrderBook
    participant Bids as Bids (SkipList: Price DESC, Time ASC)
    participant Asks as Asks (SkipList: Price ASC, Time ASC)

    Client->>OrderBook: processOrder(Order)
    OrderBook->>OrderBook: Route to contra book
    loop While price matches and qty > 0
        OrderBook->>Contra Book: Poll best price/time
        OrderBook->>Client: Emit Trade
    end
    alt Remaining quantity
        OrderBook->>Own Book: Insert resting order
    end
