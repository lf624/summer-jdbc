package com.learn.summer.jdbc.tx;

import jakarta.annotation.Nullable;

import java.sql.Connection;

public class TransactionUtils {
    @Nullable
    public static Connection getCurrentConnection() {
        TransactionStatus ts = DataSourceTransactionManager.transactionStatus.get();
        return ts == null ? null : ts.connection;
    }
}
