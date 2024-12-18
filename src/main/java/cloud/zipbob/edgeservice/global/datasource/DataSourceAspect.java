package cloud.zipbob.edgeservice.global.datasource;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class DataSourceAspect {

    @Before("execution(* cloud.zipbob.edgeservice.domain.member.service.*.get*(..)) || execution(* cloud.zipbob.edgeservice.domain.member.service.*.check*(..))")
    public void setReadDataSource() {
        log.info("Switching to Slave DataSource");
        DataSourceContextHolder.setDataSourceType("slave");
    }

    @Before("execution(* cloud.zipbob.edgeservice.domain.member.service.*.save*(..)) || execution(* cloud.zipbob.edgeservice.domain.member.service.*.update*(..)) || execution(* cloud.zipbob.edgeservice.domain.member.service.*.delete*(..))")
    public void setWriteDataSource() {
        log.info("Switching to Master DataSource");
        DataSourceContextHolder.setDataSourceType("master");
    }

}

