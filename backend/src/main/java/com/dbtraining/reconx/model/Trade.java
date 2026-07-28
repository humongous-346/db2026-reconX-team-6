package com.dbtraining.reconx.model;

import java.time.LocalDate;
import java.util.Objects;

abstract sealed class Trade
        implements TradeType
        permits EquityTrade, FXTrade, BondTrade, DerivativeTrade {

    private final TradeRef tradeRef;
    private final Money notional;
    private final LocalDate tradeDate;

    protected Trade(
            TradeRef tradeRef,
            Money notional,
            LocalDate tradeDate) {

        this.tradeRef = Objects.requireNonNull(tradeRef, "tradeRef cannot be null");
        this.notional = Objects.requireNonNull(notional, "notional cannot be null");
        this.tradeDate = Objects.requireNonNull(tradeDate, "tradeDate cannot be null");
    }

    @Override
    public TradeRef tradeRef() {
        return tradeRef;
    }

    @Override
    public Money notional() {
        return notional;
    }

    @Override
    public LocalDate tradeDate() {
        return tradeDate;
    }
}