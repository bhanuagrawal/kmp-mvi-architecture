package com.bhanuagrawal.kmplab.presentation.home

import androidx.compose.runtime.Immutable
import com.bhanuagrawal.kmplab.presentation.common.ViewState
import com.bhanuagrawal.kmplab.presentation.home.model.User

@Immutable
data class HomePageState(
    val users: List<User>,
    val allItemLoaded: Boolean,
    val isLoadingBefore: Boolean = false,
    val isLoadingAfter: Boolean = false,
    override val isLoading: Boolean,
    override val isError: Boolean,
    override val errorMessage: String?,
) : ViewState