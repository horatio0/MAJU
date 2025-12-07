package hanshinUniv.maju.dto.detailanalysis;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailAnalysisRequest {
    private Integer frame;
    private String face;
    private String pose;
}

