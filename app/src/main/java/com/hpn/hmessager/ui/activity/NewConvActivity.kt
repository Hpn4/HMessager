package com.hpn.hmessager.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hpn.hmessager.bl.crypto.KeyUtils
import com.hpn.hmessager.bl.crypto.X3DH
import com.hpn.hmessager.bl.io.StorageManager
import com.hpn.hmessager.ui.composable.HButton
import com.hpn.hmessager.ui.composable.TopBar
import com.hpn.hmessager.ui.composable.qr.Barcode
import com.hpn.hmessager.ui.composable.qr.BarcodeType
import com.hpn.hmessager.ui.composable.qr.QRScanner
import com.hpn.hmessager.ui.theme.HMessagerTheme

class NewConvActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storage = StorageManager(this, intent)
        val localUser = storage.loadLocalUser()
        val auth = X3DH(localUser, KeyUtils.generateX25519KeyPair())
        val act = this

        setContent {
            HMessagerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = { TopBar(text = "New conversation") },
                        content = {
                            var isScanned by remember { mutableStateOf(false) }
                            var isScanMode by remember { mutableStateOf(false) }
                            var qrCodeLocal by remember { mutableStateOf(auth.generateQrCode(true)) }
                            var qrCodeRemote by remember { mutableStateOf("") }

                            Box(
                                modifier = Modifier
                                    .padding(it)
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                            ) {

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (isScanMode) {
                                        QRScanner(
                                            onQrCodeScanned = { code ->
                                                if (!isScanned) {
                                                    qrCodeRemote = code
                                                    isScanned = true
                                                }
                                            },
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .width(400.dp)
                                                .height(400.dp)
                                                .clip(RoundedCornerShape(5))
                                        )

                                        Spacer(modifier = Modifier.padding(40.dp))

                                        HButton(
                                            onClick = {
                                                isScanMode = false
                                                qrCodeLocal = auth.generateQrCode(!isScanned)
                                            },
                                            text = "Code",
                                            modifier = Modifier.padding(horizontal = 20.dp)
                                        )
                                    } else {
                                        Barcode(
                                            type = BarcodeType.QR_CODE,
                                            value = qrCodeLocal,
                                            width = 400.dp,
                                            height = 400.dp,
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .clip(RoundedCornerShape(5))
                                                .background(MaterialTheme.colorScheme.onPrimary)
                                                .width(400.dp)
                                                .height(400.dp)
                                        )


                                        Spacer(modifier = Modifier.padding(40.dp))

                                        HButton(
                                            onClick = {
                                                isScanMode = true
                                            },
                                            text = "Scan",
                                            modifier = Modifier.padding(horizontal = 20.dp)
                                        )
                                    }

                                    if (isScanned) {
                                        Text(
                                            text = "Code correctly scanned",
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }

                                HButton(
                                    onClick = {
                                        val conv = auth.generateConversation(
                                            qrCodeRemote,
                                            storage
                                        )

                                        val intent = Intent(act, ConvActivity::class.java)
                                        storage.saveRootKeyIntent(intent)
                                        intent.putExtra("convId", conv.convId)

                                        startActivity(intent)
                                    },
                                    text = "Continue",
                                    enabled = isScanned,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(16.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}