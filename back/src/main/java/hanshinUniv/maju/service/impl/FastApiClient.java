package hanshinUniv.maju.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hanshinUniv.maju.entity.CommonAnalysis;
import hanshinUniv.maju.entity.DetailAnalysis;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import hanshinUniv.maju.exception.ExternalServiceException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FastApiClient {
    private final WebClient webClient;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    public FastApiClient(@Value("${app.fastapi.url}") String fastApiUrl) {
        this.webClient = WebClient.builder().baseUrl(fastApiUrl).build();
    }

    public AnalysisResult requestAnalyze(File file) {
        try {
            FileSystemResource resource = new FileSystemResource(file);
            AnalysisResult res = webClient.post()
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData("file", resource))
                    .retrieve()
                    .bodyToMono(AnalysisResult.class)
                    .block();
            if (res == null) throw new ExternalServiceException("FastAPI returned empty response");
            return res;
        } catch (ExternalServiceException ex) {
            log.error("FastApiClient external service error: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("FastApiClient.analyzeVideo error: {}", ex.getMessage(), ex);
            throw new ExternalServiceException("Failed to communicate with FastAPI", ex);
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResult {
        @JsonProperty("metadata")
        private Metadata metadata;

        @JsonProperty("issue_count")
        private Map<String, Object> issueCount;

        @JsonProperty("score")
        private Score score;

        @JsonProperty("frame_details")
        private List<DetailedIssue> detailedIssues;

        public Integer getTotalFrames() {
            return metadata != null ? metadata.totalFrames : null;
        }

        public Integer getAnalyzedFrames() {
            return metadata != null ? metadata.analyzedFrames : null;
        }

        public Integer getFaceIssueFrames() {
            // frame_details에서 face 이슈가 있는 프레임 수 계산
            if (detailedIssues == null) return 0;
            return (int) detailedIssues.stream()
                    .filter(di -> di.issues != null && (
                            (di.issues.eye != null && !di.issues.eye.isEmpty()) ||
                            (di.issues.mouth != null && !di.issues.mouth.isEmpty())
                    ))
                    .count();
        }

        public Integer getPoseIssueFrames() {
            // frame_details에서 pose 이슈가 있는 프레임 수 계산
            if (detailedIssues == null) return 0;
            return (int) detailedIssues.stream()
                    .filter(di -> di.issues != null && di.issues.pose != null && !di.issues.pose.isEmpty())
                    .count();
        }

        public Integer getBothIssueFrames() {
            // frame_details에서 face와 pose 둘 다 이슈가 있는 프레임 수 계산
            if (detailedIssues == null) return 0;
            return (int) detailedIssues.stream()
                    .filter(di -> di.issues != null &&
                            ((di.issues.eye != null && !di.issues.eye.isEmpty()) ||
                             (di.issues.mouth != null && !di.issues.mouth.isEmpty())) &&
                            (di.issues.pose != null && !di.issues.pose.isEmpty())
                    )
                    .count();
        }

        public String getFaceIssueJson() {
            if (issueCount == null) return null;
            try {
                Object eye = issueCount.get("eye");
                Object mouth = issueCount.get("mouth");
                return MAPPER.writeValueAsString(Map.of("eye", eye, "mouth", mouth));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize face issue_count: {}", e.getMessage());
                return null;
            }
        }

        public String getPoseIssueJson() {
            if (issueCount == null) return null;
            try {
                Object pose = issueCount.get("pose");
                return MAPPER.writeValueAsString(Map.of("pose", pose));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize pose issue_count: {}", e.getMessage());
                return null;
            }
        }

        public List<DetailResult> getDetails() {
            if (detailedIssues == null) return List.of();
            List<DetailResult> out = new ArrayList<>();
            for (DetailedIssue di : detailedIssues) {
                DetailResult dr = new DetailResult();
                dr.setFrame(di.frame);
                try {
                    // faceJson: include eye and mouth arrays
                    List<String> eye = di.issues != null && di.issues.eye != null ? di.issues.eye : List.of();
                    List<String> mouth = di.issues != null && di.issues.mouth != null ? di.issues.mouth : List.of();
                    dr.setFaceJson(MAPPER.writeValueAsString(Map.of("eye", eye, "mouth", mouth)));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize detailed issue face arrays: {}", e.getMessage());
                    dr.setFaceJson(null);
                }
                try {
                    List<String> pose = di.issues != null && di.issues.pose != null ? di.issues.pose : List.of();
                    dr.setPoseJson(MAPPER.writeValueAsString(Map.of("pose", pose)));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize detailed issue pose arrays: {}", e.getMessage());
                    dr.setPoseJson(null);
                }
                out.add(dr);
            }
            return out;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Metadata {
            @JsonProperty("total_frames")
            private Integer totalFrames;
            @JsonProperty("analyzed_frames")
            private Integer analyzedFrames;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Score {
            @JsonProperty("total_score")
            private Double totalScore;
            @JsonProperty("face_issue_frames")
            private Integer faceIssueFrames;
            @JsonProperty("pose_issue_frames")
            private Integer poseIssueFrames;
            @JsonProperty("both_issue_frames")
            private Integer bothIssueFrames;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DetailedIssue {
            @JsonProperty("frame_number")
            private Integer frame;
            @JsonProperty("issues")
            private IssueDetail issues;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class IssueDetail {
            @JsonProperty("eye")
            private List<String> eye;
            @JsonProperty("mouth")
            private List<String> mouth;
            @JsonProperty("pose")
            private List<String> pose;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DetailResult {
            @JsonProperty("frame")
            private Integer frame;
            @JsonProperty("face")
            private String faceJson;
            @JsonProperty("pose")
            private String poseJson;
        }
    }
}