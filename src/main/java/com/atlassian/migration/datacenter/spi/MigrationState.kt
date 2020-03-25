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

import com.atlassian.migration.datacenter.core.AuthenticationService
import com.atlassian.migration.datacenter.core.aws.db.DatabaseArchivalService
import com.tinder.StateMachine

sealed class State {
    object NotStarted : State()
    object Authentication : State()
    object ProvisionApplication : State()
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
    object Error : State()
}

sealed class Event {
    object Authenticating : Event()
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
    object ErrorDetected : Event()
}

sealed class Action {
    object Authenticate : Action()
    object ProvisionApplication : Action()
    object ProvisionStack : Action()
    object CopyFilesystem : Action()
    object ExportDatabase : Action()
    object UploadDatabase : Action()
    object ImportData : Action()
    object Validate: Action()
    object Finish: Action()
    object Error: Action()
}

class MigrationState(
        private val authenticationService: AuthenticationService
)
{

    val stateMachine =
        StateMachine.create<State, Event, Action> {
            initialState(State.NotStarted)

            state<State.NotStarted> {
                on<Event.Authenticating> {
                    transitionTo(State.Authentication, Action.Authenticate)
                }
            }

            state<State.Authentication> {
                on<Event.Authenticated> {
                    transitionTo(State.ProvisionApplication, Action.ProvisionApplication)
                }
            }

            onTransition {
                val validTransition = it as? StateMachine.Transition.Valid ?: return@onTransition
                when (validTransition.sideEffect) {
                    Action.Authenticate -> authenticationService.authenticate()
                }
            }
        }

}