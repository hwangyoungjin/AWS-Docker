package com.example.demo.security.filter;

import com.example.demo.dto.request.AccountApiRequest;
import com.example.demo.security.token.AjaxAuthenticationToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginProcessingFilter extends AbstractAuthenticationProcessingFilter {

    /**
     * json방식으로 클라이언트에서 요청
     * json 방식의 요청을 객체로 추출
     */
    private ObjectMapper objectMapper = new ObjectMapper();


    /**
     * 로그인 요청 받을 url
     * 해당 url 매핑되면 필터 작동
     */
    public LoginProcessingFilter() {
        super(new AntPathRequestMatcher("/login"));
    }

    /**
     * ajax인지 아닌지
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {

        //ajax 아니면 인증처리
        if (!isAjax(request)) {
            throw new IllegalStateException("Authentication is not supported");
        }

        //AccountApiRequest타입으로 json 값 받아오기
        AccountApiRequest accountApiRequest = objectMapper.readValue(request.getReader(), AccountApiRequest.class);

        //NULL 값 확인
        if (accountApiRequest.getEmail() == null || accountApiRequest.getPassword() == null) {
            throw new IllegalArgumentException("Username or Password is Null");
        }

        //Ajax용 토큰을 만들어서 해당 토큰을 통해 인증처리
        AjaxAuthenticationToken ajaxAuthenticationToken
                = new AjaxAuthenticationToken(accountApiRequest.getEmail(), accountApiRequest.getPassword());

        //위에서 만든 토큰 AuthenticationManeger에게 전달
        return getAuthenticationManager().authenticate(ajaxAuthenticationToken);

    }

    /**
     * ajax 방식인지 아닌지에 대한 기준은
     * 사용자가 요청할떄 헤더에 정보를 담아서 보내는데 그 정보에 담긴 값과 같은지 다른지 검사
     * 클라이언트와 서버와 약속을정한다
     * <p>
     * 약속은 ajax or ContentType이 JSON 이어야 한다
     * ajax의 의미는 xml이지만 json으로 요청할 것
     */
    private boolean isAjax(HttpServletRequest request) {
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-with"))
                || request.getContentType().equals(MediaType.APPLICATION_JSON_VALUE)) {
            return true;
        }
        return false;
    }
}
