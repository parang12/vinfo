package com.example.vinfo.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Test

class DetailSampleInfoTextTest {

    @Test
    fun `buildSampleInfoText formats legacy json sample strings`() {
        val text = buildSampleInfoText(
            listOf(
                """{"artist":"Aretha Franklin","title":"Spirit in the Dark","sample_type":"contains sample","evidence_text":"Long evidence"}""",
                """{"artist":"Luther Vandross","title":"A House Is Not A Home","sample_type":"interpolation"}"""
            )
        )

        assertEquals(
            "Aretha Franklin - Spirit in the Dark (contains sample)\n" +
                "Luther Vandross - A House Is Not A Home (interpolation)",
            text
        )
    }
}
