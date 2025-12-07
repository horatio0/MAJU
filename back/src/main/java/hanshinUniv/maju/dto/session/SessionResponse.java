package hanshinUniv.maju.dto.session;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionResponse {
    private Long id;
    private String sessionTitle;
    private LocalDateTime createDate;
    private Long userId;
    private List<Long> sessionQuestionIds;
}

