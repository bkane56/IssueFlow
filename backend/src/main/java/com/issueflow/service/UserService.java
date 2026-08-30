package com.issueflow.service;

import com.issueflow.constants.ErrorConstants;
import com.issueflow.dto.response.UserResponse;
import com.issueflow.entity.User;
import com.issueflow.exception.ResourceNotFoundException;
import com.issueflow.mapper.UserMapper;
import com.issueflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public List<UserResponse> findAll() {
        return userRepository.findAllByOrderByNameAsc().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public UserResponse findById(Long id) {
        return userMapper.toResponse(getUser(id));
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorConstants.USER_NOT_FOUND.formatted(id)));
    }
}
