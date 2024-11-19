package com.learn.summer.jdbc.with.tx;

import com.learn.summer.annotation.Autowired;
import com.learn.summer.annotation.Component;
import com.learn.summer.annotation.Transactional;
import com.learn.summer.jdbc.JdbcTemplate;
import com.learn.summer.jdbc.JdbcTestBase;

@Transactional
@Component
public class UserService {
    @Autowired
    AddressService addressService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    public User createUser(String name, int age) {
        Number id = jdbcTemplate.updateAndReturnGeneratedKey(JdbcTestBase.INSERT_USER, name, age);
        User user = new User();
        user.id = id.intValue();
        user.name = name;
        user.theAge = age;
        return user;
    }

    public User getUser(int userId) {
        return jdbcTemplate.queryForObject(JdbcTestBase.SELECT_USER, User.class, userId);
    }

    public void updateUser(User user) {
        jdbcTemplate.update(JdbcTestBase.UPDATE_USER, user.name, user.theAge, user.id);
    }

    public void deleteUser(User user) {
        jdbcTemplate.update(JdbcTestBase.DELETE_USER, user.id);
        addressService.deleteAddress(user.id);
    }
}
