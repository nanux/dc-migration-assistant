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
import com.atlassian.migration.datacenter.core.auth.AuthToken
import com.atlassian.migration.datacenter.core.auth.AuthenticationService
import com.atlassian.migration.datacenter.core.auth.CredentialsProvider
import com.tinder.StateMachine

sealed class State {
    object NotStarted : State()
    data class Authenticating(val creds: CredentialsProvider) : State()
    data class Authenticated(val token: AuthToken<Any>): State()
    data class ProvisionApplication(val token: AuthToken<Any>): State()
    object ProvisionApplicationWait : State()
    object ProvisionMigrationStack : State()
    object ProvisionMigrationStackWait : State()
    object FsMigrationCopy : State()
    object FsMigrationCopyWait : State()
    object OfflineWarning : State()
    object DbMigrationExport : State()
    object DbMigrationExportWait : State()
    object DbMigrationUpload : State()
    object DbMigrationUploadWait : State()
    object DataMigrationImport : State()
    object DataMigrationImportWait : State()
    object Validate : State()
    object Cutover : State()
    object Finished : State()
    data class Error(val error: Throwable) : State()
}

sealed class Event {
    data class Authenticate(val creds: CredentialsProvider) : Event()
    object Authenticated : Event()
    object ProvisioningApplication : Event()
    object ProvisioningStack : Event()
    object FSCopy : Event()
    object DBExport : Event()
    object DBUpload : Event()
    object DBImport : Event()
    object Validation : Event()
    object Cutover : Event()
    object Finished : Event()

    data class ErrorDetected(val error: Throwable) : Event()
}

// TODO: Currently unused; do we really need this?
sealed class Action

class MigrationState(
        private val authenticationService: AuthenticationService,
        private val errorHandler: ErrorHandler
)
{
    fun start(creds: CredentialsProvider) {
        stateMachine.transition(Event.Authenticate(creds))
    }

    val stateMachine = StateMachine.create<State, Event, Action> {
        initialState(State.NotStarted)

        state<State.NotStarted> {
            on<Event.Authenticate> {
                transitionTo(State.Authenticating(it.creds))
            }
            on<Event.ErrorDetected> {
                transitionTo(State.Error(error = Throwable()))
            }
        }

        state<State.Authenticating> {
            onEnter {
                try {
                    val token = authenticationService.authenticate(creds)
                    transitionTo(State.Authenticated(token))
                } catch (e: Throwable) {
                    transitionTo(State.Error(e))
                }
            }
            on<Event.ErrorDetected> {
                transitionTo(State.Error(it.error))
            }
        }

        state<State.Error> {
            onEnter {
                errorHandler.onError(this.error)
            }
        }

        onTransition {
            if (it is StateMachine.Transition.Invalid) {
                throw InvalidTransitionException(it.toString())
            }
        }
    }

}
