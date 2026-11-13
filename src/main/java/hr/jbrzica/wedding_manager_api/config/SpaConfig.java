package hr.jbrzica.wedding_manager_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Na serveru Spring Boot servira i Angular aplikaciju (iz static/).
 * Adrese poput /kalendar ili /vjencanja/5 ne postoje kao datoteke, nego ih rješava Angular,
 * pa za njih vraćamo index.html. Za /api/... nikad, da kriva API adresa ostane 404.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String putanja, Resource lokacija) throws IOException {
                        // Standardni resolver provjerava da datoteka stvarno leži unutar static/ (nema ../)
                        Resource datoteka = super.getResource(putanja, lokacija);
                        if (datoteka != null) {
                            return datoteka;
                        }
                        if (putanja.startsWith("api/")) {
                            return null;
                        }
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
