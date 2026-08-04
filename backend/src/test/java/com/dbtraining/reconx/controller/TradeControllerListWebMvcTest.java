package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.dto.TradeResponse;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.security.JwtTokenProvider;
import com.dbtraining.reconx.service.TradeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradeController.class)
class TradeControllerListWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradeService tradeService;

    @MockBean
    private TradeMapper tradeMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    // ReconxApplication enables JPA auditing globally; a WebMvc slice has no JPA metamodel.
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @WithMockUser(roles = "VIEWER")
    void list_returnsFilteredPageWithStablePaginationMetadata() throws Exception {
        Trade trade = mock(Trade.class);
        TradeResponse response = new TradeResponse(
                42L, "TRD-20260315-0042", 10L, "SAP", 20L, "Apex",
                "EQUITY", "BUY", new BigDecimal("100.00"), new BigDecimal("245.50"),
                LocalDate.of(2026, 3, 15), "PENDING", Instant.parse("2026-03-15T10:00:00Z"),
                Instant.parse("2026-03-15T10:00:00Z"));
        PageRequest pageRequest = PageRequest.of(1, 2, Sort.by(Sort.Order.desc("createdAt")));

        when(tradeService.list(eq(LocalDate.of(2026, 3, 1)), eq(LocalDate.of(2026, 3, 31)),
                eq("PENDING"), eq(20L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(trade), pageRequest, 3));
        when(tradeMapper.toResponse(trade)).thenReturn(response);

        mockMvc.perform(get("/api/v1/trades")
                        .contextPath("/api")
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31")
                        .param("status", "PENDING")
                        .param("counterpartyId", "20")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(42))
                .andExpect(jsonPath("$.items[0].tradeRef").value("TRD-20260315-0042"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(tradeService).list(eq(LocalDate.of(2026, 3, 1)), eq(LocalDate.of(2026, 3, 31)),
                eq("PENDING"), eq(20L), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }
}
