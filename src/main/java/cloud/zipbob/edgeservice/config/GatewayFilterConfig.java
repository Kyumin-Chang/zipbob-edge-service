package cloud.zipbob.edgeservice.config;

import cloud.zipbob.edgeservice.auth.filter.AddMemberIdHeaderFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayFilterConfig {

    @Bean
    public FilterRegistrationBean<AddMemberIdHeaderFilter> addMemberIdHeaderFilter() {
        FilterRegistrationBean<AddMemberIdHeaderFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new AddMemberIdHeaderFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(2);
        return registrationBean;
    }
}
