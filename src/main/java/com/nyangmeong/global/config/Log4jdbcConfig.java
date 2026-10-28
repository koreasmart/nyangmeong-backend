package com.nyangmeong.global.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.sf.log4jdbc.sql.jdbcapi.DataSourceSpy;

/**
 * [설정 - SQL 로그(log4jdbc)]
 * - DataSource 를 DataSourceSpy 로 감싼다 : JDBC URL·드라이버를 바꾸지 않아 각자
 * application.properties 는 그대로 둔다
 * - prod 프로필은 감싸지 않는다
 * - 로거 레벨 : logback-spring.xml, log4jdbc 설정 : log4jdbc.log4j2.properties
 */
// @Component
// @Profile("!prod")
public class Log4jdbcConfig implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource dataSource && !(bean instanceof DataSourceSpy)) {
            return new DataSourceSpy(dataSource);
        }
        return bean;
    }
}
