package com.learn.summer.jdbc.with.tx;

import com.learn.summer.annotation.Autowired;
import com.learn.summer.annotation.Component;
import com.learn.summer.annotation.Transactional;
import com.learn.summer.jdbc.JdbcTemplate;
import com.learn.summer.jdbc.JdbcTestBase;

import java.util.List;

@Component
@Transactional
public class AddressService {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    UserService userService;

    public void addAddress(Address... addresses) {
        for (Address address : addresses) {
            // 检查User是否存在
            userService.getUser(address.userId);
            jdbcTemplate.update(JdbcTestBase.INSERT_ADDRESS, address.userId, address.address, address.zip);
        }
    }

    public List<Address> getAddresses(int userId) {
        return jdbcTemplate.queryForList(JdbcTestBase.SELECT_ADDRESS_BY_USERID, Address.class, userId);
    }

    public void deleteAddress(int userId) {
        jdbcTemplate.update(JdbcTestBase.DELETE_ADDRESS_BY_USERID, userId);
        if(userId == 1)
            throw new RuntimeException("Rollback delete for user id = 1");
    }
}
