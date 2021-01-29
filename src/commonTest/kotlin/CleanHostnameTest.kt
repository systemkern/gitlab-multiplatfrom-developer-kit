package com.systemkern.gitlabKDK

import kotlin.test.Test
import kotlin.test.assertEquals

internal class CleanHostnameTest {

    @Test
    fun `Can remove protocol from URL`() {
        assertEquals("www.example.com","http://www.example.com".removeProtocol())
        assertEquals("www.example.com","https://www.example.com".removeProtocol())
        assertEquals("www.example.com/","http://www.example.com/".removeProtocol())
        assertEquals("www.example.com/","https://www.example.com/".removeProtocol())
        assertEquals("www.example.com//","http://www.example.com//".removeProtocol())
        assertEquals("www.example.com//","https://www.example.com//".removeProtocol())
        assertEquals("www.example.com/http://","www.example.com/http://".removeProtocol())
        assertEquals("www.example.com/https://","www.example.com/https://".removeProtocol())
        assertEquals("www.example.http:/","www.example.http:/".removeProtocol())
        assertEquals("www.example.https:/","www.example.https:/".removeProtocol())
        assertEquals("www.example.http://","www.example.http://".removeProtocol())
        assertEquals("www.example.https://","www.example.https://".removeProtocol())
    }
}
