package hanshinUniv.maju.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question", schema = "interview")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "question", nullable = false, unique = true)
    private String question;
}