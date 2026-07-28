# ADR 0003: Use GIN Index for JSONB Metadata

## Status

Accepted

## Context

ReconX stores exchange-specific attributes inside the JSONB metadata column.

Typical application queries include searching by:

- instrument_type
- option_type
- expiry_date
- settlement_type

Standard B-tree indexes cannot efficiently index arbitrary JSONB documents.

Alternatives considered:

- No index
- B-tree index
- GIN index

## Decision

Create a PostgreSQL Generalized Inverted Index (GIN) on the JSONB metadata column.

GIN indexes are optimized for containment and key lookup operations on JSONB documents.

## Consequences

### Positive

- Faster JSONB search operations.
- Efficient containment queries.
- Better performance for filtering exchange-specific metadata.
- Scales well as metadata grows.

### Negative

- Larger index size than B-tree.
- Higher storage requirements.
- Slightly slower INSERT and UPDATE operations because the GIN index must also be maintained.