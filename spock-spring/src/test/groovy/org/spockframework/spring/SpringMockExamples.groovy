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

package org.spockframework.spring

import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Stepwise

@ContextConfiguration(locations = "SpringMockExamples-context.xml")
@Stepwise
class SpringMockExamples extends Specification {
    @SpringMock('myService2')
    IService2 mockIService2 = Mock(IService2)

    @Autowired
    IService1 service

    def "mock service is invoked"() {
        given:
        def quickBrowFoxMessage = 'The quick brown fox jumps over the lazy dog.'
        when:
        def generated = service.generateString()
        then:
        generated == quickBrowFoxMessage
        1 * mockIService2.generateQuickBrownFox() >> quickBrowFoxMessage
    }

    def "mock is reset"() {
        given:
        def quickBrowFoxMessage = 'The quick brown fox jumps over the lazy dog.'
        when:
        def generated = service.generateString()
        then:
        generated == quickBrowFoxMessage
        1 * mockIService2.generateQuickBrownFox() >> quickBrowFoxMessage
    }
}
