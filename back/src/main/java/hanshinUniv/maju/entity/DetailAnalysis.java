package hanshinUniv.maju.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "detail_analysis", schema = "interview")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DetailAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "frame", nullable = false)
    private Integer frame;

    @Column(name = "face", columnDefinition = "TEXT")
    private String face;

    @Column(name = "pose", columnDefinition = "TEXT")
    private String pose;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "common_analysis_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CommonAnalysis commonAnalysis;
}