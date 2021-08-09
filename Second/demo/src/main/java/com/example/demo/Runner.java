package com.example.demo;

import com.example.demo.model.Account;
import com.example.demo.model.Role;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Runner implements ApplicationRunner {
    @Autowired
    RoleRepository roleRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Role role1 = Role.builder()
                .roleName("ROLE_ADMIN")
                .build();
        Role role2 = Role.builder()
                .roleName("ROLE_USER")
                .build();
        roleRepository.save(role1);
        roleRepository.save(role2);

        Account account = Account.builder().email("test@gmail.com").password(passwordEncoder.encode("123123")).build();
        account.getUserRoles().add(role2);
        accountRepository.save(account);

    }
}
