# ADR 0001: Partition Trades by trade_date

## Status

Accepted

## Context

ReconX stores trade execution data received from multiple exchanges.
The system is expected to process approximately 50,000 trades every day.
This results in nearly 18 million records every year and around 91 million records after five years.

Most application queries retrieve trades for a particular trading day or a limited date range.
Data retention and archival are also performed based on trading dates.

Without partitioning, the trades table would continue growing, leading to slower queries, larger indexes, and longer maintenance operations.

Alternatives considered:

- Single large table with indexes
- Partition by exchange_id
- Partition by trade_date

## Decision

Partition the trades table using PostgreSQL range partitioning on the trade_date column.

Each trading period will be stored in a separate partition.
Queries filtering by trade_date will benefit from partition pruning, allowing PostgreSQL to scan only relevant partitions.

## Consequences

### Positive

- Faster date-based queries through partition pruning.
- Easier archival and deletion of historical data.
- Smaller indexes on each partition.
- Improved maintenance operations such as VACUUM and ANALYZE.

### Negative

- Additional operational overhead when creating future partitions.
- Slightly more complex schema management.
- Queries without trade_date filters may need to access multiple partitions.