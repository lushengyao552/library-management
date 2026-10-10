package com.example.library.controller;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.Result;
import com.example.library.common.result.ResultCode;
import com.example.library.dto.LoginDTO;
import com.example.library.entity.User;
import com.example.library.repository.UserRepository;
import com.example.library.util.JwtUtil;
import com.example.library.vo.LoginVO;
import com.example.library.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证", description = "登录注册")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        User user = userRepository.getOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_FOUND);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        LoginVO vo = LoginVO.builder()
                .token(token)
                .tokenPrefix("Bearer ")
                .expiresIn(86400L)
                .user(userVO)
                .build();
        return Result.success(vo);
    }
}
