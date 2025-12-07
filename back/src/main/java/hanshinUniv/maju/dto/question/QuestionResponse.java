package hanshinUniv.maju.dto.question;

import hanshinUniv.maju.entity.Question;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionResponse {
    private Long id;
    private String category;
    private String question;
}