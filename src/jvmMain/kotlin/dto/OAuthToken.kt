package com.mlreef.rest.external_api.gitlab.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class OAuthToken(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("refresh_token")
    val refreshToken: String?,
    @JsonProperty("token_type")
    val tokenType: String,
    val scope: String,
    @JsonProperty("created_at")
    val createdAt: Long
)
