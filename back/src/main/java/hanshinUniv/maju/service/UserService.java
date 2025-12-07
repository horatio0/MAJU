package hanshinUniv.maju.service;

import hanshinUniv.maju.dto.user.UserUpdateRequest;
import hanshinUniv.maju.dto.user.PasswordUpdateRequest;
import hanshinUniv.maju.dto.user.UserRequest;
import hanshinUniv.maju.dto.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse create(UserRequest userRequest);
    UserResponse findById(Long id);
    UserResponse findByEmail(String email);
    UserResponse update(Long userId, UserUpdateRequest request);
    UserResponse updatePassword(Long userId, PasswordUpdateRequest request);
    void delete(Long userId);
    String generateToken(String email);
}
