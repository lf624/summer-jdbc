package com.learn.summer.jdbc.tx;

import com.learn.summer.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

public class DataSourceTransactionManager implements PlatformTransactionManager, InvocationHandler {
    final Logger logger = LoggerFactory.getLogger(getClass());

    static ThreadLocal<TransactionStatus> transactionStatus = new ThreadLocal<>();
    final DataSource dataSource;

    public DataSourceTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        TransactionStatus ts = transactionStatus.get();
        if(ts == null) {
            // 当前无事务，创建新事务
            try(Connection conn = dataSource.getConnection()) {
                final boolean autoCommit = conn.getAutoCommit();
                // 关闭自动提交事务
                if(autoCommit) conn.setAutoCommit(false);
                try {
                    // 设置ThreadLocal状态
                    transactionStatus.set(new TransactionStatus(conn));
                    Object r = method.invoke(proxy, args);
                    conn.commit(); // 提交事务
                    return r;
                } catch (InvocationTargetException e) {
                    logger.warn("will rollback transaction for caused exception: {}",
                            e.getCause() == null ? "null" : e.getCause().getClass().getName());
                    TransactionException te = new TransactionException(e.getCause());
                    try {
                        conn.rollback();
                    }catch (SQLException sqle) {
                        te.addSuppressed(sqle);
                    }
                    throw te;
                } finally {
                    transactionStatus.remove();
                    // 还原设置
                    if(autoCommit) conn.setAutoCommit(true);
                }
            }
        } else {
            // 当前有事务，加入当前事务执行
            return method.invoke(proxy, args);
        }
    }
}
