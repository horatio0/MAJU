package hanshinUniv.maju.dto.session;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionRequest {
    private String sessionTitle;
}

