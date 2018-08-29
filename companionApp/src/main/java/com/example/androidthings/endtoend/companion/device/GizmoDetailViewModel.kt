/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.endtoend.companion.device

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.example.androidthings.endtoend.companion.auth.AuthProvider
import com.example.androidthings.endtoend.companion.data.Toggle
import com.example.androidthings.endtoend.companion.data.ToggleCommand
import com.example.androidthings.endtoend.companion.domain.LoadGizmoUseCase
import com.example.androidthings.endtoend.companion.domain.LoadGizmoUseCaseParameters
import com.example.androidthings.endtoend.companion.domain.SendToggleCommandUseCase
import com.example.androidthings.endtoend.shared.domain.successOr
import com.google.firebase.auth.UserInfo

class GizmoDetailViewModel(
    private val authProvider: AuthProvider,
    private val loadGizmoUseCase: LoadGizmoUseCase,
    private val sendToggleCommandUseCase: SendToggleCommandUseCase
) : ViewModel(), AuthProvider by authProvider {

    // We need both the user info and the gizmo ID before we can load anything. One of these is
    // provided by another LiveData, so use a MediatorLiveData.
    private val loadGizmoArgs = MediatorLiveData<LoadGizmoUseCaseParameters>()
    private val loadGizmoArgsObserver = Observer<LoadGizmoUseCaseParameters> {
        loadGizmoUseCase.execute(it)
    }

    val gizmoLiveData = loadGizmoUseCase.observe()

    init {
        loadGizmoArgs.observeForever(loadGizmoArgsObserver)
        loadGizmoArgs.addSource(userLiveData) { user -> setUser(user) }
    }

    override fun onCleared() {
        super.onCleared()
        loadGizmoArgs.removeObserver(loadGizmoArgsObserver)
    }

    private fun setUser(userInfo: UserInfo?) {
        val args = loadGizmoArgs.value ?: LoadGizmoUseCaseParameters(null, null)
        loadGizmoArgs.value = args.copy(userInfo = userInfo)
    }

    fun setGizmoId(gizmoId: String) {
        val args = loadGizmoArgs.value ?: LoadGizmoUseCaseParameters(null, null)
        loadGizmoArgs.value = args.copy(gizmoId = gizmoId)
    }

    fun onToggleClicked(toggle: Toggle) {
        val userId = userLiveData.value?.uid ?: return
        val gizmoId = gizmoLiveData.value?.successOr(null)?.id ?: return
        // Send toggle command
        sendToggleCommandUseCase.execute(
            ToggleCommand(
                userId, gizmoId, toggle.id, !toggle.on, System.currentTimeMillis()
            )
        )
    }
}
