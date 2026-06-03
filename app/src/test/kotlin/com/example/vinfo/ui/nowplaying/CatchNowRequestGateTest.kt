package com.example.vinfo.ui.nowplaying

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatchNowRequestGateTest {
    @Test
    fun `tryStart rejects duplicate requests until finished`() {
        val gate = CatchNowRequestGate()

        assertTrue(gate.tryStart())
        assertFalse(gate.tryStart())

        gate.finish()

        assertTrue(gate.tryStart())
    }
}
