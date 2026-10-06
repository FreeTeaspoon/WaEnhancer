/* Dialog pattern adapted from HyperLPA e613bb84, GPL-3.0. */
package com.freeteaspoon.wppenhacer.ui.miuix

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.freeteaspoon.wppenhacer.R
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun ManagerDialogActions(
    onCancel: () -> Unit,
    confirmText: String = stringResource(R.string.manager_save),
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(stringResource(android.R.string.cancel), onCancel, Modifier.weight(1f))
        TextButton(confirmText, onConfirm, Modifier.weight(1f), enabled = confirmEnabled,
            colors = ButtonDefaults.textButtonColorsPrimary())
    }
}

@Composable
internal fun ManagerTextInputDialog(
    show: Boolean,
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    summary: String? = null,
    placeholder: String = "",
    maxLength: Int = 256,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    inputFilter: (String) -> String = { it },
    allowBlank: Boolean = false,
    validate: (String) -> String? = { null },
    onDismissFinished: (() -> Unit)? = null,
    suffix: String? = null,
    confirmText: String = stringResource(R.string.manager_save),
    singleLine: Boolean = true,
) {
    WindowDialog(show = show, title = title, summary = summary,
        onDismissRequest = onDismiss, onDismissFinished = onDismissFinished) {
        var value by remember { mutableStateOf(TextFieldValue(initialValue, TextRange(initialValue.length))) }
        val focus = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) { focus.requestFocus(); keyboard?.show() }
        val error = validate(value.text)
        val valid = error == null && (allowBlank || value.text.isNotBlank())
        TextField(
            value = value,
            onValueChange = { input ->
                val filtered = inputFilter(input.text)
                if (filtered.length <= maxLength) value = if (filtered == input.text) input
                    else TextFieldValue(filtered, TextRange(filtered.length))
            },
            label = placeholder,
            useLabelAsPlaceholder = placeholder.isNotEmpty(),
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (valid) onConfirm(value.text) }),
            visualTransformation = visualTransformation,
            trailingIcon = suffix?.let { text -> { Text(text, Modifier.padding(horizontal = 16.dp)) } },
        )
        if (error != null) Text(error, color = MiuixTheme.colorScheme.error,
            style = MiuixTheme.textStyles.footnote1, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))
        ManagerDialogActions(onDismiss, confirmText, valid) { onConfirm(value.text) }
    }
}
