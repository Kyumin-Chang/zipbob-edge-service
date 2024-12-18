package cloud.zipbob.edgeservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
public class MariaDbProperties {

    private DataSourceProperties master;
    private DataSourceProperties slave;

    @Setter
    @Getter
    public static class DataSourceProperties {
        private String url;
        private String driverClassName;
        private String username;
        private String password;

    }

}
