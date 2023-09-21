package com.hpn.hmessager.ui.composable

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.repeatCount
import com.hpn.hmessager.R
import com.hpn.hmessager.bl.conversation.message.MediaAttachment
import com.hpn.hmessager.bl.conversation.message.MediaType
import com.hpn.hmessager.bl.conversation.message.Message
import com.hpn.hmessager.bl.conversation.message.MessageType
import com.hpn.hmessager.bl.user.LocalUser
import com.hpn.hmessager.ui.utils.hideSystemBars
import com.hpn.hmessager.ui.utils.openWith
import com.hpn.hmessager.ui.utils.showSystemBars
import com.hpn.hmessager.ui.utils.timeToString
import kotlinx.coroutines.delay

var imageLoader: ImageLoader? = null
var player: ExoPlayer? = null

private fun setupPlayer(context: Context) {
    if (player == null) player = ExoPlayer.Builder(context).build()
}

fun releasePlayer() {
    player?.release()
    player = null
}

private fun setupImageLoader(context: Context) {
    if (imageLoader == null) imageLoader = ImageLoader.Builder(context).components {
        add(VideoFrameDecoder.Factory())
        add(SvgDecoder.Factory())
        if (Build.VERSION.SDK_INT >= 28) {
            add(ImageDecoderDecoder.Factory())
        } else {
            add(GifDecoder.Factory())
        }
    }.build()
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
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(getColor(message))
                    .padding(padding.dp),
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
private fun getColor(message: Message, multiplier: Float = 1f): Color {
    val col =
        if (message.metadata.user is LocalUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer

    if (multiplier != 1f) return col.copy(
        green = col.green * multiplier, red = col.red * multiplier, blue = col.blue * multiplier
    )

    return col
}

@Composable
fun DrawMsg(message: Message) {
    when (message.type) {
        MessageType.ONLY_EMOJI -> DrawEmojiOnlyMsg(message)
        MessageType.TEXT -> DrawTextMsg(message)
        MessageType.MEDIA -> DrawMediaMsg(message)

        else -> {
            DrawStandardMsg(message) {
                Text(text = "Not implemented yet", color = MaterialTheme.colorScheme.error)
            }
        }
    }
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

@Composable
fun DrawMediaMsg(message: Message) {
    val attachment = message.mediaAttachment

    when (attachment.mediaType) {
        MediaType.AUDIO -> DrawAudioMsg(message)

        MediaType.DOCUMENT -> DrawDocumentMsg(message)

        MediaType.IMAGE -> DrawImageMsg(message)

        MediaType.GIF -> DrawImageMsg(message, true)

        MediaType.VIDEO -> DrawVideoMsg(message)

        else -> {
            DrawStandardMsg(message) {
                Text(text = "Not implemented yet", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DrawAudioMsg(message: Message) {
    val context = LocalContext.current
    val col = getColor(message, 0.75f)
    val exPlayer = remember {
        setupPlayer(context)
        player as ExoPlayer
    }
    val mediaItem = remember {
        MediaItem.Builder().setUri(message.mediaAttachment.file).setMediaId(message.id.toString())
            .build()
    }

    DrawStandardMsg(message, padding = 1) {
        Row(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(col),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val metadata = message.mediaAttachment.audioMetadata
            var playing by remember { mutableStateOf(false) }

            // Total duration
            val duration by remember { mutableStateOf(timeToString(metadata.duration)) }
            var progress by remember { mutableFloatStateOf(0f) }

            LaunchedEffect(playing) {
                while (playing && exPlayer.mediaItemCount > 0 && exPlayer.getMediaItemAt(0) == mediaItem) {
                    progress = exPlayer.currentPosition.toFloat() / exPlayer.duration.toFloat()
                    delay(100)
                }
            }

            IconButton(onClick = {
                playing = !playing

                if (playing) {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)

                    if (currentVolume == 0) {
                        playing = false
                        Toast.makeText(context, "Increase the volume", Toast.LENGTH_SHORT).show()
                    } else {
                        if (exPlayer.mediaItemCount == 0 || exPlayer.getMediaItemAt(0) != mediaItem) {
                            exPlayer.setMediaItem(mediaItem)
                            exPlayer.prepare()
                            exPlayer.playWhenReady = true
                            exPlayer.seekTo((progress * exPlayer.duration).toLong())
                        } else {
                            exPlayer.seekTo((progress * exPlayer.duration).toLong())
                            exPlayer.play()
                        }
                    }
                } else if (exPlayer.mediaItemCount > 0 && exPlayer.getMediaItemAt(0) == mediaItem) exPlayer.pause()
            }) {
                Icon(
                    painter = painterResource(id = if (playing) R.drawable.pause_black_24dp else R.drawable.play_arrow_black_24dp),
                    contentDescription = "Play/Pause",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(70.dp)
                )
            }

            AudioWaveform(
                amplitudes = metadata.waveform,
                onProgressChange = {
                    progress = it
                    if (exPlayer.mediaItemCount > 0 && exPlayer.getMediaItemAt(0) == mediaItem) {
                        exPlayer.seekTo((it * exPlayer.duration).toLong())
                    }
                },
                modifier = Modifier.fillMaxWidth(0.7f).padding(7.dp),
                waveformAlignment = Alignment.CenterVertically,
                spikePadding = 2.dp,
                progress = progress,
                amplitudeType = AmplitudeType.Max
            )

            Text(
                text = duration,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}

@Composable
private fun DrawDocumentMsg(message: Message) {
    val name = message.mediaAttachment.name
    val col = getColor(message, 0.85f)
    val context = LocalContext.current

    DrawStandardMsg(message, padding = 3) {
        Row(
            Modifier
                .clickable {
                    openWith(context, message.mediaAttachment.file)
                }
                .clip(RoundedCornerShape(20.dp))
                .background(col)
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.document_24dp), contentDescription = name
            )

            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSecondary,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(0.7f)
            )

            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(id = R.drawable.file_download_black_24dp),
                    contentDescription = "Download file"
                )
            }
        }
    }
}

@Composable
private fun DrawImageMsg(message: Message, gif: Boolean = false) {
    val context = LocalContext.current
    setupImageLoader(context)

    DrawStandardMsg(message, padding = 4) {
        val you = message.metadata.user is LocalUser
        val media = message.mediaAttachment

        var popupFullScreen by remember { mutableStateOf(false) }

        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context).repeatCount(5).data(media.file).build(),
            imageLoader = imageLoader!!
        )

        Image(
            painter = painter,
            contentDescription = "Image",
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .fillMaxWidth(0.65f)
                .aspectRatio(if (gif) 1f else (3 / 4f))
                .clickable {
                    popupFullScreen = true
                },
            contentScale = if (gif) ContentScale.FillWidth else ContentScale.Crop,
            alignment = if (you) Alignment.CenterEnd else Alignment.CenterStart
        )

        val view = LocalView.current

        var scaleTarget by remember { mutableFloatStateOf(0f) }

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
                ZoomableFullScreenImage(media, scaleTarget) {
                    popupFullScreen = false
                }
            }
        }
    }
}

@Composable
private fun DrawVideoMsg(message: Message) {
    val context = LocalContext.current
    setupImageLoader(context)

    DrawStandardMsg(message, padding = 4) {
        val you = message.metadata.user is LocalUser
        val media = message.mediaAttachment

        var popupFullScreen by remember { mutableStateOf(false) }
        val exPlayer = remember {
            setupPlayer(context)
            player as ExoPlayer
        }
        val painter = rememberAsyncImagePainter(
            media.file, imageLoader = imageLoader!!
        )

        Box {
            Image(
                painter = painter,
                contentDescription = "Image",
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .fillMaxWidth(0.65f)
                    .aspectRatio(3 / 4f)
                    .clickable {
                        popupFullScreen = true
                        exPlayer.setMediaItem(MediaItem.fromUri(media.file))
                        exPlayer.prepare()
                        exPlayer.playWhenReady = true
                    },
                contentScale = ContentScale.Crop,
                alignment = if (you) Alignment.CenterEnd else Alignment.CenterStart
            )

            Icon(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(10.dp)
                    .align(Alignment.Center),
                painter = painterResource(id = R.drawable.play_arrow_black_24dp),
                contentDescription = "Play video",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        val view = LocalView.current
        var scaleTarget by remember { mutableFloatStateOf(0f) }

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
                onDismissRequest = {
                    popupFullScreen = false
                    exPlayer.pause()
                }, properties = DialogProperties(
                    usePlatformDefaultWidth = false, decorFitsSystemWindows = false
                )
            ) {
                MediaScaffold(title = media.name, visible = true, onBackPress = {
                    exPlayer.pause()
                    popupFullScreen = false
                }, openWith = {
                    exPlayer.pause()
                    openWith(context, media.file)
                }) { pad ->
                    AndroidView(modifier = Modifier
                        .padding(pad)
                        .fillMaxSize()
                        .align(Alignment.CenterVertically), factory = { context ->
                        PlayerView(context).apply {
                            player = exPlayer
                        }
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoomableFullScreenImage(
    mediaAttachment: MediaAttachment,
    scaleTarget: Float,
    onBackPress: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    var scaffoldVisible by remember { mutableStateOf(false) }

    val scaleAnimation by animateFloatAsState(
        targetValue = scaleTarget, animationSpec = tween(
            durationMillis = 250, easing = FastOutSlowInEasing
        ), label = "imageScale"
    )
    val context = LocalContext.current

    MediaScaffold(title = mediaAttachment.name,
        visible = scaffoldVisible,
        onBackPress = onBackPress,
        openWith = {
            openWith(context, mediaAttachment.file)
        }) {
        Image(painter = rememberAsyncImagePainter(
            model = mediaAttachment.file, imageLoader = imageLoader!!
        ),
            contentDescription = "Image",
            modifier = Modifier
                .fillMaxSize()
                .padding(it.calculateBottomPadding())
                .onSizeChanged { s -> size = s }
                .combinedClickable(onClick = {
                    scaffoldVisible = !scaffoldVisible
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
                    scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY
                )
                .scale(scaleAnimation),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaScaffold(
    title: String,
    visible: Boolean,
    onBackPress: () -> Unit,
    openWith: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        containerColor = Color.Black,
        topBar = {
            AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
                TopAppBar(
                    title = { Text(text = title) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(
                            alpha = 0.5f
                        )
                    ),
                    navigationIcon = {
                        // Back button
                        IconButton(onClick = onBackPress) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack, contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        // Download button
                        Row {
                            IconButton(onClick = { }) {
                                Icon(
                                    painterResource(id = R.drawable.file_download_black_24dp),
                                    contentDescription = "Download"
                                )
                            }

                            // More button
                            IconButton(onClick = { openWith() }) {
                                Icon(
                                    Icons.Default.MoreVert, contentDescription = "More"
                                )
                            }
                        }
                    },

                    )
            }
        },
        content = content
    )
}