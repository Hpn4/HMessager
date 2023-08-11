package com.hpn.hmessager.ui.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.hpn.hmessager.R
import com.hpn.hmessager.bl.conversation.Conversation
import com.hpn.hmessager.bl.conversation.Conversations
import com.hpn.hmessager.bl.conversation.Message
import com.hpn.hmessager.bl.conversation.MessageType
import com.hpn.hmessager.bl.io.ConversationStorage
import com.hpn.hmessager.bl.io.PaquetManager
import com.hpn.hmessager.bl.io.StorageManager
import com.hpn.hmessager.bl.user.LocalUser
import com.hpn.hmessager.ui.composable.ConvTopBar
import com.hpn.hmessager.ui.theme.HMessagerTheme
import java.io.DataInputStream
import java.io.File
import java.io.IOException


class ConvActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Authorize to draw on system UI
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Setup storage manager and get/load conversation
        val storage = StorageManager(this, intent)
        val user = storage.loadLocalUser()
        val convId = intent.getIntExtra("convId", 0)
        val conv = Conversations.getOrLoadConversation(convId, storage, user)
        conv.setConversationStorage(ConversationStorage(conv, storage))

        setContent {
            val msg = remember { mutableStateListOf<Message>() }

            conv.initConv(msg)

            Conv(msg, conv) {
                // Close connection
                PaquetManager.close()
                storage.storeConversation(conv)

                // Switch to home screen
                val int = Intent(this, ConvListActivity::class.java)
                storage.saveRootKeyIntent(int)

                startActivity(int)
            }
        }
    }

    @Composable
    fun Conv(
        msg: SnapshotStateList<Message>,
        conv: Conversation,
        onBackButton: () -> Unit,
    ) {
        BackHandler {
            onBackButton()
        }

        HMessagerTheme {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
            ) {

                Scaffold(topBar = { ConvTopBar("conv.remoteUser.name", onBackButton) }, content = {
                    val lazyList = rememberLazyListState()

                    LaunchedEffect(msg.size) {
                        lazyList.animateScrollToItem(msg.size)
                    }
                    LaunchedEffect(lazyList.canScrollBackward) {
                        if (!lazyList.canScrollBackward) conv.seePreviousMessages(10)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .padding(
                                top = it.calculateTopPadding(),
                                bottom = it.calculateBottomPadding() - 42.dp
                            )
                            .fillMaxSize(),
                        state = lazyList,
                        contentPadding = PaddingValues(10.dp, 20.dp, 10.dp, 10.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        items(msg) { message ->
                            DrawMsg(message)
                        }
                    }
                }, bottomBar = {
                    BottomBar { text, uris, type ->
                        if (type == MessageType.TEXT) conv.sendText(text)
                        else conv.sendMedias(uris, type)
                    }
                })
            }
        }
    }

    @Composable
    fun DrawMsg(message: Message) {
        val you = message.user is LocalUser

        val color =
            if (you) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
        val isPhoto = message.type == MessageType.IMAGE

        Row(
            modifier = Modifier.fillMaxWidth(if (you) 1.0f else 0.7f),
            horizontalArrangement = if (you) Arrangement.End else Arrangement.Start
        ) {
            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(color)
                    .padding(if (isPhoto) 4.dp else 15.dp)
            ) {

                when (message.type) {
                    MessageType.TEXT -> Text(
                        text = message.data, color = MaterialTheme.colorScheme.onPrimary
                    )

                    MessageType.IMAGE -> AsyncImage(
                        model = message.dataBytes,
                        contentDescription = "Image",
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .fillMaxWidth(0.65f),
                        contentScale = ContentScale.FillWidth,
                        alignment = if (you) Alignment.CenterEnd else Alignment.CenterStart
                    )

                    MessageType.UNKNOWN -> Text(text = "Document", color = Color.Red)

                    else -> Text("Flemme de coder", color = Color.Red)
                }
            }
        }

    }

    @Composable
    fun BottomBar(onSend: (String, List<ByteArray>, MessageType) -> Unit) {
        var message by remember { mutableStateOf("") }
        var attach by remember { mutableStateOf(false) }
        var type by remember { mutableStateOf(MessageType.TEXT) }
        val scroll = rememberScrollState()

        var uris by remember { mutableStateOf<List<ByteArray>>(emptyList()) }
        var tmpUriPicture by remember { mutableStateOf<Uri?>(null) }

        val context = LocalContext.current
        val onChosen: (List<Uri>, MessageType) -> Unit = { medias, t ->
            uris = readMedias(context, medias)
            type = t
            attach = false
        }

        val takePicture = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
            onResult = { uri ->
                if (uri && tmpUriPicture != null) onChosen.invoke(
                    listOf(tmpUriPicture as Uri), type
                )
            })

        val onTakePicture: () -> Unit = {
            val file = File(context.cacheDir, "tmp_picture.jpg")
            file.createNewFile()

            tmpUriPicture = FileProvider.getUriForFile(
                context, "com.hpn.hmessager.ui.activity.ConvActivity.provider",
                file
            )
            type = MessageType.IMAGE
            takePicture.launch(tmpUriPicture)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .background(Color.Transparent),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .background(Color.Transparent)
            ) {

                TextField(
                    value = message,
                    onValueChange = { message = it },
                    maxLines = 6,
                    trailingIcon = { Spacer(Modifier.padding(horizontal = 20.dp)) },
                    modifier = Modifier
                        .verticalScroll(scroll)
                        .fillMaxWidth(),
                    placeholder = { Text(text = "Message") },
                    shape = RoundedCornerShape(25.dp),
                    colors = TextFieldDefaults.colors(
                        cursorColor = MaterialTheme.colorScheme.primary,

                        focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimaryContainer,

                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,

                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,

                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = (-5).dp),
                    horizontalArrangement = Arrangement.spacedBy((-15).dp)
                ) {

                    // Put file
                    IconButton(onClick = {
                        attach = true
                    }) {
                        Icon(
                            modifier = Modifier.rotate(-45f),
                            painter = painterResource(id = R.drawable.attach_file_black_24dp),
                            contentDescription = "Attach document",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Take picture/video
                    if (message.isBlank()) {
                        IconButton(onClick = { onTakePicture() }) {
                            Icon(
                                painterResource(id = R.drawable.camera_svgrepo),
                                contentDescription = "Take picture",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            val (id, description, onClick: () -> Unit) = if (message.isBlank() && uris.isEmpty()) {
                Triple(
                    R.drawable.microphone_svgrepo_com, "Microphone"
                ) {}
            } else {
                Triple(
                    R.drawable.send_black_24dp, "Send"
                ) {
                    onSend(message, uris, type)
                    uris = emptyList()
                    type = MessageType.TEXT
                    message = ""
                }
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(3.dp)
            ) {
                Icon(
                    painter = painterResource(id = id),
                    description,
                    Modifier.size(30.dp),
                    MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (attach) {
            val padValue = WindowInsets.Companion.ime.exclude(WindowInsets.Companion.navigationBars)
                .asPaddingValues()

            Popup(alignment = Alignment.BottomStart,
                offset = IntOffset(0, 0),
                onDismissRequest = { attach = false }) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .height(padValue.calculateBottomPadding())
                        .fillMaxWidth()
                ) {
                    AttachmentMenu(Modifier.align(Alignment.Center), onChosen, onTakePicture)
                }
            }
        }
    }

    private fun readMedias(
        context: Context, uris: List<Uri>,
    ): List<ByteArray> {
        val datas = mutableListOf<ByteArray>()
        for (media in uris) {
            val data: ByteArray

            // Read
            try {
                val inputStream = context.contentResolver.openInputStream(media)
                val bis = DataInputStream(inputStream)
                data = bis.readBytes()

                datas.add(data)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return datas
    }

    @Composable
    fun AttachmentMenu(
        modifier: Modifier,
        onChosen: (List<Uri>, MessageType) -> Unit,
        onTakePicture: () -> Unit,
    ) {
        var type by remember { mutableStateOf(MessageType.IMAGE) }
        val multipleMediasPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(),
            onResult = { uris ->
                onChosen(uris, type)
            })
        val multipleDocsPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents(),
            onResult = { uris ->
                onChosen(uris, type)
            })

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 80.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AttachmentItem(name = "Document", id = R.drawable.document_24dp, color = Color.Blue) {
                type = MessageType.UNKNOWN
                multipleDocsPicker.launch("*/*")
            }

            AttachmentItem(name = "Picture", id = R.drawable.picture_24dp, color = Color.Magenta) {
                type = MessageType.IMAGE
                multipleMediasPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            AttachmentItem(
                name = "Camera",
                id = R.drawable.camera_svgrepo,
                color = Color.Green,
                onClick = onTakePicture
            )

            AttachmentItem(name = "Audio", id = R.drawable.audio_24dp, color = Color.Red) {
                type = MessageType.AUDIO
                multipleDocsPicker.launch("audio/*")
            }

            AttachmentItem(
                name = "Video", id = R.drawable.videocam_black_24dp, color = Color.Cyan
            ) {
                type = MessageType.VIDEO
                multipleMediasPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            }
        }
    }

    @Composable
    fun AttachmentItem(name: String, id: Int, color: Color, onClick: () -> Unit = {}) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(color)
                    .padding(3.dp)
            ) {
                Icon(
                    painter = painterResource(id = id),
                    name,
                    Modifier.size(30.dp),
                    MaterialTheme.colorScheme.onPrimary
                )
            }

            Text(text = name, color = MaterialTheme.colorScheme.onSecondary)
        }
    }

}