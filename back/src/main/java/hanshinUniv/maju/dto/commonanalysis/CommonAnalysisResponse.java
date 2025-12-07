package hanshinUniv.maju.dto.commonanalysis;

import hanshinUniv.maju.entity.CommonAnalysis;
import hanshinUniv.maju.dto.detailanalysis.DetailAnalysisResponse;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonAnalysisResponse {
    private Long id;
    private Integer totalFrames;
    private Integer analyzedFrames;
    private Integer faceIssueFrames;
    private Integer poseIssueFrames;
    private Integer bothIssueFrames;
    private String faceIssue;
    private String poseIssue;
    private Long answerId;
    private List<Long> detailAnalysisIds;
}

