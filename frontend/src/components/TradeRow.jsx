import React from 'react';

function TradeRowImpl({ trade, onClick }) {
  return (
    <div className="trade-row" role="row">
      <span>
        <button type="button" onClick={() => onClick(trade.id)}>
          {trade.tradeRef}
        </button>
      </span>
      <span>{trade.instrument}</span>
      <span>{trade.quantity}</span>
      <span>{trade.price}</span>
      <span><span className={`status-pill ${String(trade.status).toLowerCase()}`}>{trade.status}</span></span>
    </div>
  );
}

function areEqual(prev, next) {
  return prev.trade.id === next.trade.id
    && prev.trade.status === next.trade.status
    && prev.trade.price === next.trade.price
    && prev.onClick === next.onClick;
}

export const TradeRow = React.memo(TradeRowImpl, areEqual);
