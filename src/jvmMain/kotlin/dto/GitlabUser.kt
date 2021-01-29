package com.mlreef.rest.external_api.gitlab.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.mlreef.rest.external_api.gitlab.GitlabAccessLevel
import com.mlreef.rest.external_api.gitlab.GitlabActivityState
import org.springframework.context.annotation.Scope
import java.io.Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
@Scope("session")
class GitlabUser(
    val id: Long,
    val username: String = "",
    val name: String = "",
    val email: String = "",
    val publicEmail: String = "",
    val state: String = "",
//    val organization: String = "",
    val avatarUrl: String = "",
    val weburlUrl: String = "",
    val isAdmin: Boolean = false,
    val canCreateGroup: Boolean = true,
    val canCreateProject: Boolean = true,
    val privateProfile: Boolean = true
) : Serializable

fun GitlabUserInProject.toGitlabUser(): GitlabUser =
    GitlabUser(
        id = this.id,
        username = this.username,
        name = this.name,
        state = this.state.name,
        avatarUrl = this.avatarUrl,
        weburlUrl = this.webUrl,
        isAdmin = false
    )

fun GitlabUser.toGitlabUserInProject(accessLevel: GitlabAccessLevel): GitlabUserInProject =
    GitlabUserInProject(
        id = this.id,
        webUrl = this.weburlUrl,
        name = this.name,
        username = this.username,
        state = GitlabActivityState.ACTIVE,
        avatarUrl = this.avatarUrl,
        accessLevel = accessLevel
    )
