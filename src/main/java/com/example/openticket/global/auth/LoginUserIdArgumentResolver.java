package com.example.openticket.global.auth;

import com.example.openticket.global.exception.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasAnnotation = parameter.hasParameterAnnotation(LoginUserId.class);
        boolean isLongType = Long.class.isAssignableFrom(parameter.getParameterType());
        return hasAnnotation && isLongType;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Long userId = (Long) webRequest.getAttribute("userId", RequestAttributes.SCOPE_REQUEST);
        if (userId == null) {
            throw new UnauthorizedException("로그인이 필요한 서비스입니다.");
        }
        return userId;
    }
}
