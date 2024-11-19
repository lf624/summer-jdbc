package com.learn.summer.jdbc;

import com.learn.summer.exception.DataAccessException;
import com.learn.summer.jdbc.tx.TransactionUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcTemplate {
    final DataSource dataSource;

    public JdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T execute(ConnectionCallback<T> action) {
        // 尝试获取当前事务连接
        Connection current = TransactionUtils.getCurrentConnection();
        if(current != null) {
            try {
                return action.doInConnection(current);
            } catch (SQLException e) {
                throw new DataAccessException(e);
            }
        }
        // 获取新连接
        try(Connection conn = dataSource.getConnection()) {
            final boolean autoCommit = conn.getAutoCommit();
            // 确保回调方法内部的事务控制不干扰外部的事务控制
            if(!autoCommit)
                conn.setAutoCommit(true);
            T result = action.doInConnection(conn);
            // 恢复连接的 autoCommit 状态，以确保外部使用该连接时，事务行为不会受到影响
            if(!autoCommit)
                conn.setAutoCommit(false);
            return result;
        }catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) {
        return execute((Connection conn) -> {
            try(PreparedStatement ps = psc.createPreparedStatement(conn)) {
                return action.doInPreparedStatement(ps);
            }
        });
    }

    public int update(String sql, Object... args) {
        return execute(preparedStatementCreator(sql, args),
                PreparedStatement::executeUpdate);
    }

    public <T> List<T> queryForList(String sql, RowMapper<T> rowMapper, Object... args) {
        return execute(preparedStatementCreator(sql, args),
            (PreparedStatement ps) -> {
                List<T> list = new ArrayList<>();
                try(ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        list.add(rowMapper.mapRow(rs, rs.getRow()));
                }
                return list;
            }
        );
    }

    public <T> List<T> queryForList(String sql, Class<T> clazz, Object... args) {
        return queryForList(sql, new BeanRowMapper<>(clazz), args);
    }

    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
        // execute(PreparedStatementCreator, PreparedStatementCallback<T>)
        return execute(preparedStatementCreator(sql, args), (PreparedStatement ps) -> {
            T t = null;
            try(ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if(t == null)
                        t = rowMapper.mapRow(rs, rs.getRow());
                    else
                        throw new DataAccessException("Multiple rows found.");
                }
            }
            if(t == null)
                throw new DataAccessException("Empty result set.");
            return t;
        });
    }

    public Number updateAndReturnGeneratedKey(String sql, Object... args) {
        return execute((Connection conn) -> {
           var ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
           bindArgs(ps, args);
           return ps;
        }, (PreparedStatement ps) -> {
            int n = ps.executeUpdate();
            if(n == 0)
                throw new DataAccessException("0 rows inserted.");
            if(n > 1)
                throw new DataAccessException("Multiple rows inserted.");
            try(ResultSet keys = ps.getGeneratedKeys()) {
                while (keys.next())
                    return (Number) keys.getObject(1);
            }
            throw new DataAccessException("Return generatedKey wrong.");
        });
    }

    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, Class<T> clazz, Object... args) {
        if(clazz.equals(String.class))
            return (T) queryForObject(sql, StringRowMapper.instance, args);
        if(clazz == Boolean.class || clazz == boolean.class)
            return (T) queryForObject(sql, BooleanRowMapper.instance, args);
        if(Number.class.isAssignableFrom(clazz) || clazz.isPrimitive())
            return (T) queryForObject(sql, NumberRowMapper.instance, args);
        return queryForObject(sql, new BeanRowMapper<T>(clazz), args);
    }

    public Number queryForNumber(String sql, Object... args) {
        return queryForObject(sql, NumberRowMapper.instance, args);
    }

    private PreparedStatementCreator preparedStatementCreator(String sql, Object... args) {
        return (Connection conn) -> {
            PreparedStatement ps = conn.prepareStatement(sql);
            bindArgs(ps, args);
            return ps;
        };
    }
    private void bindArgs(PreparedStatement ps, Object... args) throws SQLException{
        for(int i = 0; i < args.length; i++)
            ps.setObject(i + 1, args[i]);
    }
}

class StringRowMapper implements RowMapper<String> {
    static StringRowMapper instance = new StringRowMapper();

    @Override
    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getString(1);
    }
}

class BooleanRowMapper implements RowMapper<Boolean> {
    static BooleanRowMapper instance = new BooleanRowMapper();

    @Override
    public Boolean mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getBoolean(1);
    }
}

class NumberRowMapper implements RowMapper<Number> {

    static NumberRowMapper instance = new NumberRowMapper();

    @Override
    public Number mapRow(ResultSet rs, int rowNum) throws SQLException {
        return (Number) rs.getObject(1);
    }
}