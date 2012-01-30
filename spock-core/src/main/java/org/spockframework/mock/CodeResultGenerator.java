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

package org.spockframework.mock;

import org.spockframework.util.GroovyRuntimeUtil;

import groovy.lang.Closure;

/**
 *
 * @author Peter Niederwieser
 */
public class CodeResultGenerator implements IResultGenerator {
  private final Closure code;
  private final boolean provideExtendedInfo;

  public CodeResultGenerator(Closure code) {
    this.code = code;
    Class<?>[] paramTypes = code.getParameterTypes();
    provideExtendedInfo = paramTypes.length == 1 && paramTypes[0] == IMockInvocation.class;
  }

  public Object generate(IMockInvocation invocation) {
    Object result = GroovyRuntimeUtil.invokeClosure(code, provideExtendedInfo ? invocation : invocation.getArguments());
    Class<?> returnType = invocation.getMethod().getReturnType();
    
    // don't attempt cast for void methods (closure could be an action that accidentally returns a value)
    if (returnType == void.class || returnType == Void.class) return null;

    return GroovyRuntimeUtil.coerce(result, invocation.getMethod().getReturnType());
  }
}
