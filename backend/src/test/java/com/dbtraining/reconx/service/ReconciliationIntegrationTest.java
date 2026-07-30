import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;


@Autowired
private InternalTradeRepository internalTradeRepo;

@Autowired
private ExternalTradeRepository externalTradeRepo;

@Autowired
private ReconResultRepository reconResultRepo;

@Autowired
private ReconciliationService reconciliationService;

@Test
void insertedTradesAreReconciledAndPersisted() {

    Trade internal = new Trade(
            "TRD-INT-1",
            "CP-1",
            "SAP.DE",
            new BigDecimal("100"),
            new BigDecimal("245.50"),
            LocalDate.now());

    Trade external = new Trade(
            "TRD-INT-1",
            "CP-1",
            "SAP.DE",
            new BigDecimal("100"),
            new BigDecimal("245.50"),
            LocalDate.now());

    internalTradeRepo.save(internal);
    externalTradeRepo.save(external);

    reconciliationService.runRecon(
            internalTradeRepo.findAll(),
            externalTradeRepo.findAll());

    List<ReconResult> persisted = reconResultRepo.findAll();

    assertThat(persisted).hasSize(1);
    assertThat(persisted.get(0).status())
            .isEqualTo(ReconResult.Status.MATCHED);
    assertThat(persisted.get(0).tradeRef())
            .isEqualTo("TRD-INT-1");
}