package com.bamdoliro.maru.shared.config;

import com.bamdoliro.maru.shared.auth.AuthenticationArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthenticationArgumentResolver authenticationArgumentResolver;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authenticationArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(validatedAllowedOrigins())
                .allowedMethods(
                        HttpMethod.GET.name(),
                        HttpMethod.HEAD.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.DELETE.name(),
                        HttpMethod.PATCH.name()
                )
                .allowCredentials(true);
    }

    /**
     * 쿠키를 주고받기 위해 allowCredentials(true)를 쓰므로 와일드카드 origin은 허용하지 않는다.
     */
    private String[] validatedAllowedOrigins() {
        String[] origins = Arrays.stream(allowedOrigins)
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);

        if (origins.length == 0) {
            throw new IllegalStateException("cors.allowed-origins가 비어 있습니다.");
        }
        if (Arrays.stream(origins).anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalStateException("allowCredentials(true)에서는 cors.allowed-origins에 와일드카드(*)를 쓸 수 없습니다.");
        }
        return origins;
    }
}
