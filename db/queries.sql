-- ============================================================================
-- TICKET-ADV010 — VWAP per instrument per day (window function)
-- ============================================================================
SELECT DISTINCT
    t.instrument_id,
    t.trade_date,
    SUM(t.price * t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date)
        / NULLIF(SUM(t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date), 0)
            AS vwap
FROM trades t
WHERE t.deleted_at IS NULL
  AND t.asset_class = 'EQUITY'
ORDER BY t.trade_date DESC, t.instrument_id;


-- ============================================================================
-- TICKET-ADV011 — Recursive CTE: trade lifecycle (execution -> settlement
--                -> recon_break -> resolution)
-- ============================================================================
-- ============================================================================
-- TICKET-ADV011 — Recursive CTE: trade lifecycle rollup
-- EXECUTION → CONFIRMATION → SETTLEMENT → RECON_BREAK → RESOLUTION
-- ============================================================================

WITH RECURSIVE trade_lifecycle AS (

    -- ------------------------------------------------------------------------
    -- Base case: every trade starts at execution stage
    -- ------------------------------------------------------------------------
    SELECT
        t.id AS trade_id,
        1 AS stage,
        'EXECUTION' AS stage_name,
        t.created_at AS event_at,
        t.status AS event_status
    FROM trades t
    WHERE t.deleted_at IS NULL


    UNION ALL


    -- ------------------------------------------------------------------------
    -- Recursive case: move trade to next lifecycle stage
    -- ------------------------------------------------------------------------
    SELECT
        tl.trade_id,
        tl.stage + 1 AS stage,

        CASE tl.stage
            WHEN 1 THEN 'CONFIRMATION'
            WHEN 2 THEN 'SETTLEMENT'
            WHEN 3 THEN 'RECON_BREAK'
            WHEN 4 THEN 'RESOLUTION'
        END AS stage_name,

        next_event.event_at,
        next_event.event_status

    FROM trade_lifecycle tl

    JOIN LATERAL (

        -- Stage 1 → 2 : Confirmation
        SELECT
            modified_at AS event_at,
            status AS event_status
        FROM trades
        WHERE id = tl.trade_id
          AND tl.stage = 1


        UNION ALL


        -- Stage 2 → 3 : Settlement
        SELECT
            settlement_date::timestamp AS event_at,
            status AS event_status
        FROM settlements
        WHERE trade_id = tl.trade_id
          AND tl.stage = 2


        UNION ALL


        -- Stage 3 → 4 : Recon break
        SELECT
            detected_at AS event_at,
            status AS event_status
        FROM recon_breaks
        WHERE trade_id = tl.trade_id
          AND tl.stage = 3


        UNION ALL


        -- Stage 4 → 5 : Resolution
        SELECT
            resolved_at AS event_at,
            'RESOLVED' AS event_status
        FROM recon_breaks
        WHERE trade_id = tl.trade_id
          AND tl.stage = 4
          AND resolved_at IS NOT NULL

    ) next_event ON TRUE

    -- termination condition
    WHERE tl.stage < 5
      AND next_event.event_at IS NOT NULL
)

SELECT
    trade_id,
    stage,
    stage_name,
    event_at,
    event_status
FROM trade_lifecycle
ORDER BY trade_id, stage;


-- ============================================================================
-- ADV008 — REFRESH the daily-summary materialised view (concurrent so it can
--         run while the dashboard is reading it)
-- ============================================================================
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_recon_summary;


-- ============================================================================
-- ADV009 — JSONB lookup: which instruments have sector = 'Banking'?
-- ============================================================================
SELECT id, symbol, metadata
FROM instruments
WHERE metadata @> '{"sector":"Banking"}'::jsonb;
