package com.learn.summer.jdbc.with.tx;

import com.learn.summer.annotation.ComponentScan;
import com.learn.summer.annotation.Configuration;
import com.learn.summer.annotation.Import;
import com.learn.summer.jdbc.JdbcConfiguration;

@Import(JdbcConfiguration.class)
@ComponentScan
@Configuration
public class JdbcWithTxApplication {
}
