package com.example.demo.repository;

import com.example.demo.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account,Long> {
    Account findByEmail(String email);

    @Query("select a from Account a join fetch a.userRoles where a.email = :email")
    Account findByEmailWithRoles(@Param("email") String email);

}
