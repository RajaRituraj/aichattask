package com.example.aichattask.presentation.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()

    // Stable random UUID for this device session
    val userId: String = UUID.randomUUID().toString().take(8)

    fun onNameChange(name: String) {
        _userName.value = name
    }

    fun isNameValid() = _userName.value.trim().length >= 2
}
