package externalproxy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000",
                        "https://m-one.al",
                        "https://m-one.rs",
                        "https://m-one.me",
                        "https://m-one-mne.netlify.app",
                        "https://m-one-bosnia.netlify.app",
                        "https://m-one-nm.netlify.app", 
                        "https://m-one-alb.netlify.app")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*");
    }
}
