package com.yangzheng.filter;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.util.WebUtils;
import org.crazycake.shiro.RedisManager;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Deque;
import java.util.LinkedList;

public class KickoutSessionControlFilter extends AccessControlFilter {

    private String kickoutUrl; //踢出后到的地址
    private RedisManager redisManager;
    private boolean kickoutAfter = false; //踢出之前登录的/之后登录的用户 默认踢出之前登录的用户
    private int maxSession = 1; //同一个帐号最大会话数 默认1

    private SessionManager sessionManager;
    private Cache<String, Deque<Serializable>> cache;

    public void setRedisManager(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    public void setKickoutUrl(String kickoutUrl) {
        this.kickoutUrl = kickoutUrl;
    }

    public void setKickoutAfter(boolean kickoutAfter) {
        this.kickoutAfter = kickoutAfter;
    }

    public void setMaxSession(int maxSession) {
        this.maxSession = maxSession;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    //设置Cache的key的前缀
    public void setCacheManager(CacheManager cacheManager) {
        this.cache = cacheManager.getCache("shiro_redis_cache:");
    }

    /**
     * 表示是否允许访问；mappedValue就是[urls]配置中拦截器参数部分，如果允许访问返回true，否则false；
     * (感觉这里应该是对白名单（不需要登录的接口）放行的)
     * 如果isAccessAllowed返回true则onAccessDenied方法不会继续执行
     *
     * @param request
     * @param response
     * @param mappedValue
     * @return
     * @throws Exception
     */

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
        System.err.println(">>>>>>>>>>>>>>>>Session 队列>>>>>>>>>>>>>>>>>>");
        HttpServletRequest request1 = (HttpServletRequest) request;
        String username = request1.getParameter("username");
        String sessionId = request1.getSession().getId();
        if (cache.get(username)==null) {
            //如果没有登录，直接进行登录的流程
            Deque<Serializable> deque = new LinkedList<>();
            deque.push(sessionId);
            cache.put(username, deque);
            return true;
        } else {
            Deque<Serializable> deque = cache.get(username);
            String first = (String)deque.getFirst();
            if (first.equals(sessionId)) {
                return true;
            }

            throw new RuntimeException();
        }


    }


    /**
     * 表示当访问拒绝时是否已经处理了；如果返回true表示需要继续处理；
     * 如果返回false表示该拦截器实例已经处理了，将直接返回即可。
     * onAccessDenied是否执行取决于isAccessAllowed的值，
     * 如果返回true则onAccessDenied不会执行；如果返回false，执行onAccessDenied
     * 如果onAccessDenied也返回false，则直接返回，
     * 不会进入请求的方法（只有isAccessAllowed和onAccessDenied的情况下）
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        //退出
        Subject subject = getSubject(request, response);
        subject.logout();
        //保存访问路径
        saveRequest(request);
        //重定向
        WebUtils.issueRedirect(request, response, kickoutUrl);

        return Boolean.FALSE;
    }



}
