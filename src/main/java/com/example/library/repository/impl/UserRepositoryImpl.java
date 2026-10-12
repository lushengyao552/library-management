package com.example.library.repository.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.library.entity.User;
import com.example.library.mapper.UserMapper;
import com.example.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {

    private final UserMapper userMapper;
}
