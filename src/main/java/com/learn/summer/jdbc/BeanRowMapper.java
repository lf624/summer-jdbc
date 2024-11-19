package com.learn.summer.jdbc;

import com.learn.summer.exception.DataAccessException;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class BeanRowMapper<T> implements RowMapper<T> {
    final Logger logger = LoggerFactory.getLogger(getClass());

    Class<T> clazz;
    Constructor<T> constructor;
    Map<String, Field> fields = new HashMap<>();
    Map<String, Method> methods = new HashMap<>();

    public BeanRowMapper(Class<T> clazz) {
        this.clazz = clazz;
        try {
            constructor = clazz.getConstructor();
        }catch (NoSuchMethodException e) {
            throw new DataAccessException(("No public default constructor method found for class %s " +
                    "when build BeanRowMapper.").formatted(clazz.getName()));
        }
        for(Field f : clazz.getFields()) {
            String name = f.getName();
            fields.put(name, f);
            logger.debug("add row mapping field: {}", name);
        }
        for(Method m : clazz.getMethods()) {
            Parameter[] ps = m.getParameters();
            if(ps.length == 1) {
                String name = m.getName();
                if(name.length() >= 4 && name.startsWith("set")) {
                    String prop = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    methods.put(prop, m);
                    logger.debug("add row mapping: {} to {}({})",
                            prop, name, ps[0].getType().getName());
                }
            }
        }
    }

    @Nullable
    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        T bean;
        try {
            bean = this.constructor.newInstance();
            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();
            for(int i = 1; i <= columns; i++) {
                // label为sql中 as 指定的别名，若未指定则为列名
                String label = meta.getColumnLabel(i);
                Method method = this.methods.get(label);
                if(method != null) {
                    method.invoke(bean, rs.getObject(label));
                } else {
                    Field field = this.fields.get(label);
                    if(field != null)
                        field.set(bean, rs.getObject(label));
                }
            }
        }catch (ReflectiveOperationException e) {
            throw new DataAccessException("Can't map result set to class %s".formatted(clazz.getName()), e);
        }
        return bean;
    }
}
