package com.hpn.hmessager.presentation.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hpn.hmessager.R
import com.hpn.hmessager.data.repository.StorageManager
import com.hpn.hmessager.presentation.composable.ConfirmPasswordTextField
import com.hpn.hmessager.presentation.composable.DialogScreen
import com.hpn.hmessager.presentation.composable.PasswordTextField
import com.hpn.hmessager.presentation.theme.HMessagerTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = StorageManager(this)

        setContent {
            HMessagerTheme(pref = storage.loadPreference()) {
                Surface(
                    color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()
                ) {
                    if (storage.isFirstLaunch) {
                        DialogFirstScreen { password ->
                            storage.setup(password)
                            val localUser = storage.createLocalUser()
                            storage.storeLocalUser(localUser)

                            val intent = Intent(this, ConvListActivity::class.java)
                            storage.saveRootKeyIntent(intent)
                            startActivity(intent)
                        }
                    } else {
                        DialogLaunchScreen(storage = storage, onClick = { password ->
                            val intent = Intent(this, ConvListActivity::class.java)
                            storage.setup(password)
                            storage.saveRootKeyIntent(intent)
                            startActivity(intent)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun DialogLaunchScreen(storage: StorageManager, onClick: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    DialogScreen(
        title = "Enter your password: ",
        spacedBy = 5,
        buttonOnClick = {
            if (password.isNotEmpty()) {
                storage.setup(password)

                if (storage.loadLocalUser() != null) {
                    onClick(password)
                } else error = true
            } else error = true
        },
    ) {
        Spacer(modifier = Modifier.height(15.dp))

        PasswordTextField(
            text = password, onTextChanged = {
                password = it
                error = false
            }, error = error
        )
    }
}

@Composable
fun DialogFirstScreen(onClick: (String) -> Unit) {
    var isGood by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var cPassword by remember { mutableStateOf("") }

    DialogScreen(spacedBy = 20,
        title = stringResource(id = R.string.first_password_title),
        buttonEnabled = isGood,
        buttonOnClick = { onClick(password) }) {
        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = stringResource(id = R.string.first_password_explanation),
            color = MaterialTheme.colorScheme.onSecondary,
            fontSize = 16.sp
        )

        PasswordTextField(
            text = password,
            onTextChanged = { password = it },
            validateStrengthPassword = true,
            isGood = isGood
        )

        ConfirmPasswordTextField(text = cPassword,
            confirmText = password,
            onTextChanged = { newValue -> cPassword = newValue },
            isGood = isGood,
            onPasswordMatch = { isMatching -> isGood = isMatching })
    }
}
