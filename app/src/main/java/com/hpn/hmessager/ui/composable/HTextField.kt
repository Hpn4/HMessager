package com.hpn.hmessager.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hpn.hmessager.R


private fun strengthChecker(password: String): StrengthPasswordTypes =
    when {
        REGEX_STRONG_PASSWORD.toRegex().containsMatchIn(password) -> StrengthPasswordTypes.STRONG
        else -> StrengthPasswordTypes.WEAK
    }

enum class StrengthPasswordTypes {
    STRONG,
    WEAK
}

private const val REGEX_STRONG_PASSWORD =
    "(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9])(?=.{8,})"


@Composable
fun getTextFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimaryContainer,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedContainerColor = MaterialTheme.colorScheme.background,
        unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
        unfocusedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

@Composable
fun getTextFieldColorsGood(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimaryContainer,
        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
        cursorColor = MaterialTheme.colorScheme.tertiary,
        focusedContainerColor = MaterialTheme.colorScheme.background,
        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
        unfocusedTextColor = MaterialTheme.colorScheme.onSecondary,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
        focusedLabelColor = MaterialTheme.colorScheme.tertiary,
        unfocusedBorderColor = MaterialTheme.colorScheme.tertiary,
        unfocusedLabelColor = MaterialTheme.colorScheme.tertiary
    )
}

@Composable
fun MessageTextField(
    text: String,
    onTextChanged: (text: String) -> Unit
) {
    TextField(
        value = text,
        onValueChange = onTextChanged,
        maxLines = 6,
        trailingIcon = { Spacer(Modifier.padding(horizontal = 20.dp)) },
        modifier = Modifier
            .verticalScroll(rememberScrollState())
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
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
        ),
    )
}

@Composable
fun SimpleTextField(text: String,
                    modifier: Modifier = Modifier,
                    onTextChanged: (text: String) -> Unit,
                    label: String = "",
                    placeholder: String = "") {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChanged,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        colors = getTextFieldColors(),
        shape = RoundedCornerShape(20),
        modifier = modifier
    )
}

@Composable
fun PasswordTextField(
    text: String,
    modifier: Modifier = Modifier,
    semanticContentDescription: String = "",
    validateStrengthPassword: Boolean = false,
    isGood: Boolean = false,
    error: Boolean = false,
    onHasStrongPassword: (isStrong: Boolean) -> Unit = {},
    onTextChanged: (text: String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val showPassword = remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth().requiredHeight(90.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            modifier = Modifier
                .semantics { contentDescription = semanticContentDescription },
            shape = RoundedCornerShape(20),
            value = text,
            onValueChange = onTextChanged,
            label = { Text("Password", modifier = Modifier.background(Color.Transparent)) },
            placeholder = { Text("Enter your password") },
            keyboardOptions = KeyboardOptions.Default.copy(
                autoCorrect = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            ),
            singleLine = true,
            visualTransformation = if (showPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val (icon, iconColor) = if (showPassword.value) {
                    Pair(
                        R.drawable.baseline_visibility_24,
                        MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Pair(
                        R.drawable.baseline_visibility_off_24,
                        MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                IconButton(onClick = { showPassword.value = !showPassword.value }) {
                    Icon(
                        painterResource(id = icon),
                        contentDescription = "Visibility",
                        tint = iconColor
                    )
                }
            },
            colors = if (isGood) getTextFieldColorsGood() else getTextFieldColors(),
            isError = error
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (validateStrengthPassword && text.isNotEmpty()) {
            val strengthPasswordType = strengthChecker(text)
            if (strengthPasswordType == StrengthPasswordTypes.STRONG) {
                onHasStrongPassword(true)
            } else {
                onHasStrongPassword(false)
            }
            Text(
                modifier = Modifier.semantics { contentDescription = "StrengthPasswordMessage" },
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontSize = 10.sp
                        )
                    ) {
                        append(stringResource(id = R.string.warning_password_level))
                        when (strengthPasswordType) {
                            StrengthPasswordTypes.STRONG ->
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
                                    append(" ${stringResource(id = R.string.warning_password_level_strong)}")
                                }

                            StrengthPasswordTypes.WEAK ->
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.error)) {
                                    append(" ${stringResource(id = R.string.warning_password_level_weak)}")
                                }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ConfirmPasswordTextField(
    text: String,
    confirmText: String,
    modifier: Modifier = Modifier,
    semanticContentDescription: String = "",
    onPasswordMatch: (isMatching: Boolean) -> Unit = {},
    isGood: Boolean = false,
    onTextChanged: (text: String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val showPassword = remember { mutableStateOf(false) }
    val matchError = remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            modifier = Modifier
                .semantics { contentDescription = semanticContentDescription },
            value = text,
            shape = RoundedCornerShape(20),
            onValueChange = onTextChanged,
            label = { Text("Confirm password", modifier = Modifier.background(Color.Transparent)) },
            placeholder = { Text("Enter your password") },
            keyboardOptions = KeyboardOptions.Default.copy(
                autoCorrect = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            ),
            singleLine = true,
            isError = matchError.value,
            visualTransformation =
            if (showPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val (icon, iconColor) = if (showPassword.value) {
                    Pair(
                        R.drawable.baseline_visibility_24,
                        MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Pair(
                        R.drawable.baseline_visibility_off_24,
                        MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                IconButton(onClick = { showPassword.value = !showPassword.value }) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = "Visibility",
                        tint = iconColor
                    )
                }
            },
            colors = if (isGood) getTextFieldColorsGood() else getTextFieldColors()
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (confirmText != text && text.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.error_password_no_match),
                color = MaterialTheme.colorScheme.error,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { contentDescription = "ConfirmPasswordMessage" },
            )
            matchError.value = true
            onPasswordMatch(false)
        } else {
            matchError.value = false
            onPasswordMatch(text.isNotEmpty())
        }
    }
}