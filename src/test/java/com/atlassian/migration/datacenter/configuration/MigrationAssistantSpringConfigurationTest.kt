/*
 * Copyright 2020 Atlassian
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
package com.atlassian.migration.datacenter.configuration

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import java.lang.reflect.Modifier

internal class MigrationAssistantSpringConfigurationTest {
    @Test
    fun shouldEnsureAllBeansAreDefined() {
        val declaredMethods = MigrationAssistantBeanConfiguration::class.java.declaredMethods
        for (method in declaredMethods) {
            if (Modifier.isPublic(method.modifiers)) {
                val declaredAnnotationsByType = method.getDeclaredAnnotationsByType(Bean::class.java)
                Assertions.assertEquals(1, declaredAnnotationsByType.size, "Method " + method.name + " does not have a Bean annotation")
            }
        }
    }

    @Test
    fun shouldEnsureAllOsgiBeansAreDefined() {
        val declaredMethods = MigrationAssistantOsgiImportConfiguration::class.java.declaredMethods
        for (method in declaredMethods) {
            if (Modifier.isPublic(method.modifiers)) {
                val declaredAnnotationsByType = method.getDeclaredAnnotationsByType(Bean::class.java)
                Assertions.assertEquals(1, declaredAnnotationsByType.size, "Method " + method.name + " does not have a Bean annotation")
            }
        }
    }
}