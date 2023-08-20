package com.hpn.hmessager.ui.composable

import android.media.MediaPlayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.hpn.hmessager.R
import com.hpn.hmessager.bl.conversation.MediaType
import com.hpn.hmessager.bl.conversation.Message
import com.hpn.hmessager.bl.conversation.MessageType
import com.hpn.hmessager.bl.user.LocalUser
import com.hpn.hmessager.ui.utils.hideSystemBars
import com.hpn.hmessager.ui.utils.showSystemBars

@Composable
fun DrawMsg(message: Message) {
    when (message.type) {
        MessageType.ONLY_EMOJI -> DrawEmojiOnlyMsg(message)
        MessageType.TEXT -> DrawTextMsg(message)
        MessageType.MEDIA -> DrawMedia(message)

        else -> {
            DrawStandardMsg(message) {
                Text(text = "Not implemented yet", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun DrawMedia(message: Message) {
    val attachment = message.mediaAttachment

    when (attachment.mediaType) {
        MediaType.IMAGE -> DrawImageMsg(message)
        MediaType.VIDEO -> DrawStandardMsg(message) {
            Text(text = "Wesh mon reuf c'est une vidéo", color = MaterialTheme.colorScheme.error)
        }

        MediaType.AUDIO -> DrawStandardMsg(message) {
            Text(text = "Wesh mon reuf c'est un audio", color = MaterialTheme.colorScheme.error)
        }

        MediaType.DOCUMENT -> DrawDocumentMsg(message)

        else -> {
            DrawStandardMsg(message) {
                Text(text = "Not implemented yet", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DrawStandardMsg(
    message: Message,
    modifier: Modifier = Modifier,
    padding: Int = 15,
    drawBox: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val you = message.metadata.user is LocalUser

    Row(
        modifier = Modifier.fillMaxWidth(if (you) 1.0f else 0.7f),
        horizontalArrangement = if (you) Arrangement.End else Arrangement.Start
    ) {
        Spacer(modifier = Modifier.width(10.dp))

        if (drawBox) {
            val color =
                if (you) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer

            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(color)
                    .padding(padding.dp)
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
private fun DrawDocumentMsg(message: Message) {
    val name = message.mediaAttachment.name
    var col =
        if (message.metadata.user is LocalUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    col = col.copy(green = col.green * 0.85f, red = col.red * 0.85f, blue = col.blue * 0.85f)

    DrawStandardMsg(message, padding = 5) {
        Row(
            Modifier
                .clip(RoundedCornerShape(15.dp))
                .background(col)
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.document_24dp),
                contentDescription = name
            )

            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSecondary,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(0.7f)
            )



            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    painter = painterResource(id = R.drawable.file_download_black_24dp),
                    contentDescription = "Download file"
                )
            }
        }
    }
}

@Composable
private fun DrawAudioMessage(message: Message) {
}

@Composable
private fun DrawEmojiOnlyMsg(message: Message) {
    DrawStandardMsg(message, drawBox = false) {
        Text(
            text = message.data, fontSize = 50.sp, lineHeight = 60.sp,
        )
    }
}

@Composable
private fun DrawTextMsg(message: Message) {
    DrawStandardMsg(message) {
        Text(
            text = message.data, color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DrawImageMsg(message: Message) {
    DrawStandardMsg(message, padding = 4) {
        val you = message.metadata.user is LocalUser
        val media = message.mediaAttachment

        var popupFullScreen by remember { mutableStateOf(false) }

        AsyncImage(
            model = media.data,
            contentDescription = "Image",
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .fillMaxWidth(0.65f)
                .aspectRatio(3 / 4f)
                .clickable { popupFullScreen = true },
            contentScale = ContentScale.Crop,
            alignment = if (you) Alignment.CenterEnd else Alignment.CenterStart
        )

        val view = LocalView.current

        var scale by remember { mutableStateOf(1f) }
        var scaleTarget by remember { mutableStateOf(0f) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }
        var size by remember { mutableStateOf(IntSize.Zero) }

        LaunchedEffect(popupFullScreen) {
            if (popupFullScreen) {
                scaleTarget = 1f
                hideSystemBars(view)
            } else {
                scaleTarget = 0f
                showSystemBars(view)
            }
        }

        if (popupFullScreen) {
            Dialog(
                onDismissRequest = { popupFullScreen = false }, properties = DialogProperties(
                    usePlatformDefaultWidth = false, decorFitsSystemWindows = false
                )
            ) {
                var alphaTargetMenu by remember { mutableStateOf(0f) }
                val alphaAnimation by animateFloatAsState(
                    targetValue = alphaTargetMenu, animationSpec = tween(
                        durationMillis = 600, easing = LinearOutSlowInEasing
                    ), label = "imageAlpha"
                )

                val scaleAnimation by animateFloatAsState(
                    targetValue = scaleTarget, animationSpec = tween(
                        durationMillis = 250, easing = FastOutSlowInEasing
                    ), label = "imageScale"
                )

                Scaffold(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                    containerColor = Color.Black,
                    topBar = {
                        TopAppBar(
                            title = { Text(text = media.name) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Black.copy(
                                    alpha = 0.5f
                                )
                            ),
                            modifier = Modifier.alpha(alphaAnimation),
                            navigationIcon = {
                                // Back button
                                IconButton(onClick = { popupFullScreen = false }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            actions = {
                                // More actions icon
                                IconButton(onClick = { }) {
                                    Icon(
                                        painterResource(id = R.drawable.file_download_black_24dp),
                                        contentDescription = "Download"
                                    )
                                }
                            },

                            )
                    }) {
                    AsyncImage(model = media.data,
                        contentDescription = "Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it.calculateBottomPadding())
                            .onSizeChanged { s -> size = s }
                            .combinedClickable(onClick = {
                                alphaTargetMenu = if (alphaTargetMenu == 0f) 1f else 0f
                            },
                                onDoubleClick = {
                                    if (scale == 1f) scale = 2f
                                    else {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() })
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = maxOf(1f, minOf(scale * zoom, 3f))
                                    val maxX = (size.width * (scale - 1)) / 2
                                    val minX = -maxX
                                    offsetX = maxOf(
                                        minX, minOf(maxX, offsetX + pan.x)
                                    )
                                    val maxY = (size.height * (scale - 1)) / 2
                                    val minY = -maxY
                                    offsetY = maxOf(
                                        minY, minOf(maxY, offsetY + pan.y)
                                    )
                                }
                            }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .scale(scaleAnimation),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center)
                }
            }
        }
    }
}