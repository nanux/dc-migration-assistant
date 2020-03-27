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

package com.atlassian.migration.datacenter.core.state

import com.atlassian.migration.datacenter.api.ErrorHandler
import com.atlassian.migration.datacenter.core.provisioning.*
import com.atlassian.migration.datacenter.spi.InvalidTransitionException
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows

internal class MigrationStateKtTest {

    val authenticationService = spyk<AuthenticationService>()
    val stackProvisioner = spyk<StackProvisioner>()
    val applicationProvisioner = spyk<ApplicationProvisioner>()
    val creds = spyk<CredentialsProvider>()
    val token = spyk<AuthToken<Any>>()
    val handle = spyk<StackHandle>()
    init {
        every { authenticationService.authenticate(creds) } returns token
    }

    val errorHandler = spyk<ErrorHandler>()
    class MyError : Throwable()
    val myError = MyError()

    val migrationState = MigrationState(authenticationService, stackProvisioner, applicationProvisioner, errorHandler)

    @Test
    fun testStartState() {
        assertEquals(migrationState.stateMachine.state, State.NotStarted)
    }

    @Test
    fun fullRun() {
        migrationState.start(creds)

        assertEquals(State.Finished(handle), migrationState.stateMachine.state)
        verify {
            authenticationService.authenticate(creds)
            stackProvisioner.provision(token)
            applicationProvisioner.provisionApplication(handle)
        }
    }

    @Test
    fun assertAuthenticationAction() {
        migrationState.start(creds)
        verify {
            authenticationService.authenticate(creds)
        }
        assertEquals(migrationState.stateMachine.state, State.Authenticating(creds))
    }

    @Test
    fun canTransitionToError() {
        migrationState.stateMachine.transition(Event.Authenticate(creds))
        verify {
            authenticationService.authenticate(creds)
        }
        assertEquals(migrationState.stateMachine.state, State.Authenticating(creds))

        migrationState.stateMachine.transition(Event.ErrorDetected(myError))
        verify {
            errorHandler.onError(myError)
        }
        assertEquals(migrationState.stateMachine.state, State.Error(myError))
    }

    @Test
    fun cannotTransitionOutOfError() {
        migrationState.stateMachine.transition(Event.Authenticate(creds))
        verify {
            authenticationService.authenticate(creds)
        }
        assertEquals(migrationState.stateMachine.state, State.Authenticating(creds))

        migrationState.stateMachine.transition(Event.ErrorDetected(myError))
        verify {
            errorHandler.onError(myError)
        }

        assertThrows<InvalidTransitionException> { migrationState.stateMachine.transition(Event.Authenticate(creds)) }
    }
}
