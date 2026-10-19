package com.bhanuagrawal.kmplab.presentation.common

interface ViewState {
    val isLoading: Boolean
    val isError: Boolean
    val errorMessage: String?
}