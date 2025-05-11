package bot.tornado.cachedaudioprovider;

import bot.tornado.cachedaudioprovider.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class CachedAudioProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(CachedAudioProviderApplication.class, args);
    }

}
