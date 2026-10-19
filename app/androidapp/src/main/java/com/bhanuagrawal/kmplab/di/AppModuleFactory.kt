package com.bhanuagrawal.kmplab.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bhanuagrawal.kmplab.AppDataStore
import com.bhanuagrawal.kmplab.MyApp
import com.bhanuagrawal.kmplab.ui.RegistrationViewmodel
import com.bhanuagrawal.kmplab.SharedPrefDataStore
import com.bhanuagrawal.kmplab.presentation.home.LoadUsersEvenHandler

object AppModuleFactory {


    fun getSharedModule() = MyApp.instance.sharedLogicModule


    fun getLoadUserEventHandler(shareModule: ShareModule): LoadUsersEvenHandler{
        return LoadUsersEvenHandler(shareModule.getUserPager())
    }


    fun getAppDataStore(): AppDataStore{
        return SharedPrefDataStore(MyApp.instance)
    }



    fun getRegistritrationViewmodelFactory(shareModule: ShareModule): ViewModelProvider.Factory {
        return viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle() // from CreationExtr
                RegistrationViewmodel(getAppDataStore(), savedStateHandle, shareModule.getRegisterUsecase())
            }
        }
    }
}