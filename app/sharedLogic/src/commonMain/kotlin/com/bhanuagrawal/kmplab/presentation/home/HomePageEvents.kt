package com.bhanuagrawal.kmplab.presentation.home

import com.bhanuagrawal.kmplab.domain.paging.ScrollDirection
import com.bhanuagrawal.kmplab.presentation.common.Event
import com.bhanuagrawal.kmplab.presentation.home.model.User

sealed class HomePageEvents : Event {
    data object AddUser : HomePageEvents()

    data class RemoveUser(val user: User) : HomePageEvents()

    data class LoadData(val scrollDirection: ScrollDirection? = null) :
        HomePageEvents() {

    }
}
