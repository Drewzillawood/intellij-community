// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.gitlab.authentication.ui

import com.intellij.collaboration.auth.ui.LazyLoadingAccountsDetailsProvider
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.openapi.components.service
import icons.CollaborationToolsIcons
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.plugins.gitlab.GitLabServersManager
import org.jetbrains.plugins.gitlab.api.GitLabApi
import org.jetbrains.plugins.gitlab.api.dto.GitLabUserDTO
import org.jetbrains.plugins.gitlab.api.request.getCurrentUser
import org.jetbrains.plugins.gitlab.api.request.loadImage
import org.jetbrains.plugins.gitlab.authentication.accounts.GitLabAccount
import org.jetbrains.plugins.gitlab.isServerVersionSupported
import org.jetbrains.plugins.gitlab.util.GitLabBundle
import java.awt.Image

internal class GitLabAccountsDetailsProvider(scope: CoroutineScope,
                                             private val apiClientSupplier: suspend (GitLabAccount) -> GitLabApi?)
  : LazyLoadingAccountsDetailsProvider<GitLabAccount, GitLabUserDTO>(scope, CollaborationToolsIcons.Review.DefaultAvatar) {

  override suspend fun loadDetails(account: GitLabAccount): Result<GitLabUserDTO> {
    val api = apiClientSupplier(account) ?: return Result.Error(CollaborationToolsBundle.message("account.token.missing"), true)
    val details = api.graphQL.getCurrentUser(account.server) ?: return Result.Error(CollaborationToolsBundle.message("account.token.invalid"), true)
    try {
      val supported = service<GitLabServersManager>().isServerVersionSupported(account.server, api)
      if (!supported) return Result.Error(GitLabBundle.message("server.version.unsupported.short"), false)
    }
    catch (e: Exception) {
      return Result.Error(e.message, false)
    }
    return Result.Success(details)
  }

  override suspend fun loadAvatar(account: GitLabAccount, url: String): Image? {
    val api = apiClientSupplier(account) ?: return null
    val actualUrl = if (url.startsWith("http")) url else account.server.uri + url
    return api.loadImage(actualUrl)
  }
}