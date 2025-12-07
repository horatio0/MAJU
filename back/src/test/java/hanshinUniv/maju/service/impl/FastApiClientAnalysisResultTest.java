package hanshinUniv.maju.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanshinUniv.maju.service.impl.FastApiClient.AnalysisResult;
import hanshinUniv.maju.service.impl.FastApiClient.AnalysisResult.DetailedIssue;
import hanshinUniv.maju.service.impl.FastApiClient.AnalysisResult.DetailResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FastApiClientAnalysisResultTest {

    private static final String SAMPLE_JSON = "{\n" +
            "  \"metadata\": {\n" +
            "    \"total_frames\": 1000,\n" +
            "    \"analyzed_frames\": 200\n" +
            "  },\n" +
            "  \"issue_count\": {\n" +
            "    \"eye\": {\n" +
            "      \"gaze_averted\": 5,\n" +
            "      \"frown\": 3,\n" +
            "      \"squint\": 2,\n" +
            "      \"face_not_detected\": 0\n" +
            "    },\n" +
            "    \"mouth\": {\n" +
            "      \"jaw_forward\": 1,\n" +
            "      \"jaw_side\": 0,\n" +
            "      \"jaw_open\": 2,\n" +
            "      \"mouth_funnel\": 0,\n" +
            "      \"lip_pucker\": 1,\n" +
            "      \"smile\": 4,\n" +
            "      \"mouth_frown\": 0,\n" +
            "      \"mouth_dimple\": 0,\n" +
            "      \"mouth_stretch\": 0,\n" +
            "      \"lip_roll\": 0,\n" +
            "      \"chin_raise\": 0,\n" +
            "      \"lip_press\": 0,\n" +
            "      \"lower_lip_down\": 0,\n" +
            "      \"upper_lip_up\": 1,\n" +
            "      \"cheek_puff\": 0,\n" +
            "      \"cheek_squint\": 0,\n" +
            "      \"nose_sneer\": 0\n" +
            "    },\n" +
            "    \"pose\": {\n" +
            "      \"shoulder\": 3,\n" +
            "      \"face_forward\": 2,\n" +
            "      \"body_tilt\": 1,\n" +
            "      \"knee_position\": 0,\n" +
            "      \"knee_balance\": 0,\n" +
            "      \"hand_position\": 2,\n" +
            "      \"stiff_posture\": 1,\n" +
            "      \"leg_alignment\": 0,\n" +
            "      \"pose_not_detected\": 0\n" +
            "    }\n" +
            "  },\n" +
            "  \"score\": {\n" +
            "    \"total_score\": 97.5,\n" +
            "    \"face_issue_frames\": 8,\n" +
            "    \"pose_issue_frames\": 4,\n" +
            "    \"both_issue_frames\": 3\n" +
            "  },\n" +
            "  \"detailed_issues\": [\n" +
            "    {\n" +
            "      \"frame\": 45,\n" +
            "      \"eye\": [\"gaze_averted\"],\n" +
            "      \"mouth\": [],\n" +
            "      \"pose\": []\n" +
            "    },\n" +
            "    {\n" +
            "      \"frame\": 150,\n" +
            "      \"eye\": [],\n" +
            "      \"mouth\": [\"smile\", \"lip_pucker\"],\n" +
            "      \"pose\": [\"shoulder\"]\n" +
            "    },\n" +
            "    {\n" +
            "      \"frame\": 320,\n" +
            "      \"eye\": [\"frown\", \"squint\"],\n" +
            "      \"mouth\": [],\n" +
            "      \"pose\": [\"body_tilt\", \"stiff_posture\"]\n" +
            "    },\n" +
            "    {\n" +
            "      \"frame\": 450,\n" +
            "      \"eye\": [\"gaze_averted\"],\n" +
            "      \"mouth\": [\"jaw_open\"],\n" +
            "      \"pose\": []\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    @Test
    public void deserializeAnalysisResult_and_check_fields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AnalysisResult result = mapper.readValue(SAMPLE_JSON, AnalysisResult.class);

        assertNotNull(result);
        assertNotNull(result.getMetadata());
        assertEquals(1000, result.getTotalFrames());
        assertEquals(200, result.getAnalyzedFrames());

        assertNotNull(result.getIssueCount());
        Map<?, ?> issueCount = result.getIssueCount();
        assertTrue(issueCount.containsKey("eye"));
        assertTrue(issueCount.containsKey("mouth"));
        assertTrue(issueCount.containsKey("pose"));

        // score
        assertNotNull(result.getScore());
        assertEquals(97.5, result.getScore().getTotalScore());
        assertEquals(8, result.getScore().getFaceIssueFrames());
        assertEquals(4, result.getScore().getPoseIssueFrames());
        assertEquals(3, result.getScore().getBothIssueFrames());

        // detailed issues
        List<DetailedIssue> detailed = result.getDetailedIssues();
        assertNotNull(detailed);
        assertEquals(4, detailed.size());
        assertEquals(45, detailed.get(0).getFrame());
        assertEquals(150, detailed.get(1).getFrame());

        // helper getters for storage JSON
        String faceJson = result.getFaceIssueJson();
        assertNotNull(faceJson);
        assertTrue(faceJson.contains("\"eye\""));
        assertTrue(faceJson.contains("\"mouth\""));

        String poseJson = result.getPoseIssueJson();
        assertNotNull(poseJson);
        assertTrue(poseJson.contains("\"pose\""));

        List<DetailResult> details = result.getDetails();
        assertNotNull(details);
        assertEquals(4, details.size());
        assertEquals(45, details.get(0).getFrame());
        assertTrue(details.get(0).getFaceJson().contains("gaze_averted"));
    }
}

