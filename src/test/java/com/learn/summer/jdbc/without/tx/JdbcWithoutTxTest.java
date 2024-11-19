package com.learn.summer.jdbc.without.tx;

import com.learn.summer.context.AnnotationConfigApplicationContext;
import com.learn.summer.exception.DataAccessException;
import com.learn.summer.jdbc.JdbcTemplate;
import com.learn.summer.jdbc.JdbcTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JdbcWithoutTxTest extends JdbcTestBase {
    @Test
    public void testJdbcWithoutTx() {
        try(var ctx = new AnnotationConfigApplicationContext(
                JdbcWithoutTxApplication.class, createPropertyResolver())) {
            JdbcTemplate jdbcTemplate = ctx.getBean(JdbcTemplate.class);
            jdbcTemplate.update(CREATE_USER);
            jdbcTemplate.update(CREATE_ADDRESS);
            // Insert user
            int userId1 = jdbcTemplate.updateAndReturnGeneratedKey(INSERT_USER, "alysia", 12).intValue();
            int userId2 = jdbcTemplate.updateAndReturnGeneratedKey(INSERT_USER, "lucy", null).intValue();
            assertEquals(1, userId1);
            assertEquals(2, userId2);
            // query user
            User alysia = jdbcTemplate.queryForObject(SELECT_USER, User.class, userId1);
            User lucy = jdbcTemplate.queryForObject(SELECT_USER, User.class, userId2);
            assertEquals(1, alysia.id);
            assertEquals("alysia", alysia.name);
            assertEquals(12, alysia.theAge);
            assertEquals("lucy", lucy.name);
            // query name id
            assertEquals("lucy", jdbcTemplate.queryForObject(SELECT_USER_NAME, String.class, userId2));
            assertEquals(12, jdbcTemplate.queryForObject(SELECT_USER_AGE, int.class, userId1));
            // update user
            int n1 = jdbcTemplate.update(UPDATE_USER, "Alysia", 18, alysia.id);
            assertEquals(1, n1);
            // delete user
            int n2 = jdbcTemplate.update(DELETE_USER, lucy.id);
            assertEquals(1, n2);
        }
        // re-open db and query
        try(var ctx = new AnnotationConfigApplicationContext(
                JdbcWithoutTxApplication.class, createPropertyResolver())) {
            JdbcTemplate jdbcTemplate = ctx.getBean(JdbcTemplate.class);
            User alysia = jdbcTemplate.queryForObject(SELECT_USER, User.class, 1);
            assertEquals("Alysia", alysia.name);
            assertEquals(18, alysia.theAge);
            assertThrows(DataAccessException.class, () -> {
                jdbcTemplate.queryForObject(SELECT_USER, User.class, 2);
            });
        }
    }
}
