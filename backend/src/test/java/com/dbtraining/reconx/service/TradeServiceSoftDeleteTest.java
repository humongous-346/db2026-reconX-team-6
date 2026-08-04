package com.dbtraining.reconx.service;

import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceSoftDeleteTest {

    @Mock private TradeRepository tradeRepo;
    @Mock private CounterpartyRepository counterpartyRepo;
    @Mock private InstrumentRepository instrumentRepo;
    @Mock private TradeEventProducer events;
    @Mock private TradeMetrics metrics;

    @InjectMocks private TradeService service;

    @Test
    void softDelete_setsDeletedAtAndSaves() {
        Trade trade = new Trade();
        when(tradeRepo.findById(15L)).thenReturn(Optional.of(trade));

        service.softDelete(15L, "admin@db.com");

        assertThat(trade.getDeletedAt()).isNotNull();
        verify(tradeRepo).save(trade);
    }

    @Test
    void softDelete_throwsWhenTradeIsMissing() {
        when(tradeRepo.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDelete(404L, "admin@db.com"))
                .isInstanceOf(TradeNotFoundException.class)
                .hasMessageContaining("id 404");
    }
}
