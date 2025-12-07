package hanshinUniv.maju.dto.user;

import hanshinUniv.maju.entity.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private LocalDateTime regDate;
    private LocalDateTime updDate;
    private List<Long> sessionIds;
}