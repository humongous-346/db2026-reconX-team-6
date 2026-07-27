```mermaid
C4Context
  title C4 Context - ReconX Enterprise Trade Reconciliation Platform

  %% Personas
  Person(trader, "Trader", "Books and amends trades; investigates breaks.")
  Person(recon_analyst, "Recon Analyst", "Resolves daily reconciliation breaks.")
  Person(ops_admin, "Ops Admin", "Manages users, audits activity.")
  Person(compliance, "Compliance Officer", "Reads audit log + reports only.")

  %% Main System
  System(reconx, "ReconX", "Internal trade reconciliation platform. Auto-matches internal vs external trade records, surfaces breaks, tracks resolution SLAs.")

  %% External Systems
  System_Ext(oms, "Internal OMS", "Source of internal trade records (intra-day Kafka feed).")
  System_Ext(counterparty, "Counterparty Trade Files", "EOD CSV feeds from custodian/counterparties via SFTP.")
  System_Ext(bloomberg, "Bloomberg Pricing", "Reference market data for break investigation.")
  System_Ext(email, "Corporate Email Gateway", "Sends break-resolution notifications to Ops.")
  System_Ext(sso, "Corporate SSO (Entra ID)", "Issues JWT after OIDC login.")
  System_Ext(grafana, "Grafana / Prometheus", "Scrapes metrics for SRE dashboards and alerts.")

  %% Relationships (Person to ReconX)
  Rel(trader, reconx, "Books trades, views breaks", "HTTPS")
  Rel(recon_analyst, reconx, "Resolves breaks", "HTTPS")
  Rel(ops_admin, reconx, "User admin, audit", "HTTPS")
  Rel(compliance, reconx, "Reads audit log + reports", "HTTPS, read-only")

  %% Relationships (External Systems to/from ReconX)
  Rel(oms, reconx, "Streams trade events", "Kafka topic: trade-events")
  Rel(counterparty, reconx, "Drops EOD trade CSVs", "SFTP poll, 5-min interval")
  Rel(reconx, bloomberg, "Fetches reference prices", "HTTPS, REST")
  Rel(reconx, email, "Sends break notifications", "SMTP")
  Rel(reconx, sso, "Validates user", "OIDC, HTTPS")
  Rel(grafana, reconx, "Scrapes /actuator/prometheus", "HTTPS")