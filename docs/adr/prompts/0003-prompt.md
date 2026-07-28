Generate an Architecture Decision Record for ReconX.

Decision:
Use PostgreSQL GIN indexes for JSONB metadata.

Use Michael Nygard ADR format.

Include:
- Title
- Status
- Context
- Decision
- Consequences

Mention:
- JSONB search performance
- Alternatives (No index, B-tree, GIN)
- Expected workload
- Trade-offs

Return Markdown only.