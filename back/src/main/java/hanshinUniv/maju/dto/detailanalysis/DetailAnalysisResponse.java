package hanshinUniv.maju.dto.detailanalysis;

import hanshinUniv.maju.entity.DetailAnalysis;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailAnalysisResponse {
    private Long id;
    private Integer frame;
    private String face;
    private String pose;
    private Long commonAnalysisId;
}

