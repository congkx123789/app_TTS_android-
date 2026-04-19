package com.example.texttosound.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.texttosound.viewmodel.BookViewModel

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun FloatingMiniPlayer(
    viewModel: BookViewModel,
    bookCover: ByteArray?,
    title: String,
    isSynthesizing: Boolean,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onCloseClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 16.dp)
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offset += dragAmount
                }
            },
        contentAlignment = Alignment.BottomEnd
    ) {
        // Use a wrapping Box to contain the overlapping cover and pill
        Box(
            modifier = Modifier.width(130.dp).height(44.dp)
        ) {
            // Pill background (aligned to the right, slightly thinner than the container to allow cover to overhang)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(110.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF333333).copy(alpha = 0.95f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 28.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Play/Pause
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { onPlayPauseClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSynthesizing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (isPlaying) {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Box(modifier = Modifier.width(3.dp).height(10.dp).background(Color.White, RoundedCornerShape(1.dp)))
                                Box(modifier = Modifier.width(3.dp).height(10.dp).background(Color.White, RoundedCornerShape(1.dp)))
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Close (X)
                    IconButton(onClick = onCloseClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Stop",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Cover Image (Circular, on the left)
            val bitmap = bookCover?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
            val coverModifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(2.dp)
                .clip(CircleShape)
                .clickable { onClick() }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Cover",
                    modifier = coverModifier,
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = coverModifier.background(Color.DarkGray))
            }
        }
    }
}
