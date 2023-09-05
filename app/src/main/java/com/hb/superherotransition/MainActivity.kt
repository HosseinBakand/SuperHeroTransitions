package com.hb.superherotransition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hb.superhero.SharedElement
import com.hb.superhero.SharedElementType
import com.hb.superhero.SharedElementsRoot
import com.hb.superherotransition.ui.theme.SuperHeroTransitionTheme

//import com.hb.superhero.SharedElementType
//import com.hb.superhero.SharedElementsRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
//            SuperHeroTransitionTheme {
//                // A surface container using the 'background' color from the theme
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    Greeting("Android")
//                }
//            }
            val from = remember {
                mutableStateOf(SharedElementType.FROM)
            }
            MaterialTheme {
                SharedElementsRoot(from) {
                    when (val selectedUser = viewModel.selectedUser) {
                        null -> UsersListScreen()
                        else -> UserDetailsScreen(selectedUser)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SuperHeroTransitionTheme {
        Greeting("Android")
    }
}

//@Model
class ViewModel(var selectedUser: User? = null)

val viewModel = ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersListScreen() {
    LazyColumn() {
        items(users) { user ->
            ListItem(
                leadingContent = {
                    SharedElement(tag = user, type = SharedElementType.FROM) {
                        Image(
                            painter = painterResource(id = user.avatar),
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.FillBounds,
                            contentDescription = null
                        )
                    }
                },
                headlineText = {
                    SharedElement(tag = user to user.name, type = SharedElementType.FROM) {
                        Text(text = user.name)
                    }
                },
                modifier = Modifier.clickable { viewModel.selectedUser = user },
            )
        }
    }
}

@Composable
fun UserDetailsScreen(user: User) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier
                .clickable { viewModel.selectedUser = null }
                .size(200.dp)

        ) {
            SharedElement(tag = user, type = SharedElementType.TO) {

                Image(
                    painter = painterResource(id = user.avatar),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                    contentDescription = null
                )
            }
        }
        SharedElement(
            tag = user to user.name,
            type = SharedElementType.TO,
            modifier = Modifier
        ) {
            Text(text = user.name, style = MaterialTheme.typography.titleMedium)
        }
    }
}

data class User(@DrawableRes val avatar: Int, val name: String)

val users = listOf(
    User(R.drawable.avatar_1, "Adam"),
    User(R.drawable.avatar_2, "Andrew"),
    User(R.drawable.avatar_3, "Anna"),
    User(R.drawable.avatar_4, "Boris"),
    User(R.drawable.avatar_5, "Carl"),
    User(R.drawable.avatar_6, "Donna"),
    User(R.drawable.avatar_7, "Emily"),
    User(R.drawable.avatar_8, "Fiona"),
    User(R.drawable.avatar_9, "Grace"),
    User(R.drawable.avatar_10, "Irene"),
    User(R.drawable.avatar_11, "Jack"),
    User(R.drawable.avatar_12, "Jake"),
    User(R.drawable.avatar_13, "Mary"),
    User(R.drawable.avatar_14, "Peter"),
    User(R.drawable.avatar_15, "Rose"),
    User(R.drawable.avatar_16, "Victor")
)