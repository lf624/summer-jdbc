package com.learn.summer.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Transactional {
    String value() default "platformTransactionManager";
}
