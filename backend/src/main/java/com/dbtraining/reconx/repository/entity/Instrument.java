package com.dbtraining.reconx.repository.entity;

import jakarta.persistence.*;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;
/**
 * TICKET-ADV051 — JPA entity Instrument. JSONB metadata column wired via
 * the Hypersistence Utils JsonBinaryType on Postgres; H2 stores it as a
 * plain CLOB via the dialect translation (acceptable for dev).
 */
@Entity
@Table(name = "instruments")
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "asset_class", nullable = false, length = 20)
    private String assetClass;

    @Column(nullable = false, length = 3)
    private String currency;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadata = new HashMap<>();

    public Instrument() {}

    public Long getId()         { return id; }
    public String getSymbol()   { return symbol; }
    public String getName()     { return name; }
    public String getAssetClass(){ return assetClass; }
    public String getCurrency() { return currency; }
    public Map<String, Object> getMetadata() {
    return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
