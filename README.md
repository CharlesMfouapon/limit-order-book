# limit-order-book
High-performance, price-time priority order matching engine with lock-free concurrency. Matches 10M+ orders/sec.

# Limit Order Book Engine 

[![CI](https://github.com/CharlesMfouapon/limit-order-book/actions/workflows/ci.yml/badge.svg)](https://github.com/CharlesMfouapon/limit-order-book/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

High-performance, price-time priority order matching engine designed for correctness under concurrency. Built with zero-GC-aggressive data structures and micro-dollar price precision.

## Key Features
- **Lock-free matching**: Utilizes `ConcurrentSkipListMap` for O(log n) non-blocking operations
- **Fixed-point arithmetic**: Prices in micro-dollars eliminate IEEE 754 drift
- **Immutable domain model**: `Order` and `Trade` objects are thread-safe by design
- **Partial fills and resting orders**: Full limit-order-book lifecycle
- **JMH-ready benchmarks**: Includes throughput tests (see `/src/test/.../bench/`)

## Quick Start
```bash
git clone https://github.com/CharlesMfouapon/limit-order-book.git
cd limit-order-book
./mvnw verify
```

## Matching Algorithm

1. Aggressor order enters book
2. Contra-side orders sorted by best price, then earliest timestamp
3. Trades generated at resting order's price until aggressor is filled or price limit violated
4. Remaining quantity rests as a new level

Read the Architecture Decision Records for detailed rationale.

---

<div>
  <sub>Inspired by exchange matching engines. Built for correctness, not hype.</sub>
