package cloud.zipbob.edgeservice.config;

import cloud.zipbob.edgeservice.global.datasource.RoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Slf4j
@EnableTransactionManagement
@RequiredArgsConstructor
@EnableJpaRepositories(
        basePackages = "cloud.zipbob.edgeservice.domain.member.repository",
        entityManagerFactoryRef = "masterEntityManagerFactory",
        transactionManagerRef = "masterTransactionManager"
)
public class DataSourceConfig {

    private final MariaDbProperties mariaDbProperties;

    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        log.info("Initializing Master DataSource");
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(mariaDbProperties.getMaster().getUrl());
        dataSource.setDriverClassName(mariaDbProperties.getMaster().getDriverClassName());
        dataSource.setUsername(mariaDbProperties.getMaster().getUsername());
        dataSource.setPassword(mariaDbProperties.getMaster().getPassword());
        return dataSource;
    }

    @Bean(name = "slaveDataSource")
    public DataSource slaveDataSource() {
        log.info("Initializing Slave DataSource");
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(mariaDbProperties.getSlave().getUrl());
        dataSource.setDriverClassName(mariaDbProperties.getSlave().getDriverClassName());
        dataSource.setUsername(mariaDbProperties.getSlave().getUsername());
        dataSource.setPassword(mariaDbProperties.getSlave().getPassword());
        return dataSource;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("Initializing Routing DataSource");
        RoutingDataSource routingDataSource = new RoutingDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDataSource());
        targetDataSources.put("slave", slaveDataSource());

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDataSource());

        return routingDataSource;
    }


    @Bean(name = "masterEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean masterEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource());
        factory.setPackagesToScan("cloud.zipbob.edgeservice.domain.member");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(hibernateProperties());
        return factory;
    }

    @Bean(name = "masterTransactionManager")
    public PlatformTransactionManager masterTransactionManager(
            LocalContainerEntityManagerFactoryBean masterEntityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(masterEntityManagerFactory.getObject()));
    }

    private Map<String, Object> hibernateProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", true);
        properties.put("hibernate.format_sql", true);
        properties.put("hibernate.use_sql_comments", true);
        properties.put("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect");
        return properties;
    }
}
