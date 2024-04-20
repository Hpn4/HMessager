package com.hpn.hmessager.presentation.composable

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun HButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor : Color = MaterialTheme.colorScheme.primary,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit = 16.sp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.secondary,
            disabledContentColor = MaterialTheme.colorScheme.onSecondary
        )
    ) {
        Text(text = text,
            modifier = Modifier.padding(horizontal = 15.dp),
            fontWeight = fontWeight,
            fontSize = fontSize)
    }
}

@Composable
fun DialogScreen(
    spacedBy: Int = 30,
    title: String,
    buttonOnClick: () -> Unit = {},
    buttonEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val activity = (LocalContext.current as? Activity)

    Dialog(onDismissRequest = { activity?.finish() }) {
        Surface(
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(spacedBy.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                content()

                HButton(
                    text = "Continue",
                    enabled = buttonEnabled,
                    onClick = buttonOnClick,
                    modifier = Modifier
                        .padding(16.dp)
                )
            }
        }
    }
}