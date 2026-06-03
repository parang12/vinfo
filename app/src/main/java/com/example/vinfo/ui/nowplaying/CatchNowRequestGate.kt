package com.example.vinfo.ui.nowplaying

import java.util.concurrent.atomic.AtomicBoolean

class CatchNowRequestGate {
    private val running = AtomicBoolean(false)

    fun tryStart(): Boolean {
        return running.compareAndSet(false, true)
    }

    fun finish() {
        running.set(false)
    }
}
