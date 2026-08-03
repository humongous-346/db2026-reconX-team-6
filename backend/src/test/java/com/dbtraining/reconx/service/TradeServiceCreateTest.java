package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.exception.DuplicateTradeRefException;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceCreateTest {

    @Mock private TradeRepository tradeRepo;
    @Mock private CounterpartyRepository counterpartyRepo;
    @Mock private InstrumentRepository instrumentRepo;
    @Mock private TradeEventProducer events;
    @Mock private TradeMetrics metrics;

    @InjectMocks private TradeService service;

    @Test
    void create_persistsPendingTradeWithReferencedEntities() {
        TradeRequest request = validRequest();
        Instrument instrument = new Instrument();
        Counterparty counterparty = new Counterparty();
        when(tradeRepo.findByTradeRef(request.tradeRef())).thenReturn(Optional.empty());
        when(instrumentRepo.findById(request.instrumentId())).thenReturn(Optional.of(instrument));
        when(counterpartyRepo.findById(request.counterpartyId())).thenReturn(Optional.of(counterparty));
        when(tradeRepo.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trade saved = service.create(request, "trader@reconx.local");

        ArgumentCaptor<Trade> trade = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepo).save(trade.capture());
        assertThat(saved).isSameAs(trade.getValue());
        assertThat(saved.getTradeRef()).isEqualTo(request.tradeRef());
        assertThat(saved.getInstrument()).isSameAs(instrument);
        assertThat(saved.getCounterparty()).isSameAs(counterparty);
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getQuantity()).isEqualByComparingTo("100.00");
        assertThat(saved.getPrice()).isEqualByComparingTo("245.50");
    }

    @Test
    void create_rejectsDuplicateReferenceBeforeRepositoryLookups() {
        TradeRequest request = validRequest();
        when(tradeRepo.findByTradeRef(request.tradeRef())).thenReturn(Optional.of(new Trade()));

        assertThatThrownBy(() -> service.create(request, "trader@reconx.local"))
                .isInstanceOf(DuplicateTradeRefException.class)
                .hasMessageContaining(request.tradeRef());

        verifyNoInteractions(instrumentRepo, counterpartyRepo, events, metrics);
    }

    @Test
    void create_rejectsMissingInstrument() {
        TradeRequest request = validRequest();
        when(tradeRepo.findByTradeRef(request.tradeRef())).thenReturn(Optional.empty());
        when(instrumentRepo.findById(request.instrumentId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request, "trader@reconx.local"))
                .isInstanceOf(TradeNotFoundException.class)
                .hasMessageContaining("instrument id " + request.instrumentId());
    }

    @Test
    void update_overwritesTradeFields() {
        Trade existing = new Trade();
        existing.setTradeRef("TRD-20260315-0001");

        TradeRequest request = new TradeRequest("TRD-20260316-0002", 11L, 21L, "FX", "SELL",
                new BigDecimal("300.00"), new BigDecimal("1.12"), LocalDate.of(2026, 3, 16));

        Instrument instrument = new Instrument();
        Counterparty counterparty = new Counterparty();

        when(tradeRepo.findByTradeRef(request.tradeRef())).thenReturn(Optional.empty());
        when(tradeRepo.findById(99L)).thenReturn(Optional.of(existing));
        when(instrumentRepo.findById(request.instrumentId())).thenReturn(Optional.of(instrument));
        when(counterpartyRepo.findById(request.counterpartyId())).thenReturn(Optional.of(counterparty));
        when(tradeRepo.save(existing)).thenReturn(existing);

        Trade updated = service.update(99L, request, "trader@reconx.local");

        assertThat(updated.getTradeRef()).isEqualTo(request.tradeRef());
        assertThat(updated.getInstrument()).isSameAs(instrument);
        assertThat(updated.getCounterparty()).isSameAs(counterparty);
        assertThat(updated.getAssetClass()).isEqualTo(request.assetClass());
        assertThat(updated.getSide()).isEqualTo(request.side());
        assertThat(updated.getQuantity()).isEqualByComparingTo("300.00");
        assertThat(updated.getPrice()).isEqualByComparingTo("1.12");
        assertThat(updated.getTradeDate()).isEqualTo(LocalDate.of(2026, 3, 16));
    }

    @Test
    void update_rejectsMissingTradeId() {
        TradeRequest request = validRequest();
        when(tradeRepo.findByTradeRef(request.tradeRef())).thenReturn(Optional.empty());
        when(tradeRepo.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(404L, request, "trader@reconx.local"))
                .isInstanceOf(TradeNotFoundException.class)
                .hasMessageContaining("id 404");
    }

    private TradeRequest validRequest() {
        return new TradeRequest("TRD-20260315-0001", 10L, 20L, "EQUITY", "BUY",
                new BigDecimal("100.00"), new BigDecimal("245.50"), LocalDate.of(2026, 3, 15));
    }
}
