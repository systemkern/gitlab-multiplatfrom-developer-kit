package com.systemkern.gitlabKDK

/**
 * Removes the protocol prefix from a given string.
 * So http://example.com/foo/bar -> example.com/foo/bar
 *
 * Default protocols to remove are "http://" and "https://"
 */
internal fun String.removeProtocol(protocols: List<String> = listOf("http://", "https://")): String =
    protocols.fold(initial = this) { it, prefix -> it.removePrefix(prefix) }

/**
 * @param rootUrl: the root url to your Gitlab instance including the protocol and
 * the optional relative root path
 * e.g. https://gitlab.example.com or http://gitlab.example.com/gitlab-relative-root/
 *
 */
const val oauthLoginPath = "oauth/token"

