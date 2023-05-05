package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取手机短信验证码
     * @param user
     * @param session
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session){
        //获取手机号
        String phone = user.getPhone();

        if(StringUtils.isNotEmpty(phone)){
            //生成随机的验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code:{}",code);
            //调用阿里云提供的短信服务API完成发送短信
        // SMSUtils.sendMessage("阿里云短信测试","SMS_154950909",phone,code);
            //需要将发送的短信存入session中
            //session.setAttribute(phone,code);

            //将随机生成的验证码缓存到Redis中，设置有效期5分钟
            redisTemplate.opsForValue().set(phone,code,5,TimeUnit.MINUTES);

            return R.success("手机验证码发送成功");
        }


        return R.error("手机验证码发送失败");
    }


    /**
     * 移动端用户登录
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map,HttpSession session){
        log.info(map.toString());
        //获取手机号
        String phone = map.get("phone").toString();
        //获取验证码
        String code = map.get("code").toString();
        //从session中获取保存的验证码
     /*   Object codeInSession = session.getAttribute(phone);*/

        //从Redis中获取缓存的验证码
        Object codeInSession = redisTemplate.opsForValue().get(phone);

        //比对验证码-页面提交的验证码和session保存的验证码
        if(codeInSession!=null&&codeInSession.equals(code)){
            //如果比对成功，则登录成功
            LambdaQueryWrapper<User> queryWrapper=new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);

            User user = userService.getOne(queryWrapper);
            //判断当前手机号对应用户是否是新用户，如果是新用户则自动完成注册
            if(user==null){
                user =new User();
                user.setPhone(phone);
                user.setStatus(1);
                //保存到user表中
                userService.save(user);
            }
            //设置用户，防止过滤器过滤
            session.setAttribute("user",user.getId());

            //如果登录成功则删除Redis中的验证码
            redisTemplate.delete(phone);
           return R.success(user);
        }


        return R.error("登录失败");
    }

}
