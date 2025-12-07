package hanshinUniv.maju.controller;

import hanshinUniv.maju.dto.ApiResponse;
import hanshinUniv.maju.dto.commonanalysis.CommonAnalysisRequest;
import hanshinUniv.maju.dto.commonanalysis.CommonAnalysisResponse;
import hanshinUniv.maju.dto.commonanalysis.ReportResponse;
import hanshinUniv.maju.dto.detailanalysis.DetailAnalysisRequest;
import hanshinUniv.maju.dto.detailanalysis.DetailAnalysisResponse;
import hanshinUniv.maju.dto.user.CustomUserDetails;
import hanshinUniv.maju.entity.AnalysisTask;
import hanshinUniv.maju.entity.Answer;
import hanshinUniv.maju.repository.AnalysisTaskRepository;
import hanshinUniv.maju.service.CommonAnalysisService;
import hanshinUniv.maju.service.DetailAnalysisService;
import hanshinUniv.maju.service.impl.FastApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public class SimpleAnalysisController {

    private final FastApiClient fastApiClient;
    private final CommonAnalysisService commonAnalysisService;
    private final DetailAnalysisService detailAnalysisService;
    private final AnalysisTaskRepository analysisTaskRepository;

    @Value("${app.file.storage-dir:/tmp/maju/videos}")
    private String storageDir;

    @Autowired
    public SimpleAnalysisController(FastApiClient fastApiClient,
                                   CommonAnalysisService commonAnalysisService,
                                   DetailAnalysisService detailAnalysisService,
                                   AnalysisTaskRepository analysisTaskRepository) {
        this.fastApiClient = fastApiClient;
        this.commonAnalysisService = commonAnalysisService;
        this.detailAnalysisService = detailAnalysisService;
        this.analysisTaskRepository = analysisTaskRepository;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<Map<String, String>>> analyzeVideoSimple(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("File is empty"));
        }

        try {
            Long userId = userDetails.getId();
            System.out.println("======= 파일 업로드 시작 =======");
            System.out.println("사용자 ID: " + userId);
            System.out.println("파일명: " + file.getOriginalFilename());
            System.out.println("파일 크기: " + file.getSize());
            
            // 저장 디렉토리 생성
            Path storagePath = Paths.get(storageDir);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }

            // 파일 저장
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = storagePath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            System.out.println("파일 저장 완료: " + filePath);

            // 작업 ID 생성
            String taskId = UUID.randomUUID().toString();
            System.out.println("생성된 taskId: " + taskId);

            // AnalysisTask 엔티티 생성 및 저장
            AnalysisTask task = AnalysisTask.builder()
                    .taskId(taskId)
                    .videoPath(filePath.toString())
                    .status(AnalysisTask.TaskStatus.PENDING)
                    .userId(userId)
                    .build();
            task = analysisTaskRepository.save(task);
            final Long savedTaskId = task.getId();
            System.out.println("DB 저장 완료 - Task DB ID: " + savedTaskId);

            // 백그라운드 처리는 별도 스레드에서
            final File videoFile = filePath.toFile();
            new Thread(() -> processAnalysis(savedTaskId, videoFile)).start();

            // 즉시 taskId 반환
            Map<String, String> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("status", "PENDING");
            
            System.out.println("======= 응답 반환: " + response + " =======");
            return ResponseEntity.ok(ApiResponse.success("Analysis task created", response));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("File upload failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Task creation failed: " + e.getMessage()));
        }
    }

    private void processAnalysis(Long taskId, File videoFile) {
        try {
            // 상태를 PROCESSING으로 업데이트
            AnalysisTask task = analysisTaskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));
            task.setStatus(AnalysisTask.TaskStatus.PROCESSING);
            analysisTaskRepository.save(task);

            // 분석 요청
            FastApiClient.AnalysisResult result = fastApiClient.requestAnalyze(videoFile);

            // CommonAnalysis 생성 및 저장 (Answer 없이)
            CommonAnalysisRequest commonAnalysisRequest = CommonAnalysisRequest.builder()
                    .totalFrames(result.getTotalFrames())
                    .analyzedFrames(result.getAnalyzedFrames())
                    .faceIssueFrames(result.getFaceIssueFrames())
                    .poseIssueFrames(result.getPoseIssueFrames())
                    .bothIssueFrames(result.getBothIssueFrames())
                    .faceIssue(result.getFaceIssueJson())
                    .poseIssue(result.getPoseIssueJson())
                    .build();

            CommonAnalysisResponse commonAnalysisResponse = commonAnalysisService.create(null, commonAnalysisRequest);

            // DetailAnalysis 생성 및 저장
            List<DetailAnalysisRequest> detailRequests = result.getDetails().stream()
                    .map(d -> DetailAnalysisRequest.builder()
                            .frame(d.getFrame())
                            .face(d.getFaceJson())
                            .pose(d.getPoseJson())
                            .build())
                    .toList();

            detailAnalysisService.create(commonAnalysisResponse.getId(), detailRequests);

            // 상태를 COMPLETED로 업데이트  
            task.setCommonAnalysisId(commonAnalysisResponse.getId());
            task.setStatus(AnalysisTask.TaskStatus.COMPLETED);
            analysisTaskRepository.save(task);

        } catch (Exception e) {
            // 실패 시 상태를 FAILED로 업데이트
            AnalysisTask task = analysisTaskRepository.findById(taskId).orElse(null);
            if (task != null) {
                task.setStatus(AnalysisTask.TaskStatus.FAILED);
                task.setErrorMessage(e.getMessage());
                analysisTaskRepository.save(task);
            }
        }
    }

    @GetMapping("/analyze/{taskId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalysisStatus(@PathVariable String taskId) {
        AnalysisTask task = analysisTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", task.getTaskId());
        response.put("status", task.getStatus().toString());
        
        if (task.getStatus() == AnalysisTask.TaskStatus.COMPLETED && task.getCommonAnalysisId() != null) {
            // 완료된 경우 분석 결과 포함
            hanshinUniv.maju.entity.CommonAnalysis commonAnalysisEntity = 
                commonAnalysisService.findEntityById(task.getCommonAnalysisId());
            ReportResponse reportResponse = ReportResponse.from(commonAnalysisEntity);
            response.put("commonAnalysis", reportResponse);
        } else if (task.getStatus() == AnalysisTask.TaskStatus.FAILED) {
            response.put("error", task.getErrorMessage());
        }

        return ResponseEntity.ok(ApiResponse.success("Task status retrieved", response));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllTasks(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        List<AnalysisTask> tasks = analysisTaskRepository.findAll().stream()
                .filter(task -> task.getUserId() != null && task.getUserId().equals(userId))
                .toList();
        
        List<Map<String, Object>> taskList = tasks.stream().map(task -> {
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("taskId", task.getTaskId());
            taskMap.put("status", task.getStatus().toString());
            taskMap.put("createdAt", task.getCreatedAt().toString());
            
            // COMPLETED 상태이고 commonAnalysisId가 있으면 보고서도 포함
            if (task.getStatus() == AnalysisTask.TaskStatus.COMPLETED && task.getCommonAnalysisId() != null) {
                try {
                    hanshinUniv.maju.entity.CommonAnalysis commonAnalysisEntity = 
                        commonAnalysisService.findEntityById(task.getCommonAnalysisId());
                    ReportResponse reportResponse = ReportResponse.from(commonAnalysisEntity);
                    taskMap.put("commonAnalysis", reportResponse);
                } catch (Exception e) {
                    // 보고서 조회 실패 시 무시
                }
            }
            
            return taskMap;
        }).toList();
        
        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved", taskList));
    }

    @GetMapping("/video/{taskId}")
    public ResponseEntity<Resource> getVideo(@PathVariable String taskId) {
        try {
            AnalysisTask task = analysisTaskRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));
            
            File videoFile = new File(task.getVideoPath());
            if (!videoFile.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(videoFile);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + videoFile.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
