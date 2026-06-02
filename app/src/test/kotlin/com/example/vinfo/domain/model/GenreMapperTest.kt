package com.example.vinfo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GenreMapperTest {

    @Test
    fun `fromRawGenre maps common album subgenres used by Gemini`() {
        assertEquals(GenreCategory.HIP_HOP, GenreMapper.fromRawGenre("Pop Rap"))
        assertEquals(GenreCategory.HIP_HOP, GenreMapper.fromRawGenre("Progressive Rap"))
        assertEquals(GenreCategory.RNB, GenreMapper.fromRawGenre("Soul"))
        assertEquals(GenreCategory.POP, GenreMapper.fromRawGenre("Art Pop"))
    }
}
