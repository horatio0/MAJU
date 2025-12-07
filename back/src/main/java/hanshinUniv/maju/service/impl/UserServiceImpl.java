package hanshinUniv.maju.service.impl;

import hanshinUniv.maju.dto.user.UserUpdateRequest;
import hanshinUniv.maju.dto.user.PasswordUpdateRequest;
import hanshinUniv.maju.dto.user.UserRequest;
import hanshinUniv.maju.dto.user.UserResponse;
import hanshinUniv.maju.entity.User;
import hanshinUniv.maju.exception.ResourceNotFoundException;
import hanshinUniv.maju.mapper.UserMapper;
import hanshinUniv.maju.repository.UserRepository;
import hanshinUniv.maju.service.UserService;
import hanshinUniv.maju.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public UserResponse create(UserRequest request) {
        User user = userMapper.requestToEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_USER");
        User saved = userRepository.save(user);
        return userMapper.entityToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findById(id).map(userMapper::entityToResponse).orElseThrow(() -> new ResourceNotFoundException("User not found id=" + id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        return userRepository.findByEmail(email).map(userMapper::entityToResponse).orElseThrow(() -> new ResourceNotFoundException("User not found email=" + email));
    }

    @Override
    @Transactional
    public UserResponse update(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found userId=" + userId));
        user.setName(request.getName());
        User saved = userRepository.save(user);
        return userMapper.entityToResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found userId=" + userId));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Password not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        User saved = userRepository.save(user);
        return userMapper.entityToResponse(saved);
    }

    @Override
    public String generateToken(String email) {
        return jwtUtil.generateToken(email);
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        if (!userRepository.existsById(userId))
            throw new ResourceNotFoundException("User not found userId=" + userId);
        userRepository.deleteById(userId);
    }
}
