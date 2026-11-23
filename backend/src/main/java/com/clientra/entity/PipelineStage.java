package com.clientra.entity;

import jakarta.persistence.*;

/** tbl_pipeline_stages: configurable kanban columns (UC-12). */
@Entity
@Table(name = "tbl_pipeline_stages")
public class PipelineStage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer stageId;
    @Column(nullable = false, length = 80)
    private String stageName;
    @Column(nullable = false)
    private int sequenceOrder;          // position on the board
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StageType stageType = StageType.OPEN;

    public Integer getStageId() { return stageId; }
    public void setStageId(Integer stageId) { this.stageId = stageId; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public int getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(int sequenceOrder) { this.sequenceOrder = sequenceOrder; }
    public StageType getStageType() { return stageType; }
    public void setStageType(StageType stageType) { this.stageType = stageType; }
}
