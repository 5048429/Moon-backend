package com.example.demo2.controller;

import com.example.demo2.entity.ToDo;
import com.example.demo2.mapper.TodoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.alibaba.fastjson.JSONObject;
import com.example.demo2.entity.User;
import com.example.demo2.repository.UserRepository;
import com.example.demo2.util.GlobalResult;
import com.example.demo2.util.WechatUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

class UserInfo {
    public UserInfo(String json) {
        JSONObject jsonObject = JSONObject.parseObject(json);
        this.nickName = jsonObject.getString("nickName");
        this.gender = jsonObject.getIntValue("gender");
        this.language = jsonObject.getString("language");
        this.city = jsonObject.getString("city");
        this.province = jsonObject.getString("province");
        this.country = jsonObject.getString("country");
        this.avatarUrl = jsonObject.getString("avatarUrl");
    }
    public UserInfo() {
        // 默认无参构造函数
    }
    private String nickName;
    private int gender;
    private String language;
    private String city;
    private String province;
    private String country;
    private String avatarUrl;
    public String toJSONString() {
        LinkedHashMap<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put("nickName", this.nickName);
        jsonMap.put("gender", this.gender);
        jsonMap.put("language", this.language);
        jsonMap.put("city", this.city);
        jsonMap.put("province", this.province);
        jsonMap.put("country", this.country);
        jsonMap.put("avatarUrl", this.avatarUrl);
        return JSONObject.toJSONString(jsonMap);
    }
    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }
}

@RestController
@RequestMapping
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TodoMapper todoMapper;

    static class LoginRequest {
        private String code;
        private UserInfo rawData;
        private String signature;
        private String encrypteData;
        private String iv;
        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
        public String getSignature() {
            return signature;
        }


        public void setSignature(String signature) {
            this.signature = signature;
        }
        public UserInfo getRawData() {
            return rawData;
        }


        public void setRawData(UserInfo rawData) {
            this.rawData = rawData;
        }
    }

    @PostMapping("/login")
    @ResponseBody
    public GlobalResult user_login(@RequestBody LoginRequest request) {
        // 用户非敏感信息：rawData
        // 签名：signature
        UserInfo rawData = request.getRawData();

        logger.info("收得到code: {}", request.getCode());
        logger.info("收得到rawData: {}", rawData.toJSONString());

        // 1.接收小程序发送的code
        // 2.开发者服务器 登录凭证校验接口 appi + appsecret + code
        JSONObject SessionKeyOpenId = WechatUtil.getSessionKeyOrOpenId(request.getCode());
        // 3.接收微信接口服务 获取返回的参数
        String openid = SessionKeyOpenId.getString("openid");
        String sessionKey = SessionKeyOpenId.getString("session_key");
        logger.info("编译前的签名: {}", rawData.toJSONString() + sessionKey);
        // 4.校验签名 小程序发送的签名signature与服务器端生成的签名signature2 = sha1(rawData + sessionKey)
        String signature2 = DigestUtils.sha1Hex(rawData.toJSONString() + sessionKey);
        logger.info("Signature from request: {}", request.getSignature());
        logger.info("Signature generated by server: {}", signature2);
        if (!request.getSignature().equals(signature2)) {
            return GlobalResult.build(500, "签名校验失败", null);
        }
        // 5.根据返回的User实体类，判断用户是否是新用户，是的话，将用户信息存到数据库；不是的话，更新最新登录时间
        User user = userRepository.findByOpenId(openid);
        // uuid生成唯一key，用于维护微信小程序用户与服务端的会话
        String skey = UUID.randomUUID().toString();
        if (user == null) {
            // 用户信息入库
            String nickName = rawData.getNickName();
            String avatarUrl = rawData.getAvatarUrl();
            int gender = rawData.getGender();
            String city = rawData.getCity();
            String country = rawData.getCountry();
            String province = rawData.getProvince();

            user = new User();
            user.setOpenId(openid);
            user.setSkey(skey);
            user.setCreateTime(new Date());
            user.setLastVisitTime(new Date());
            user.setSessionKey(sessionKey);
            user.setCity(city);
            user.setProvince(province);
            user.setCountry(country);
            user.setAvatarUrl(avatarUrl);
            user.setGender(gender);
            user.setNickName(nickName);
            logger.info("Received login request with code: {}", request.getCode());
            logger.debug("Raw data: {}", JSONObject.toJSONString(rawData));
            userRepository.save(user);
        } else {
            // 已存在，更新用户登录时间
            user.setLastVisitTime(new Date());
            // 重新设置会话skey
            user.setSkey(skey);
            userRepository.save(user);
        }
        //encrypteData比rowData多了appid和openid
        //JSONObject userInfo = WechatUtil.getUserInfo(request.getEncrypteData(), sessionKey, request.getIv());
        //6. 把新的skey返回给小程序
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("skey", skey);
        jsonObject.put("id", user.getId());
        GlobalResult result = GlobalResult.build(200, null, jsonObject);
        return result;
    }

    @GetMapping({"/loginout"})
    public GlobalResult logout() {
        return GlobalResult.build(200, (String) null, (Object) null);
    }

    static class CalendarRequest {
        private String openId;
        private String originalDate;

        public String getOpenId() {
            return openId;
        }

        public void setOpenId(String openId) {
            this.openId = openId;
        }

        public String getOriginalDate() {
            return originalDate;
        }

        public void setOriginalDate(String originalDate) {
            this.originalDate = originalDate;
        }
    }

    @PostMapping("/calender")
    public GlobalResult updateOriginalDate(@RequestBody CalendarRequest request) {
        String openId = request.getOpenId();
        String originalDateString = request.getOriginalDate();

        Date originalDate = null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            originalDate = format.parse(originalDateString); //将字符串转换为日期
        } catch (ParseException e) {
            logger.error("日期格式错误: {}", originalDateString);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GlobalResult.build(400, "日期格式错误", null)).getBody();
        }

        User user = userRepository.findByOpenId(openId);
        if (user == null) {
            logger.error("未找到用户: {}", openId);
            return GlobalResult.build(404, "用户不存在", null);
        }

        user.setOriginalDate(originalDate);
        userRepository.save(user);
        logger.info("更新用户 {} 的生产日期为 {}", openId, originalDate);
        return GlobalResult.build(200, "更新日期成功", null);
    }

    static class ToDoRequest{
        private String openId;

        public String getOpenId() {
            return openId;
        }

        public void setOpenId(String openId) {
            this.openId = openId;
        }
    }

    @PostMapping("/whatToDo")
    public GlobalResult GetWhatToDoByOpenId(@RequestBody ToDoRequest request){
        String openId = request.getOpenId();
        logger.error("用户{}", openId);
        User user = userRepository.findByOpenId(openId);
        if (user == null) {
            logger.error("未找到用户: {}", openId);
            return GlobalResult.build(404, "用户不存在", null);
        }

        Date originaldate = user.getOriginalDate();
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();

        long millisecondsPerDay = 24 * 60 * 60 * 1000;  // 一天的毫秒数
        long daysBetween = (today.getTime() - originaldate.getTime()) / millisecondsPerDay;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        int dateAsInteger = Integer.parseInt(dateFormat.format(today));

        if(daysBetween < 1){
            logger.error("当前日期早于开始日期 {}", daysBetween);
            return GlobalResult.build(404, "当前日期早于开始日期", null);
        }

        List<ToDo> WhatToDo = new ArrayList<>();

        int day = (int) daysBetween;
        if(day > 0 && day <=7){
            WhatToDo = todoMapper.GetWhatToDoByDay(day);
        }
        else if(day <=43){
            int a = day / 7 * 7 +1;
            WhatToDo = todoMapper.GetWhatToDoByDay(a);
        }
        else{
            logger.error("当前日期超出建议范围");
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("currentDay", day);
        jsonObject.put("WhatToDo", WhatToDo);

        return GlobalResult.build(200, "查询成功",jsonObject);
    }

    @GetMapping("/setTest")
    public String setTest(){
        for(int i=0;i<100;i++){
            String s = "今天应该做第"+i+"天该做的事";
            todoMapper.SetTest(i,s);
        }
        return "success";
    }
}
