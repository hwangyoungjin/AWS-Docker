package com.example.demo.repository;

import com.example.demo.model.Role;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RoleRepositoryTest {

    @Autowired
    RoleRepository roleRepository;

    @Test
    @DisplayName(value = "Role 찾기")
    public void findRole(){
        //given

        //when
        Role role = roleRepository.findByRoleName("ROLE_USER");

        //that
        Assertions.assertEquals(role.getRoleName(),"ROLE_USER");
    }


}