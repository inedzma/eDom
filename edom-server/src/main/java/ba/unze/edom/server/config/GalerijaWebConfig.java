package ba.unze.edom.server.config;

import ba.unze.edom.server.service.javna.GalerijaService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GalerijaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String lokacija = GalerijaService.FOLDER.toAbsolutePath().toUri().toString();
        if (!lokacija.endsWith("/")) lokacija += "/";
        registry.addResourceHandler("/galerija/**")
                .addResourceLocations(lokacija)
                .setCachePeriod(3600);
    }
}