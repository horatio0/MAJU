package hanshinUniv.maju.dto.commonanalysis;

import hanshinUniv.maju.dto.detailanalysis.DetailAnalysisRequest;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonAnalysisRequest {
    private Integer totalFrames;
    private Integer analyzedFrames;
    private Integer faceIssueFrames;
    private Integer poseIssueFrames;
    private Integer bothIssueFrames;
    private String faceIssue; // JSON string
    private String poseIssue; // JSON string
}

