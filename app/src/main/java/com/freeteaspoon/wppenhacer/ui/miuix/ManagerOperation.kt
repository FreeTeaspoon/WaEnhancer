package com.freeteaspoon.wppenhacer.ui.miuix

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Keeps long-operation ownership through authoritative completion and popup exit. */
internal class ManagerOperation {
    private val lock = Mutex()
    private val _label = MutableStateFlow<String?>(null)
    val label = _label.asStateFlow()
    private val _visible = MutableStateFlow(false)
    val visible = _visible.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _epoch = MutableStateFlow(0L)
    val epoch = _epoch.asStateFlow()
    private var exit: CompletableDeferred<Unit>? = null
    private var hosts = 0

    suspend fun <T> run(label: String, block: suspend () -> T): T {
        lock.lock()
        _epoch.value++
        _label.value = label
        _busy.value = true
        exit = CompletableDeferred()
        _visible.value = true
        try { return block() }
        finally {
            _visible.value = false
            // A detached route cannot animate. Never leave a model locked after recreation.
            try {
                if (hosts > 0) exit?.await()
            } finally {
                _busy.value = false
                _label.value = null
                exit = null
                lock.unlock()
            }
        }
    }
    fun attach() { hosts++ }
    fun detach() { hosts--; if (hosts == 0 && !_visible.value) dismissed() }
    fun dismissed() { if (!_visible.value) exit?.complete(Unit) }
}

@Composable
internal fun ManagerOperationHost(operation: ManagerOperation) {
    val visible by operation.visible.collectAsStateWithLifecycle()
    val label by operation.label.collectAsStateWithLifecycle()
    val epoch by operation.epoch.collectAsStateWithLifecycle()
    var wasVisible by remember { mutableStateOf(false) }
    DisposableEffect(operation) { operation.attach(); onDispose { operation.detach() } }
    // A completed operation restored into a new composition has no exit to animate.
    LaunchedEffect(visible, label, epoch) {
        if (visible) wasVisible = true else if (!wasVisible) operation.dismissed()
    }
    OverlayDialog(show = visible, onDismissRequest = null,
        onDismissFinished = { wasVisible = false; operation.dismissed() }, cornerRadius = 36.dp) {
        Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            InfiniteProgressIndicator(size = 24.dp, color = if (LocalManagerDarkMode.current) MiuixTheme.colorScheme.onSurface else Color.Black)
            Text(label.orEmpty(), style = MiuixTheme.textStyles.body1)
        }
    }
}
