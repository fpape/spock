/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.spockframework.spring;

import org.spockframework.runtime.extension.*;
import org.spockframework.runtime.model.ErrorInfo;
import org.spockframework.runtime.model.FieldInfo;
import org.spockframework.runtime.model.SpecInfo;
import org.spockframework.util.NotThreadSafe;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NotThreadSafe
public class SpringInterceptor extends AbstractMethodInterceptor {
  private final SpringTestContextManager manager;

  private Throwable exception;
  private boolean beforeTestMethodInvoked = false;
    private List<FieldInfo> springMockFields = new ArrayList<FieldInfo>();
    private Map<String, Object> indexedDelegatingBeans = new HashMap<String, Object>();

    public SpringInterceptor(SpringTestContextManager manager) {
      this.manager = manager;
    }

  @Override
  public void interceptSetupSpecMethod(IMethodInvocation invocation) throws Throwable {
      loadSpringMocks(invocation.getSpec());
      manager.registerParentApplicationContext(createParentContext());
    manager.beforeTestClass();
    invocation.proceed();
  }

    private void loadSpringMocks(SpecInfo spec) {
        springMockFields.clear();
        for (FieldInfo field : spec.getAllFields()) {
            if (field.getReflection().isAnnotationPresent(SpringMock.class)) {
                springMockFields.add(field);
            }
        }
    }

    private ApplicationContext createParentContext() {
        GenericApplicationContext parentContext = new StaticApplicationContext();
        for (FieldInfo springMockField : springMockFields) {
            String beanName = determineBeanName(springMockField);
            Object delegatingBean = createDelegatingBean(springMockField);
//            parentContext.getBeanFactory().registerSingleton(beanName, delegatingBean);
            indexedDelegatingBeans.put(beanName, delegatingBean);
        }
        parentContext.refresh();   // seems to be required sometimes

        return parentContext;
    }

    private String determineBeanName(FieldInfo field) {
        SpringMock springMockAnnotation = field.getReflection().getAnnotation(SpringMock.class);
        String beanName = springMockAnnotation.value();

        if ("".equals(beanName)) {
            beanName = field.getName();
        }

        return beanName;
    }

    private Object createDelegatingBean(FieldInfo springMockField) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

  @Override
  public void interceptSetupMethod(IMethodInvocation invocation) throws Throwable {
    manager.prepareTestInstance(invocation.getTarget());
    exception = null;
    beforeTestMethodInvoked = true;
    manager.beforeTestMethod(invocation.getTarget(),
        invocation.getFeature().getFeatureMethod().getReflection());
    invocation.proceed();
  }

  @Override
  public void interceptCleanupMethod(IMethodInvocation invocation) throws Throwable {
    if (!beforeTestMethodInvoked) {
      invocation.proceed();
      return;
    }
    beforeTestMethodInvoked = false;

    Throwable cleanupEx = null;
    try {
      invocation.proceed();
    } catch (Throwable t) {
      cleanupEx = t;
      if (exception == null) exception = t;
    }

    Throwable afterTestMethodEx = null;
    try {
      manager.afterTestMethod(invocation.getTarget(),
          invocation.getFeature().getFeatureMethod().getReflection(), exception);
    } catch (Throwable t) {
      afterTestMethodEx = t;
    }

    if (cleanupEx != null) throw cleanupEx;
    if (afterTestMethodEx != null) throw afterTestMethodEx;
  }

  @Override
  public void interceptCleanupSpecMethod(IMethodInvocation invocation) throws Throwable {
    Throwable cleanupSpecEx = null;
    try {
      invocation.proceed();
    } catch (Throwable t) {
      cleanupSpecEx = t;
    }

    Throwable afterTestClassEx = null;
    try {
      manager.afterTestClass();
    } catch (Throwable t) {
      afterTestClassEx = t;
    }

    if (cleanupSpecEx != null) throw cleanupSpecEx;
    if (afterTestClassEx != null) throw afterTestClassEx;
  }

  public void error(ErrorInfo error) {
    if (exception == null)
      exception = error.getException();
  }
}
