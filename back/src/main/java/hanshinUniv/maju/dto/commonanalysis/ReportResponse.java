package hanshinUniv.maju.dto.commonanalysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hanshinUniv.maju.entity.CommonAnalysis;
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private Metadata metadata;
    private Map<String, Map<String, Integer>> issue_count;
    private Score score;
    private List<Map<String, Object>> detailed_issues;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Metadata {
        private Integer total_frames;
        private Integer analyzed_frames;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Score {
        private Double total_score;
        private Integer face_issue_frames;
        private Integer pose_issue_frames;
        private Integer both_issue_frames;
    }

    public static ReportResponse from(CommonAnalysis commonAnalysis) {
        ObjectMapper mapper = new ObjectMapper();
        
        // JSON 문자열을 파싱
        Map<String, Map<String, Integer>> issueCount = new HashMap<>();
        try {
            // faceIssue JSON 파싱 (eye, mouth 포함)
            if (commonAnalysis.getFaceIssue() != null) {
                Map<String, Object> faceIssueMap = mapper.readValue(commonAnalysis.getFaceIssue(), Map.class);
                
                // eye 이슈 추출
                if (faceIssueMap.containsKey("eye")) {
                    Object eyeData = faceIssueMap.get("eye");
                    if (eyeData instanceof Map) {
                        issueCount.put("eye", (Map<String, Integer>) eyeData);
                    }
                }
                
                // mouth 이슈 추출
                if (faceIssueMap.containsKey("mouth")) {
                    Object mouthData = faceIssueMap.get("mouth");
                    if (mouthData instanceof Map) {
                        issueCount.put("mouth", (Map<String, Integer>) mouthData);
                    }
                }
            }
            
            // poseIssue JSON 파싱
            if (commonAnalysis.getPoseIssue() != null) {
                Map<String, Object> poseIssueMap = mapper.readValue(commonAnalysis.getPoseIssue(), Map.class);
                if (poseIssueMap.containsKey("pose")) {
                    Object poseData = poseIssueMap.get("pose");
                    if (poseData instanceof Map) {
                        issueCount.put("pose", (Map<String, Integer>) poseData);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            // 파싱 실패 시 빈 맵 사용
            issueCount.put("eye", new HashMap<>());
            issueCount.put("mouth", new HashMap<>());
            issueCount.put("pose", new HashMap<>());
        }
        
        // 기본값 설정 (없는 경우)
        issueCount.putIfAbsent("eye", new HashMap<>());
        issueCount.putIfAbsent("mouth", new HashMap<>());
        issueCount.putIfAbsent("pose", new HashMap<>());
        
        // DetailAnalysis에서 detailed_issues 생성
        List<Map<String, Object>> detailedIssues = List.of();
        if (commonAnalysis.getDetailAnalyses() != null && !commonAnalysis.getDetailAnalyses().isEmpty()) {
            detailedIssues = commonAnalysis.getDetailAnalyses().stream()
                    .map(detail -> {
                        Map<String, Object> issueMap = new HashMap<>();
                        issueMap.put("frame", detail.getFrame());
                        
                        // face JSON 파싱
                        try {
                            if (detail.getFace() != null) {
                                Map<String, Object> faceMap = mapper.readValue(detail.getFace(), Map.class);
                                issueMap.put("eye", faceMap.getOrDefault("eye", List.of()));
                                issueMap.put("mouth", faceMap.getOrDefault("mouth", List.of()));
                            }
                        } catch (JsonProcessingException e) {
                            issueMap.put("eye", List.of());
                            issueMap.put("mouth", List.of());
                        }
                        
                        // pose JSON 파싱
                        try {
                            if (detail.getPose() != null) {
                                Map<String, Object> poseMap = mapper.readValue(detail.getPose(), Map.class);
                                issueMap.put("pose", poseMap.getOrDefault("pose", List.of()));
                            }
                        } catch (JsonProcessingException e) {
                            issueMap.put("pose", List.of());
                        }
                        
                        return issueMap;
                    })
                    .toList();
        }
        
        // 점수 계산 (100점 만점 기준)
        // 로직: 비율 기반 점수 계산
        // - 총 가능 점수 = 분석된 프레임 수 (각 프레임당 1점)
        // - 얼굴 이슈 발생 프레임마다 0.5점 감점
        // - 자세 이슈 발생 프레임마다 0.5점 감점
        // - 획득 점수 = 분석 프레임 수 - (faceIssueFrames * 0.5 + poseIssueFrames * 0.5)
        // - 최종 점수 = (획득 점수 / 분석 프레임 수) * 100
        double totalScore = 100.0;
        Integer analyzedFrames = commonAnalysis.getAnalyzedFrames();
        if (analyzedFrames != null && analyzedFrames > 0) {
            double maxScore = analyzedFrames.doubleValue();
            double deduction = (commonAnalysis.getFaceIssueFrames() != null ? commonAnalysis.getFaceIssueFrames() * 0.5 : 0) 
                             + (commonAnalysis.getPoseIssueFrames() != null ? commonAnalysis.getPoseIssueFrames() * 0.5 : 0);
            double earnedScore = Math.max(0, maxScore - deduction);
            totalScore = (earnedScore / maxScore) * 100.0;
        }
        
        return ReportResponse.builder()
                .metadata(Metadata.builder()
                        .total_frames(commonAnalysis.getTotalFrames())
                        .analyzed_frames(commonAnalysis.getAnalyzedFrames())
                        .build())
                .issue_count(issueCount)
                .score(Score.builder()
                        .total_score(totalScore)
                        .face_issue_frames(commonAnalysis.getFaceIssueFrames())
                        .pose_issue_frames(commonAnalysis.getPoseIssueFrames())
                        .both_issue_frames(commonAnalysis.getBothIssueFrames())
                        .build())
                .detailed_issues(detailedIssues)
                .build();
    }
}
