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
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType
import org.slf4j.LoggerFactory

/**
 * Creates the servlet which renders the soy template containing the frontend SPA bundle.
 */
class TemplateServlet(
    private val templateRenderer: SoyTemplateRenderer,
    private val permissionEnforcer: PermissionEnforcer,
    private val loginUriProvider: LoginUriProvider
) : HttpServlet() {
    @Throws(IOException::class)
    public override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
        try {
            when {
                permissionEnforcer.isSystemAdmin -> {
                    render(response)
                }
                permissionEnforcer.isAuthenticated -> {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN)
                }
                else -> {
                    response.sendRedirect(
                        loginUriProvider.getLoginUri(URI.create(request.requestURL.toString())).toASCIIString()
                    )
                }
            }
        } catch (exception: IOException) {
            logger.error("Unable to render template", exception)
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
        }
    }

    @Throws(IOException::class)
    private fun render(response: HttpServletResponse) {
        response.contentType = MediaType.TEXT_HTML
        templateRenderer.render(
            response.writer,
            "com.atlassian.migration.datacenter.dc-migration-assistant:dc-migration-assistant-templates",
            "dcmigration.init", emptyMap()
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateServlet::class.java)
    }
}