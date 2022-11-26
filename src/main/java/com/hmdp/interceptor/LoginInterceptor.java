package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author lxy
 * @version 1.0
 * @Description 登录拦截器
 * @date 2022/11/24 1:12
 */
public class LoginInterceptor implements HandlerInterceptor {

    /**
     * 因为LoginInterceptor是new 出来的，并没有交给Spring容器管理，所以我们不能使用Autowire或者@Resource注入，
     * 这里可以使用构造函数，在使用到拦截器的时候，传入需要的对象
     */
    private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 在进入Controller之前会被执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取请求头中的token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            // 不存在
            response.setStatus(401);
            return false;
        }

        // 2.根据token获取用户信息
        Map <Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY+token);
        if(userMap==null){
            response.setStatus(401);
            return false;
        }

        // 3.如果存在，则保存到ThreadLocal
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        UserHolder.saveUser(userDTO);

        // 4.刷新用户token的有效时间 (只要用户在这段时间内用户在线，那么就不会过期)
        String tokenKey = RedisConstants.LOGIN_USER_KEY+token;
        stringRedisTemplate.expire(tokenKey,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 5.放行
        return true;
    }

    /**
     * 在执行完Controller里面的逻辑后执行下面代码
     */    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolder.removeUser();
    }
}
