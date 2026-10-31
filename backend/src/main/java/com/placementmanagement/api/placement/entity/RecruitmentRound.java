package com.placementmanagement.api.placement.entity;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "placement_drive_rounds", uniqueConstraints = @UniqueConstraint(
        name = "placement_drive_rounds_drive_order_uq", columnNames = {"drive_id", "round_order"}))
public class RecruitmentRound {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "round_id") private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "drive_id") private PlacementDrive drive;
    @Column(name = "round_order", nullable = false) private Integer order;
    @Column(name = "round_name", nullable = false) private String name;
    @Column(name = "is_final", nullable = false) private boolean finalRound;
    @Column(name = "results_published", nullable = false) private boolean resultsPublished;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected RecruitmentRound() {}

    public RecruitmentRound(PlacementDrive drive, Integer order, String name, boolean finalRound) {
        this.drive = drive;
        this.order = order;
        this.name = name;
        this.finalRound = finalRound;
    }

    public Long getId() { return id; }
    public PlacementDrive getDrive() { return drive; }
    public Integer getOrder() { return order; }
    public String getName() { return name; }
    public boolean isFinalRound() { return finalRound; }
    public boolean isResultsPublished() { return resultsPublished; }
    public void update(Integer order, String name, boolean finalRound) {
        this.order = order;
        this.name = name;
        this.finalRound = finalRound;
        this.updatedAt = Instant.now();
    }
    public void publishResults() { this.resultsPublished = true; this.updatedAt = Instant.now(); }
}
