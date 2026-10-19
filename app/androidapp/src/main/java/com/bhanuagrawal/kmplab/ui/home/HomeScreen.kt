package com.bhanuagrawal.kmplab.ui.home

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhanuagrawal.kmplab.domain.paging.ScrollDirection
import com.bhanuagrawal.kmplab.presentation.home.HomePageEvents
import com.bhanuagrawal.kmplab.presentation.home.model.User

@Composable
fun HomeScreen(modifier: Modifier, viewmodel: HomeViewmodel) {

    val state by viewmodel.state.collectAsStateWithLifecycle()



    MyUserList (
        state.isLoading,
        state.users,
        {
            viewmodel.onEvent(HomePageEvents.RemoveUser(it))
        },
        { viewmodel.onEvent(HomePageEvents.AddUser) },
        onEvent = {
            viewmodel.onEvent(it)
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyUserList(
    isLoading: Boolean,
    users: List<User>,
    onRemove: (User) -> Unit,
    onAdd: () -> Unit,
    onEvent: (HomePageEvents) -> Unit,
) {
    val listState = rememberLazyListState()
    val currentUsers by rememberUpdatedState(users)
    val currentLoading by rememberUpdatedState(isLoading)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()?.index
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val totalItems = layoutInfo.totalItemsCount
            val loading = currentLoading       // ← State<Boolean>, tracked
            val usersSize = currentUsers.size  // ← State<List<User>>, tracked
            Triple(firstVisible to lastVisible, totalItems, loading to usersSize)
        }.collect { (positions, totalItems, meta) ->
            val (firstVisible, lastVisible) = positions
            val (loading, usersSize) = meta

            if (firstVisible == null || lastVisible == null) return@collect

            if (lastVisible >= totalItems - 3) {
                onEvent(HomePageEvents.LoadData(ScrollDirection.Down))
            }
        }
    }


    LazyColumn(state = listState){

        items(
            items = users,
            key = { it.userId }
        ) { user ->

            UserItem(
                user,
                onAdd,
                onRemove,
                modifier = Modifier.animateItem()
            )

        }

        item{
            Button(onClick = onAdd) {
                Text("+")
            }
        }

    }
}

@Composable
fun UserItem(
    user: User,
    onAdd: () -> Unit,
    onRemove: (User) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->

            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemove(user)
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
        }
    ) {
        UserView(user = user)
    }



}


@Composable
fun UserList(
    users: List<User>,
    onRemove: (User) -> Unit,
    onAdd: () -> Unit
) {
    val verticalscrollstate = rememberScrollState()
    Column(modifier = Modifier.verticalScroll(verticalscrollstate)){
        for(user in users){
            key(user.userId) {
                UserItem(user, onAdd, onRemove)
            }
        }

        Box(contentAlignment = Alignment.Center) {
            Button(onClick = onAdd) {
                Text("+")
            }
        }
    }


}

@Composable
fun UserView(modifier: Modifier = Modifier, user: User) {

    SideEffect {
        Log.d("UserView", "${user.username} recomposed")
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(width = 2.dp, color = Color.Gray, shape = RoundedCornerShape(16.dp))
            .padding(16.dp)

    ) {
        Text(text = user.userId)

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = user.username)

    }
}