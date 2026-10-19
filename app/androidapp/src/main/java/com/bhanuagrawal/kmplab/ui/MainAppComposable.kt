package com.bhanuagrawal.kmplab.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhanuagrawal.kmplab.di.AppModuleFactory
import com.bhanuagrawal.kmplab.ui.home.HomeScreen
import com.bhanuagrawal.kmplab.ui.home.HomeViewmodel
import com.bhanuagrawal.kmplab.ui.home.MyUserList
import com.bhanuagrawal.kmplab.LocalNavController
import com.bhanuagrawal.kmplab.presentation.home.HomePageEvents
import com.bhanuagrawal.kmplab.ui.upload.UploadScreen


@Composable
fun MainAppComposable(startRoute: String = Screen.HomeScreen.route) {
    Scaffold(topBar = {}, bottomBar = {}) { padding ->
        Box(modifier = Modifier.padding(padding)) {

            val navController = rememberNavController()
            CompositionLocalProvider(LocalNavController provides navController) {

                NavHost(
                    navController = navController, startDestination = startRoute
                ) {
                    composable(
                        route = Screen.HomeScreen.route
                    ) {
                        val viewModel: HomeViewmodel = viewModel(
                            factory = viewModelFactory {
                                initializer {
                                    val sharedModule = AppModuleFactory.getSharedModule()

                                    val registerUsecase = sharedModule.getRegisterUsecase()

                                    HomeViewmodel(
                                        registerUsecase,
                                        sharedModule.getDeleteUserUsecase(),
                                        AppModuleFactory.getLoadUserEventHandler(sharedModule)

                                    )
                                }
                            }
                        )
                        HomeScreen(modifier = Modifier, viewModel)

                    }

                    composable(route = Screen.LazyListScreen.route) {
                        val viewModel: HomeViewmodel = viewModel(
                            factory = viewModelFactory {
                                initializer {
                                    val sharedModule = AppModuleFactory.getSharedModule()
                                    HomeViewmodel(
                                        sharedModule.getRegisterUsecase(),
                                        sharedModule.getDeleteUserUsecase(),
                                        AppModuleFactory.getLoadUserEventHandler(sharedModule)
                                    )
                                }
                            }
                        )
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        MyUserList(
                            isLoading = state.isLoading,
                            users = state.users,
                            onRemove = { viewModel.onEvent(HomePageEvents.RemoveUser(it)) },
                            onAdd = { viewModel.onEvent(HomePageEvents.AddUser) },
                            onEvent = {}
                        )
                    }

                    composable(
                            route = Screen.RegisterScreen.route
                            ) {
                        val viewmodel: RegistrationViewmodel = viewModel(
                            factory = run{
                                val shareModule = AppModuleFactory.getSharedModule()
                                AppModuleFactory.getRegistritrationViewmodelFactory(shareModule)
                            }
                        )
                        RegistrationScreen(modifier = Modifier, viewmodel)
                    }

                    composable(route = Screen.UploadScreen.route) {
                        UploadScreen()
                    }
                }

            }

        }

    }
}

@Composable
fun MyNavHost(){

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "/home"
    ){
        composable(
            route = "/home"
        ) {


        }
    }
}


sealed class Screen(val route: String) {
    data object HomeScreen : Screen("/home")
    data object RegisterScreen : Screen("/register")

    data object UserDetail : Screen("/detail/{id}")
    data object LazyListScreen : Screen("/lazy")
    data object UploadScreen : Screen("/upload")
}