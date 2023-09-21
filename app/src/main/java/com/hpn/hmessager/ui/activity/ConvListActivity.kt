@file:RequiresApi(Build.VERSION_CODES.O)

package com.hpn.hmessager.ui.activity

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.hpn.hmessager.R
import com.hpn.hmessager.bl.conversation.ConvMetadata
import com.hpn.hmessager.bl.io.StorageManager
import com.hpn.hmessager.bl.utils.Utils
import com.hpn.hmessager.ui.composable.ConvListTopBar
import com.hpn.hmessager.ui.composable.HButton
import com.hpn.hmessager.ui.theme.HMessagerTheme

class ConvListActivity : ComponentActivity() {

    private fun switchActivity(
        cls: Class<*>,
        storage: StorageManager,
        convId: Int? = null,
    ): () -> Unit = {
        val intent = Intent(this, cls)
        storage.saveRootKeyIntent(intent)

        if (convId != null) {
            intent.putExtra("convId", convId)
        }

        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load a storage manager
        val storage = StorageManager(this, intent)

        setContent {
            val convs = remember { mutableStateListOf<ConvMetadata>() }

            // Load conversations
            LaunchedEffect(Unit) {
                convs.addAll(storage.conversations.sortedBy { it.lastMessageDate })
                //TODO: Setup websocket
            }

            // Request notification permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = {})

                LaunchedEffect(Unit) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            HMessagerTheme {
                DrawConvListScreen(convs = convs,
                    onSettingsClick = switchActivity(SettingsActivity::class.java, storage),
                    onNewConvClick = switchActivity(NewConvActivity::class.java, storage),
                    onOpenConvClick = {
                        switchActivity(ConvActivity::class.java, storage, it)()
                    }) {
                    if (storage.deleteConversation(it)) convs.removeIf { conv -> conv.convId == it }
                }
            }
        }
    }
}

@Composable
fun DrawConvListScreen(
    convs: SnapshotStateList<ConvMetadata>,
    onSettingsClick: () -> Unit,
    onNewConvClick: () -> Unit,
    onOpenConvClick: (Int) -> Unit,
    onDeleteConvClick: (Int) -> Unit,
) {
    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(topBar = { ConvListTopBar(text = "HMessager", onSettingsClick) },
            floatingActionButton = { NewConvButton(onNewConvClick) }) {
            DrawConversations(
                convs, Modifier.padding(it), onOpenConvClick, onDeleteConvClick
            )
        }
    }
}

@Composable
private fun NewConvButton(onNewConvClick: () -> Unit) {
    FloatingActionButton(
        onClick = onNewConvClick, containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            painter = painterResource(id = R.drawable.message_circle_lines),
            tint = MaterialTheme.colorScheme.onPrimary,
            contentDescription = "New conversation"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawConversations(
    convs: SnapshotStateList<ConvMetadata>,
    modifier: Modifier = Modifier,
    onClick: (Int) -> Unit,
    onDeleteConvClick: (Int) -> Unit,
) {
    var convIdDelete by remember { mutableIntStateOf(-1) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(top = 5.dp, bottom = 5.dp),
    ) {
        items(convs) { conv: ConvMetadata ->
            ConversationSwipeAction({
                if (it == DismissValue.DismissedToStart) {
                    convIdDelete = conv.convId
                    false
                } else {
                    if (conv.unreadCount < 0) {
                        conv.unreadCount = 0

                    } else conv.unreadCount = 1
                    false
                }
            }) {
                DrawConversation(conv) { onClick(conv.convId) }
            }
        }
    }

    if (convIdDelete >= 0) {
        DeleteConvDialog(dismissRequest = { convIdDelete = -1 }, buttonOnClick = {
            onDeleteConvClick(convIdDelete)
            convIdDelete = -1
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationSwipeAction(
    onConfirmValueChange: (DismissValue) -> Boolean = { true },
    content: @Composable RowScope.() -> Unit,
) {
    val state = rememberDismissState(
        confirmValueChange = onConfirmValueChange,
        positionalThreshold = { 200.dp.toPx() })

    SwipeToDismiss(state = state, background = {
        if (state.dismissDirection == DismissDirection.EndToStart) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red)
                    .padding(end = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete conversation",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(start = 10.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Mark as read",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }, dismissContent = content)
}

@Composable
private fun DrawConversation(
    conv: ConvMetadata,
    onClick: () -> Unit,
) {

    Row(modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.background)
        .clickable { onClick() }
        .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)) {

        val painter = if (conv.avatarUrl == null) {
            painterResource(id = R.drawable.account_circle_black_24dp)
        } else {
            rememberAsyncImagePainter(
                conv.avatarUrl
            )
        }

        Image(
            painter,
            contentDescription = "avatar icon",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )

        // A conversation can be empty and have no messages
        val time =
            if (conv.lastMessageDate == null) "???" else Utils.getDateString(conv.lastMessageDate.toInstant())
        val lastMessage = if (conv.lastMessage == null) " " else conv.lastMessage
        val unread = conv.unreadCount > 0

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = conv.name,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = time,
                    color = if (unread) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = lastMessage,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )

                if (unread) {
                    Text(
                        text = conv.unreadCount.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                            .requiredWidthIn(min = 20.dp, max = 30.dp)
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteConvDialog(dismissRequest: () -> Unit, buttonOnClick: () -> Unit) {
    Dialog(onDismissRequest = dismissRequest) {
        Surface(
            shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Delete conversation?", fontWeight = FontWeight.Bold, fontSize = 18.sp
                )

                Text(
                    text = "All messages and medias (images, videos, documents, audios, sounds, etc.) will be deleted. This action cannot be undone.",
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HButton(
                        text = "No", onClick = dismissRequest
                    )

                    HButton(
                        text = "Yes",
                        containerColor = MaterialTheme.colorScheme.error,
                        onClick = buttonOnClick,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}