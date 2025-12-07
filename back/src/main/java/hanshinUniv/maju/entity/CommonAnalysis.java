package hanshinUniv.maju.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "common_analysis", schema = "interview")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CommonAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_frames", nullable = true)
    private Integer totalFrames;

    @Column(name = "analyzed_frames", nullable = true)
    private Integer analyzedFrames;

    @Column(name = "face_issue_frames", nullable = true)
    private Integer faceIssueFrames;

    @Column(name = "pose_issue_frames", nullable = true)
    private Integer poseIssueFrames;

    @Column(name = "both_issue_frames", nullable = true)
    private Integer bothIssueFrames;

    @Column(name = "face_issue", columnDefinition = "JSON", nullable = true)
    private String faceIssue;

    @Column(name = "pose_issue", columnDefinition = "JSON", nullable = true)
    private String poseIssue;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = true, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Answer answer;

    @OneToMany(mappedBy = "commonAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<DetailAnalysis> detailAnalyses = new ArrayList<>();
}