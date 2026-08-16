package com.wmods.wppenhacer.ui.miuix

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ManagerSnackbarEvents {
    private val _events = MutableSharedFlow<String>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events = _events.asSharedFlow()

    @Volatile
    private var active = false

    fun register() {
        active = true
    }

    fun unregister() {
        active = false
    }

    fun tryShow(message: String): Boolean = active && _events.tryEmit(message)
}
