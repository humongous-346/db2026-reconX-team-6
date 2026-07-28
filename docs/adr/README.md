# Architecture Decision Record (ADR) Prompt Template

Use the following prompt whenever creating a new ADR for ReconX.

---

Generate an Architecture Decision Record (ADR) using the Michael Nygard format.

Project: ReconX

The ADR must contain the following sections:

- Title
- Status
- Context
- Decision
- Consequences

Requirements:

- Make the ADR specific to ReconX.
- Mention the expected workload (approximately 50,000 trades per day and nearly 91 million rows over five years where applicable).
- Describe the constraints that influenced the decision.
- Mention at least two alternatives that were considered.
- Explain why the selected option was chosen.
- Explain both positive and negative consequences.
- Status should be "Accepted".

Return the ADR in Markdown.