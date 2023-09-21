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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import com.hpn.hmessager.R
import com.hpn.hmessager.bl.conversation.Conversation
import com.hpn.hmessager.bl.conversation.Conversations
import com.hpn.hmessager.bl.conversation.message.MediaType
import com.hpn.hmessager.bl.conversation.message.Message
import com.hpn.hmessager.bl.io.ConversationStorage
import com.hpn.hmessager.bl.io.PaquetManager
import com.hpn.hmessager.bl.io.StorageManager
import com.hpn.hmessager.bl.utils.MediaHelper
import com.hpn.hmessager.ui.composable.ConvTopBar
import com.hpn.hmessager.ui.composable.DrawMsg
import com.hpn.hmessager.ui.composable.MessageTextField
import com.hpn.hmessager.ui.composable.releasePlayer
import com.hpn.hmessager.ui.theme.HMessagerTheme
import java.io.File


class ConvActivity : ComponentActivity() {

    private var conv: Conversation? = null

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
        conv.conversationStorage = ConversationStorage(conv, storage)

        this.conv = conv

        setContent {
            val msg = remember { mutableStateListOf<Message>() }

            Conv(msg, conv) {
                // Close connection
                PaquetManager.close()
                storage.storeConversation(conv)

                // Switch to home screen
                val int = Intent(this, ConvListActivity::class.java)
                storage.saveRootKeyIntent(int)

                startActivity(int)
            }

            DisposableEffect(Unit) {
                onDispose {
                    releasePlayer()
                    MediaHelper.release()
                    PaquetManager.close()
                    storage.storeConversation(conv)
                }
            }
        }
    }

    @Composable
    fun Conv(
        msg: SnapshotStateList<Message>,
        conv: Conversation,
        onBackButton: () -> Unit,
    ) {
        val context = LocalContext.current
        BackHandler {
            onBackButton()
        }

        HMessagerTheme {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
            ) {
                Box {
                    var scrollToBottom by remember { mutableStateOf(false) }
                    Scaffold(topBar = { ConvTopBar("conv.remoteUser.name", onBackButton) },
                        content = {
                            val lazyList = rememberLazyListState()

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

                            LaunchedEffect(Unit) {
                                conv.initConv(context, msg)
                                scrollToBottom = true
                            }

                            LaunchedEffect(remember { derivedStateOf { lazyList.firstVisibleItemIndex <= 3 } }) {
                                if (lazyList.firstVisibleItemIndex <= 3) {
                                    for (i in 0..20) {
                                        if (conv.seePreviousMessages()) lazyList.scrollToItem(
                                            lazyList.firstVisibleItemIndex + 1
                                        )
                                        else break
                                    }
                                }
                            }

                            LaunchedEffect(scrollToBottom) {
                                if (scrollToBottom) {
                                    if (msg.isNotEmpty()) lazyList.scrollToItem(msg.size - 1)
                                    scrollToBottom = false
                                }
                            }

                        },
                        bottomBar = {
                            BottomBar { text, uris ->
                                if (uris.isEmpty()) conv.sendText(text)
                                else conv.sendMedias(uris)
                                scrollToBottom = true
                            }
                        })
                }
            }
        }
    }

    @Composable
    fun BottomBar(onSend: (String, List<Uri>) -> Unit) {
        var message by remember { mutableStateOf("") }
        var attach by remember { mutableStateOf(false) }

        var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
        var tmpUriPicture by remember { mutableStateOf<Uri?>(null) }

        val context = LocalContext.current
        val onChosen: (List<Uri>) -> Unit = { medias ->
            uris += medias
            attach = false
        }

        val takePicture = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
            onResult = { uri ->
                if (uri && tmpUriPicture != null) onChosen(listOf(tmpUriPicture as Uri))
            })

        val onTakePicture: () -> Unit = {
            tmpUriPicture = getUri(context, MediaType.IMAGE)

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
                MessageTextField(text = message, onTextChanged = { message = it })

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = (-5).dp),
                    horizontalArrangement = Arrangement.spacedBy((-15).dp)
                ) {

                    // Put file
                    var buttonAttachmentClick by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                        buttonAttachmentClick = !buttonAttachmentClick
                        attach = buttonAttachmentClick
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
                    onSend(message, uris)
                    uris = emptyList()
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

        // Attachment menu popup
        val padValue = WindowInsets.Companion.ime.exclude(WindowInsets.Companion.navigationBars)
            .asPaddingValues()
        var bottomPadding = padValue.calculateBottomPadding()
        val isOnKeyboard = bottomPadding > 5.dp
        var yOffset = 0

        if (!isOnKeyboard) {
            bottomPadding = 220.dp
            yOffset = -300
        }

        Popup(alignment = Alignment.BottomStart,
            offset = IntOffset(0, yOffset),
            onDismissRequest = { attach = false }) {
            AnimatedVisibility(visible = attach) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = if (isOnKeyboard) 0.dp else 10.dp)
                        .height(bottomPadding)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(if (isOnKeyboard) 0.dp else 20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    AttachmentMenu(
                        Modifier.align(Alignment.Center), isOnKeyboard, onChosen, onTakePicture
                    )
                }
            }
        }
    }

    @Composable
    private fun AttachmentMenu(
        modifier: Modifier,
        onKeyboard: Boolean = true,
        onChosen: (List<Uri>) -> Unit,
        onTakePicture: () -> Unit,
    ) {
        var tmpUriVideo by remember { mutableStateOf<Uri?>(null) }
        val context = LocalContext.current

        val multipleMediasPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(),
            onResult = { uris ->
                persistentUri(context, uris)
                onChosen(uris)
            })
        val multipleDocsPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments(),
            onResult = { uris ->
                persistentUri(context, uris)
                onChosen(uris)
            })

        val takeVideo = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CaptureVideo(),
            onResult = { uri ->
                if (uri && tmpUriVideo != null) onChosen(
                    listOf(tmpUriVideo as Uri)
                )
            })


        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalArrangement = Arrangement.Center,
            modifier = modifier
                .fillMaxSize()
                .padding(top = if (onKeyboard) 50.dp else 25.dp)
        ) {
            item {
                AttachmentItem(
                    name = "Picture", id = R.drawable.picture_24dp, color = Color.Magenta
                ) {
                    multipleMediasPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }

            item {
                AttachmentItem(
                    name = "Video", id = R.drawable.movie_black_24dp, color = Color.Cyan
                ) {
                    multipleMediasPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                }
            }

            item {
                AttachmentItem(name = "Audio", id = R.drawable.audio_24dp, color = Color.Red) {
                    multipleDocsPicker.launch(arrayOf("audio/*"))
                }
            }

            item {
                AttachmentItem(
                    name = "Document", id = R.drawable.document_24dp, color = Color.Blue
                ) {
                    multipleDocsPicker.launch(arrayOf("*/*"))
                }
            }

            item {
                AttachmentItem(
                    name = "Take picture",
                    id = R.drawable.camera_svgrepo,
                    color = Color.Green,
                    onClick = onTakePicture
                )
            }

            item {
                AttachmentItem(
                    name = "Take video",
                    id = R.drawable.videocam_black_24dp,
                    color = Color.LightGray
                ) {
                    tmpUriVideo = getUri(context, MediaType.VIDEO)

                    takeVideo.launch(tmpUriVideo)
                }
            }
        }
    }

    @Composable
    private fun AttachmentItem(name: String, id: Int, color: Color, onClick: () -> Unit = {}) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val darker1 =
                Color(color.red * 0.85f, color.green * 0.85f, color.blue * 0.85f, color.alpha)
            val darker =
                Color(color.red * 0.75f, color.green * 0.75f, color.blue * 0.75f, color.alpha)

            IconButton(
                onClick = onClick, modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(darker1, darker), startY = 50f, endY = 80f
                        )
                    )
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

    private fun persistentUri(context: Context, uris: List<Uri>) {
        for (uri in uris) {
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flag)
        }
    }

    private fun getUri(context: Context, mediaType: MediaType, name: String? = null): Uri {
        val storage = conv?.conversationStorage as ConversationStorage
        val file =
            if (name.isNullOrEmpty()) storage.getMediaFile(mediaType) else storage.getMediaFile(
                name, mediaType
            ) as File
        file.createNewFile()

        return FileProvider.getUriForFile(
            context, "com.hpn.hmessager.ui.activity.ConvActivity.provider", file
        )
    }
}