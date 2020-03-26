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

package com.atlassian.migration.datacenter.spi

import com.atlassian.migration.datacenter.api.ErrorHandler
import com.atlassian.migration.datacenter.core.AuthenticationService
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class MigrationStateKtTest {

    val authenticationService = spyk<AuthenticationService>()
    val errorHandler = spyk<ErrorHandler>()
    class MyError : Throwable()
    val myError = MyError()

    val migrationState = MigrationState(authenticationService, errorHandler)

    @Test
    fun testStartState() {
        assertEquals(migrationState.stateMachine.state, State.NotStarted)
    }

    @Test
    fun assertAuthenticationAction() {
        migrationState.stateMachine.transition(Event.Authenticating)
        verify {
            authenticationService.authenticate()
        }
        assertEquals(migrationState.stateMachine.state, State.Authentication)
    }

    @Test
    fun canTransitionToError() {
        migrationState.stateMachine.transition(Event.Authenticating)
        verify {
            authenticationService.authenticate()
        }
        assertEquals(migrationState.stateMachine.state, State.Authentication)

        migrationState.stateMachine.transition(Event.ErrorDetected(myError))
        verify {
            errorHandler.onError(myError)
        }
        assertEquals(migrationState.stateMachine.state, State.Error(myError))
    }

}
