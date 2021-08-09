package com.example.demo.repository;

import com.example.demo.model.Account;
import com.example.demo.model.GenderEnum;
import com.example.demo.model.Role;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Rollback(value = false)
class AccountRepositoryTest {
    @Autowired
    AccountRepository accountRepository;

    @Autowired
    RoleRepository roleRepository;

    @Test
    @DisplayName(value = "이메일 찾기")
    public void testEmail(){
        //given
        Account account = Account.builder()
                .email("saeae@gmail.com")
                .gender(GenderEnum.MALE)
                .build();
        accountRepository.save(account);

        //when
        Account find = accountRepository.findByEmail("saeae@gmail.com");

        //that
        Assertions.assertEquals(account.getEmail(),find.getEmail());
    }

    @Test
    @DisplayName("계정 역할 불러오기")
    public void saveAccount(){
        //given
        Account account1 = Account.builder()
                .email("TEST1@gmail.com")
                .build();
       Role userRole = roleRepository.findByRoleName("ROLE_USER");
        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN");
        account1.getUserRoles().add(userRole);
        account1.getUserRoles().add(adminRole);
        accountRepository.save(account1);

        //when
        Account newAccount = accountRepository.findByEmailWithRoles("TEST1@gmail.com");
        Role newRole = newAccount.getUserRoles().get(0);

        //that
        Assertions.assertEquals(newRole.getRoleName(),userRole.getRoleName());
    }
}