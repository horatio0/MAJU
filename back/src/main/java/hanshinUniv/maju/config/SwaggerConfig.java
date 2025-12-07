package hanshinUniv.maju.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                             .group("springdoc-public")
                             .pathsToMatch("/**")
                             .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ai를 활용한 면접 연습 메인 서버 API")
                        .version("v1")
                        .description("ai를 활용한 면접 연습 메인 서버 API 명세서"));
    }
}
