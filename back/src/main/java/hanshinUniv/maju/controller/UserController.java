package hanshinUniv.maju.controller;

import hanshinUniv.maju.dto.ApiResponse;
import hanshinUniv.maju.dto.user.CustomUserDetails;
import hanshinUniv.maju.dto.user.PasswordUpdateRequest;
import hanshinUniv.maju.dto.user.UserUpdateRequest;
import hanshinUniv.maju.dto.user.UserResponse;
import hanshinUniv.maju.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @io.swagger.v3.oas.annotations.Operation(summary = "내 정보 조회", description = "인증된 사용자의 정보를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공", content = {@io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = hanshinUniv.maju.dto.ApiResponse.class))}),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 에러", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<ApiResponse<UserResponse>> getUserInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        UserResponse userResponse = userService.findById(customUserDetails.getId());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("User info retrieved successfully", userResponse));
    }

    @PutMapping("/name")
    @io.swagger.v3.oas.annotations.Operation(summary = "이름 업데이트", description = "인증된 사용자의 이름 정보를 업데이트합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이름 업데이트 성공", content = {@io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = hanshinUniv.maju.dto.ApiResponse.class))}),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 에러", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateUserInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                    @RequestBody UserUpdateRequest request) {
        UserResponse update = userService.update(customUserDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("User name updated successfully", update));
    }

    @PutMapping("/password")
    @io.swagger.v3.oas.annotations.Operation(summary = "비밀번호 변경", description = "인증된 사용자의 비밀번호를 변경합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 변경 성공", content = {@io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = hanshinUniv.maju.dto.ApiResponse.class))}),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 에러", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<ApiResponse<UserResponse>> updatePassword(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                    @RequestBody PasswordUpdateRequest request) {
        UserResponse update = userService.updatePassword(customUserDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("User password updated successfully", update));
    }

    @DeleteMapping("")
    @io.swagger.v3.oas.annotations.Operation(summary = "회원 탈퇴", description = "인증된 사용자를 삭제합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원 삭제 성공", content = {@io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = hanshinUniv.maju.dto.ApiResponse.class))}),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 에러", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<ApiResponse<String>> deleteUser(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getId();
        userService.delete(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully userId=" + userId, null));
    }
}
