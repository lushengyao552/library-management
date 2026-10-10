package com.example.library.repository.impl;

import com.baomidou.mybatisplus.extension.repository.AbstractRepository;
import com.example.library.entity.User;
import com.example.library.mapper.UserMapper;
import com.example.library.repository.UserRepository;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl extends AbstractRepository<UserMapper, User> implements UserRepository {
}
