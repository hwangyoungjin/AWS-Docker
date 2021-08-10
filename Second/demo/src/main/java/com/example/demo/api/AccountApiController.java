package com.example.demo.api;

import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountApiController {
    @GetMapping("/v1/test")
    public String test1(){
        return "success!!";
    }

    @GetMapping("/v2/test")
    public String test2(){
        return "success!!";
    }
}
