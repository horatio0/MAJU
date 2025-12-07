package hanshinUniv.maju.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "answer", schema = "interview")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_url", nullable = true)
    private String videoUrl;

    @OneToOne(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CommonAnalysis commonAnalysis;
}
