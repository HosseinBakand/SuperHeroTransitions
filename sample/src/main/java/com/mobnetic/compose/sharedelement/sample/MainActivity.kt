package com.mobnetic.compose.sharedelement.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.mobnetic.compose.sharedelement.SharedElement
import com.mobnetic.compose.sharedelement.SharedElementType
import com.mobnetic.compose.sharedelement.SharedElementsRoot

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val from = remember {
                mutableStateOf(SharedElementType.FROM)
            }
            MaterialTheme {
                SharedElementsRoot(from) {

                }
            }
        }
    }

    override fun onBackPressed() {

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
