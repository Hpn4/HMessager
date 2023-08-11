@file:RequiresApi(Build.VERSION_CODES.O)

package com.hpn.hmessager.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.hpn.hmessager.R
import com.hpn.hmessager.bl.conversation.ConvMetadata
import com.hpn.hmessager.bl.io.StorageManager
import com.hpn.hmessager.bl.utils.Utils
import com.hpn.hmessager.ui.composable.ConvListTopBar
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
            HMessagerTheme {
                DrawConvListScreen(convs = storage.conversations,
                    onSettingsClick = switchActivity(SettingsActivity::class.java, storage),
                    onNewConvClick = switchActivity(NewConvActivity::class.java, storage),
                    onOpenConvClick = {
                        switchActivity(ConvActivity::class.java, storage, it)()
                    })
            }
        }
    }
}

@Composable
fun DrawConvListScreen(
    convs: List<ConvMetadata>,
    onSettingsClick: () -> Unit,
    onNewConvClick: () -> Unit,
    onOpenConvClick: (Int) -> Unit,
) {
    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(topBar = { ConvListTopBar(text = "HMessager", onSettingsClick) }, content = {
            DrawConversations(
                convs = convs, onClick = onOpenConvClick, modifier = Modifier.padding(it)
            )
        }, floatingActionButton = {
            FloatingActionButton(
                onClick = onNewConvClick, containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.message_circle_lines),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    contentDescription = "New conversation"
                )
            }
        })
    }
}

@Composable
fun DrawConversations(
    convs: List<ConvMetadata>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(5.dp),
    ) {
        items(convs) {
            DrawConversation(it) { onClick(it.convId) }
        }
    }
}

@Composable
fun DrawConversation(
    conv: ConvMetadata,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        if (conv.avatarUrl == null) {
            Image(
                Icons.Default.AccountCircle,
                contentDescription = "avatar icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Green)
            )
        } else {
            Image(
                rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current).data(conv.avatarUrl).build()
                ),
                contentDescription = "avatar icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            )
        }

        // A conversation can be empty and have no messages
        val time =
            if (conv.lastMessageDate == null) "???" else Utils.getDateString(conv.lastMessageDate.toInstant())
        val lastMessage = if(conv.lastMessage == null) " " else conv.lastMessage

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
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }

            Text(
                text = lastMessage,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1
            )
        }
    }
}