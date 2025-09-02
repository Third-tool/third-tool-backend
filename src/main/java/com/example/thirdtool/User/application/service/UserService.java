package com.example.thirdtool.User.application.service;

import com.example.thirdtool.User.domain.model.User;
import com.example.thirdtool.User.domain.repository.UserRepository;
import com.example.thirdtool.User.presentation.dto.UserJoinRequestDto;
import com.example.thirdtool.User.presentation.dto.UserLoginRequestDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User join(UserJoinRequestDto dto) {
        // 이메일 중복 확인
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }


        User user = User.of(dto.email(), dto.password());

        return userRepository.save(user);
    }

    @Transactional
    public User login(UserLoginRequestDto dto) {
        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(dto.email())
                                  .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 잘못되었습니다."));

        return user;
    }
}
