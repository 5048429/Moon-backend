package com.example.demo2.service.impl;

import com.example.demo2.entity.User;
import com.example.demo2.mapper.UserMapper;
import com.example.demo2.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Autowired
    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public void saveUser(User user) {
        userMapper.insert(user);
    }
}