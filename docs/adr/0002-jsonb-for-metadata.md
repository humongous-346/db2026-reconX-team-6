# ADR 0002: Store Flexible Instrument Metadata using JSONB

## Status

Accepted

## Context

ReconX integrates data from multiple exchanges and financial instruments.

Different exchanges expose different attributes such as:

- option_type
- strike_price
- expiry_date
- settlement_type
- trading_session

Adding a relational column for every possible attribute would require frequent schema migrations and introduce many nullable columns.

Alternatives considered:

- Fully normalized relational tables
- Plain JSON
- PostgreSQL JSONB

## Decision

Store flexible instrument metadata using PostgreSQL JSONB.

Frequently queried business fields remain normal relational columns, while exchange-specific attributes are stored inside the JSONB metadata column.

## Consequences

### Positive

- Flexible schema for new exchanges.
- Fewer database migrations.
- Reduced number of nullable columns.
- Efficient storage of semi-structured data.
- Native PostgreSQL JSON operators can be used.

### Negative

- JSON schema is not strictly enforced by PostgreSQL.
- Developers must understand JSONB operators.
- Some analytical queries become more complex compared to relational columns.