package com.example.demo2.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    //http://ww.baidu.com/s?nickname=zhangshan
    @RequestMapping(value= "/hello",method = RequestMethod.GET)
    public String hello(String nickname){

        return "你好啊wo"+nickname;
    }
}
