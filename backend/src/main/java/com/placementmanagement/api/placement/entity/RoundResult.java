package com.placementmanagement.api.placement.entity;

import java.time.Instant;
import jakarta.persistence.*;
import com.placementmanagement.api.auth.entity.UserAccount;

@Entity
@Table(name = "placement_round_results", uniqueConstraints = @UniqueConstraint(
        name = "placement_round_results_round_application_uq", columnNames = {"round_id", "application_id"}))
public class RoundResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "round_result_id") private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "round_id") private RecruitmentRound round;
    @ManyToOne(optional = false) @JoinColumn(name = "application_id") private PlacementApplication application;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RoundResultStatus result = RoundResultStatus.PENDING;
    @Column(name = "recorded_at") private Instant recordedAt;
    @ManyToOne @JoinColumn(name = "recorded_by_user_id") private UserAccount recordedBy;

    protected RoundResult() {}

    public RoundResult(RecruitmentRound round, PlacementApplication application) {
        this.round = round;
        this.application = application;
    }

    public Long getId() { return id; }
    public RecruitmentRound getRound() { return round; }
    public PlacementApplication getApplication() { return application; }
    public RoundResultStatus getResult() { return result; }
    public Instant getRecordedAt() { return recordedAt; }
    public void update(RoundResultStatus result, UserAccount recordedBy) {
        this.result = result;
        this.recordedBy = recordedBy;
        this.recordedAt = result == RoundResultStatus.PENDING ? null : Instant.now();
    }
}
