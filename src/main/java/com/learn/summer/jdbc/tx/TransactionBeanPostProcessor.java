package com.learn.summer.jdbc.tx;

import com.learn.summer.annotation.Transactional;
import com.learn.summer.aop.AnnotationProxyBeanPostProcessor;

public class TransactionBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Transactional> {
}
