package com.learn.summer.jdbc.with.tx;

import com.learn.summer.context.AnnotationConfigApplicationContext;
import com.learn.summer.exception.TransactionException;
import com.learn.summer.jdbc.JdbcTemplate;
import com.learn.summer.jdbc.JdbcTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JdbcWithTxTest extends JdbcTestBase {
    @Test
    public void testTx() {
        try(var ctx = new AnnotationConfigApplicationContext(JdbcWithTxApplication.class, createPropertyResolver())) {
            JdbcTemplate jdbcTemplate = ctx.getBean(JdbcTemplate.class);
            jdbcTemplate.update(CREATE_USER);
            jdbcTemplate.update(CREATE_ADDRESS);

            UserService userService = ctx.getBean(UserService.class);
            AddressService addressService = ctx.getBean(AddressService.class);
            // Proxied
            assertNotSame(UserService.class, userService.getClass());
            assertNotSame(AddressService.class, addressService.getClass());
            // 代理对象未注入
            assertNull(userService.addressService);
            assertNull(addressService.userService);

            // Insert user
            User bob = userService.createUser("Bob", 12);
            assertEquals(1, bob.id);

            // insert address
            Address addr1 = new Address(bob.id, "Broadway, New York", 10012);
            Address addr2 = new Address(bob.id, "Fifth Avenue, New York", 10080);
            // addr3 中的 user 不存在
            Address addr3 = new Address(bob.id + 1, "Ocean Drive, Miami, Florida", 33411);
            assertThrows(TransactionException.class, () -> {
                addressService.addAddress(addr1, addr2, addr3);
            });

            // 所有的 address 都不应插入成功
            assertTrue(addressService.getAddresses(bob.id).isEmpty());

            // 仅插入 Bob 的两个地址
            addressService.addAddress(addr1, addr2);
            assertEquals(2, addressService.getAddresses(bob.id).size());

            // 删除 bob 将会导致 rollback
            assertThrows(TransactionException.class, () -> {
                userService.deleteUser(bob);
            });

            // bob 和他的地址依然存在
            assertEquals("Bob", userService.getUser(bob.id).name);
            assertEquals(2, addressService.getAddresses(bob.id).size());
        }
        // re-open db and query
        try(var ctx = new AnnotationConfigApplicationContext(
                JdbcWithTxApplication.class, createPropertyResolver())) {
            AddressService addressService = ctx.getBean(AddressService.class);
            List<Address> addressesOfBob = addressService.getAddresses(1);
            assertEquals(2, addressesOfBob.size());
        }
    }
}
