package com.hpn.hmessager.presentation.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hpn.hmessager.data.model.Preference
import com.hpn.hmessager.data.repository.StorageManager
import com.hpn.hmessager.data.model.user.Config
import com.hpn.hmessager.presentation.composable.HButton
import com.hpn.hmessager.presentation.composable.SimpleTextField
import com.hpn.hmessager.presentation.composable.getTextFieldColors
import com.hpn.hmessager.presentation.theme.HMessagerTheme
import com.hpn.hmessager.presentation.theme.ThemesName

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storage =
            StorageManager(this, intent)
        val act = this
        val user = storage.loadLocalUser()

        setContent {
            HMessagerTheme(pref = storage.loadPreference()) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    var ipA by remember { mutableStateOf(user.config.host[0].toString()) }
                    var ipB by remember { mutableStateOf(user.config.host[1].toString()) }
                    var ipC by remember { mutableStateOf(user.config.host[2].toString()) }
                    var ipD by remember { mutableStateOf(user.config.host[3].toString()) }
                    var port by remember { mutableStateOf(user.config.port.toString()) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "Server IP: ",
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .padding(end = 15.dp)
                                )

                                SimpleTextField(
                                    text = ipA,
                                    onTextChanged = { ipA = it },
                                    modifier = Modifier.weight(0.5f)
                                )

                                Text(text = ".")

                                SimpleTextField(
                                    text = ipB,
                                    onTextChanged = { ipB = it },
                                    modifier = Modifier.weight(0.5f)
                                )

                                Text(text = ".")

                                SimpleTextField(
                                    text = ipC,
                                    onTextChanged = { ipC = it },
                                    modifier = Modifier.weight(0.5f)
                                )

                                Text(text = ".")

                                SimpleTextField(
                                    text = ipD,
                                    onTextChanged = { ipD = it },
                                    modifier = Modifier.weight(0.5f)
                                )
                            }

                            Row {
                                Text(text = "Server port: ", modifier = Modifier.padding(16.dp))

                                SimpleTextField(text = port, onTextChanged = { port = it })
                            }

                            Row {
                                Text(text = "Theme: ", modifier = Modifier.padding(16.dp).padding(end = 30.dp))

                                ThemeComboBox(storage)
                            }
                        }

                        HButton(
                            onClick = {
                                val c =
                                    Config()
                                c.setHost(ipA.toByte(), ipB.toByte(), ipC.toByte(), ipD.toByte())
                                c.port = port.toInt()

                                user.config = c

                                storage.storeLocalUser(user)

                                val intent = Intent(act, ConvListActivity::class.java)
                                storage.saveRootKeyIntent(intent)

                                startActivity(intent)
                            },
                            text = "Save",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ThemeComboBox(storage: StorageManager) {
        // state of the menu
        var expanded by remember {
            mutableStateOf(false)
        }

        // remember the selected item
        var pref = storage.loadPreference()
        var selectedItem by remember {
            mutableStateOf(ThemesName[pref?.themeId ?: 0])
        }

        // box
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {
            expanded = it
        }) {
            TextField(
                value = selectedItem,
                onValueChange = {},
                readOnly = true,
                label = { Text(text = "Theme") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                },
                colors = getTextFieldColors(),
                modifier = Modifier.menuAnchor(),
                shape = RoundedCornerShape(20)
            )

            // menu
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {

                ThemesName.forEach { selectedOption ->
                    // menu item
                    DropdownMenuItem(text = { Text(text = selectedOption) }, onClick = {
                        selectedItem = selectedOption
                        expanded = false

                        val themeId = ThemesName.indexOf(selectedOption)
                        pref = Preference()
                        pref.themeId = themeId

                        storage.storePreference(pref)
                    })
                }
            }
        }
    }
}