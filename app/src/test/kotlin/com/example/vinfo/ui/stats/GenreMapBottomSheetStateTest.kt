package com.example.vinfo.ui.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class GenreMapBottomSheetStateTest {

    @Test
    fun `dragging down collapses the insight sheet one step`() {
        assertEquals(
            TasteFlowSheetState.Peek,
            nextTasteFlowSheetStateAfterDrag(
                current = TasteFlowSheetState.Expanded,
                dragDeltaPx = 80f,
                thresholdPx = 64f
            )
        )
        assertEquals(
            TasteFlowSheetState.Hidden,
            nextTasteFlowSheetStateAfterDrag(
                current = TasteFlowSheetState.Peek,
                dragDeltaPx = 80f,
                thresholdPx = 64f
            )
        )
    }

    @Test
    fun `dragging up expands the insight sheet one step`() {
        assertEquals(
            TasteFlowSheetState.Peek,
            nextTasteFlowSheetStateAfterDrag(
                current = TasteFlowSheetState.Hidden,
                dragDeltaPx = -80f,
                thresholdPx = 64f
            )
        )
        assertEquals(
            TasteFlowSheetState.Expanded,
            nextTasteFlowSheetStateAfterDrag(
                current = TasteFlowSheetState.Peek,
                dragDeltaPx = -80f,
                thresholdPx = 64f
            )
        )
    }

    @Test
    fun `small drags keep the insight sheet in the same state`() {
        assertEquals(
            TasteFlowSheetState.Peek,
            nextTasteFlowSheetStateAfterDrag(
                current = TasteFlowSheetState.Peek,
                dragDeltaPx = 20f,
                thresholdPx = 64f
            )
        )
    }

    @Test
    fun `handle tap toggles between expanded and hidden`() {
        assertEquals(TasteFlowSheetState.Hidden, nextTasteFlowSheetStateOnHandleTap(TasteFlowSheetState.Expanded))
        assertEquals(TasteFlowSheetState.Expanded, nextTasteFlowSheetStateOnHandleTap(TasteFlowSheetState.Peek))
        assertEquals(TasteFlowSheetState.Expanded, nextTasteFlowSheetStateOnHandleTap(TasteFlowSheetState.Hidden))
    }
}
