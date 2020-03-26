/*
 * Copyright 2020 Atlassian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atlassian.migration.datacenter

import com.atlassian.sal.api.auth.LoginUriProvider
import com.atlassian.sal.api.permission.PermissionEnforcer
import com.atlassian.soy.renderer.SoyTemplateRenderer
import java.io.IOException
import java.net.URI
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class TemplateServletTest {
    private var sut: TemplateServlet? = null

    @Mock
    private val mockRenderer: SoyTemplateRenderer? = null

    @Mock
    private val mockPermissionEnforcer: PermissionEnforcer? = null

    @Mock
    private val mockLoginUriProvider: LoginUriProvider? = null

    @Spy
    private val mockResp: HttpServletResponse? = null

    @Mock
    private val mockReq: HttpServletRequest? = null

    @BeforeEach
    fun init() {
        sut = TemplateServlet(mockRenderer!!, mockPermissionEnforcer!!, mockLoginUriProvider!!)
    }

    @Test
    fun itShouldReturnHtmlPageWhenSuccessful() {
        Mockito.`when`(mockPermissionEnforcer!!.isSystemAdmin).thenReturn(true)
        try {
            sut!!.doGet(mockReq!!, mockResp!!)
            Mockito.verify(mockResp).contentType = MediaType.TEXT_HTML
        } catch (ioe: IOException) {
            Assertions.fail<Any>()
        }
    }

    @Test
    fun itShouldReturnForbiddenWhenNotSysAdmin() {
        Mockito.`when`(mockPermissionEnforcer!!.isSystemAdmin).thenReturn(false)
        Mockito.`when`(mockPermissionEnforcer.isAuthenticated).thenReturn(true)
        try {
            sut!!.doGet(mockReq!!, mockResp!!)
            Mockito.verify(mockResp).sendError(HttpServletResponse.SC_FORBIDDEN)
        } catch (ioe: IOException) {
            Assertions.fail<Any>()
        }
    }

    @Test
    fun itShouldRedirectWhenUnauthenticated() {
        Mockito.`when`(mockPermissionEnforcer!!.isSystemAdmin).thenReturn(false)
        Mockito.`when`(mockPermissionEnforcer.isAuthenticated).thenReturn(false)
        Mockito.`when`(mockLoginUriProvider!!.getLoginUri(DUMMY_LOGIN_URI)).thenReturn(DUMMY_LOGIN_URI)
        Mockito.`when`(mockReq!!.requestURL).thenReturn(StringBuffer(DUMMY_LOGIN_URL_STRING))
        try {
            sut!!.doGet(mockReq, mockResp!!)
            Mockito.verify(mockResp).sendRedirect(DUMMY_LOGIN_URL_STRING)
        } catch (ioe: IOException) {
            Assertions.fail<Any>()
        }
    }

    companion object {
        private const val DUMMY_LOGIN_URL_STRING = "dummy-app.com/login"
        private val DUMMY_LOGIN_URI = URI.create(DUMMY_LOGIN_URL_STRING)
    }
}