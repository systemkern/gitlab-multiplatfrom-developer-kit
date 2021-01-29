package com.mlreef.rest.external_api.gitlab

import com.mlreef.rest.external_api.gitlab.dto.GitlabProject
import com.mlreef.rest.external_api.gitlab.dto.GitlabUser
import com.mlreef.rest.utils.RandomUtils
import com.mlreef.rest.utils.Slugs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test


class GitlabRestClientTest {
    private val gitlabRestClient = GitlabRestClient(
        rootUrl = "http://localhost:80",
        adminUsername = "root",         // default gitlab root user is "root"
        adminPassword = "password",     // default root password is "password"
        adminUserToken = "",
        builder = RestTemplateBuilder()
    )

    private lateinit var token: String

    fun createRealUser(userName: String? = null, password: String? = null): Pair<GitlabUser, String> {
        val username = userName ?: RandomUtils.generateRandomUserName(20)
        val email = "$username@example.com"
        val plainPassword = password ?: RandomUtils.generateRandomPassword(30, true)
        val userInGitlab = gitlabRestClient.adminCreateUser(email, username, "Existing $username", plainPassword)
        val loggedClient = gitlabRestClient.userLoginOAuthToGitlab(username, plainPassword)
        return userInGitlab to loggedClient.accessToken
    }


    @BeforeTest
    fun fillRepo() {
        token = gitlabRestClient.adminCreateUserToken(gitlabUserId = 1, tokenName = "unitTest")
            .token!!
        val (_, thatToken) = createRealUser()
        token = thatToken
    }


    @Test
    fun `Can login root user`() {
        gitlabRestClient.userLoginOAuthToGitlab(
            userName = gitlabRestClient.adminUsername,
            password = gitlabRestClient.adminPassword,
        )
    }

    @Test
    fun `createProject shall return created project`() {
        val (_, token) = createRealUser()
        val createProject = gitlabRestClient.createProject(
            token = token,
            slug = "unique",
            name = "name",
            description = "description",
            defaultBranch = "master",
            visibility = "public",
        )
        assertThat(createProject).isNotNull
    }

    @Test
    fun `createProject must not accept empty token`() {
        assertThrows<GitlabCommonException> {
            gitlabRestClient.createProject(
                token = "",
                slug = "another",
                name = "another",
                description = "description",
                defaultBranch = "master",
                visibility = "public",
            )
        }
    }

    @Test
    fun `Can fork project into same namespace`() {
        val (_, token) = createRealUser()
        val original = gitlabRestClient.createProject(
            token = token,
            slug = "original-name",
            name = "Original Name",
            description = "description",
            defaultBranch = "master",
            visibility = "public",
        )
        val targetName = "New Name"
        val fork = gitlabRestClient.forkProject(
            token = token,
            sourceId = original.id,
            targetName = targetName,
        )

        with(fork) {
            assertThat(id).isNotEqualTo(original.id)
            assertThat(name).isEqualTo(targetName)
            assertThat(path).contains("new-name")
            assertThat(namespace!!.path).contains(original.namespace!!.path)
            assertThat(createdAt).isNotEqualTo(original.createdAt)
        }
    }

    @Test
    fun `Can fork project from foreign namespace into personal namespace`() {
        val (_, token) = createRealUser()
        val original = gitlabRestClient.createProject(
            token = this.token,
            slug = "original-name",
            name = "Original Name",
            description = "description",
            defaultBranch = "master",
            visibility = "public",
        )
        val fork = gitlabRestClient.forkProject(
            token = token,
            sourceId = original.id,
            targetName = original.name, // possible because we are forking from a foreign namespace to our private namespace
        )

        with(fork) {
            assertThat(id).isNotEqualTo(original.id)
            assertThat(name).isEqualTo(original.name)
            assertThat(path).isEqualTo(original.path)
            assertThat(namespace!!.path).doesNotContain(original.namespace!!.path)
            assertThat(createdAt).isNotEqualTo(original.createdAt)
        }
    }

    @Test
    fun `createProject must not accept duplicate slug`() {
        val createProject = createProject(token)
        assertThat(createProject).isNotNull
        assertThrows<GitlabCommonException> {
            createProject("")
        }
    }

    @Test
    fun `userUpdateProject must not accept empty name`() {
        val createProject = createProject(token)
        assertThat(createProject).isNotNull
        assertThrows<BadRequestException> {
            gitlabRestClient.userUpdateProject(
                id = createProject.id,
                token = token,
                name = "",
                visibility = "public",
            )
        }
    }

    @Test
    fun `userUpdateProject must not change visibility`() {
        val createProject = createProject(token)
        assertThat(createProject).isNotNull
        assertThrows<BadRequestException> {
            gitlabRestClient.userUpdateProject(
                id = createProject.id,
                token = token,
                visibility = "",
            )
        }
    }

    @Test
    fun `userUpdateProject should accept to change visibility  often`() {
        val createProject = createProject(token)
        assertThat(createProject).isNotNull

        gitlabRestClient.userUpdateProject(
            id = createProject.id,
            token = token,
            visibility = "public"
        )
        gitlabRestClient.userUpdateProject(
            id = createProject.id,
            token = token,
            visibility = "private"
        )
    }

    @Test
    fun `Can create Git branch`() {
        val createProject = createProject(token)
        assertThat(createProject).isNotNull
        val createBranch = gitlabRestClient.createBranch(
            token = token,
            projectId = createProject.id,
            targetBranch = "second",
            sourceBranch = "master",
        )
        assertThat(createBranch).isNotNull
    }

    @Test
    fun `Cannot create branch without valid source branch`() {
        val createProject = createProject(token)
        assertThat(createProject).isNotNull

        assertThrows<BadRequestException> {
            gitlabRestClient.createBranch(
                token = token,
                projectId = createProject.id,
                targetBranch = "second",
                sourceBranch = "no",
            )
        }
    }

    @Test
    fun `Cannot delete master branch`() {
        val createProject = createProject(token)
        assertThat(createProject).isNotNull
        assertThrows<BadRequestException> {
            gitlabRestClient.deleteBranch(
                token = token,
                projectId = createProject.id,
                targetBranch = "master",
            )
        }
    }

    @Test
    fun `Can delete branch`() {
        val createProject = createProject(token)
        assertThat(createProject).isNotNull
        val createBranch = gitlabRestClient.createBranch(
            token = token,
            projectId = createProject.id,
            targetBranch = "second",
            sourceBranch = "master"
        )
        assertThat(createBranch).isNotNull

        gitlabRestClient.deleteBranch(
            token = token,
            projectId = createProject.id,
            targetBranch = "second"
        )
    }

    @Test
    fun `deleteBranch must not delete branch twice`() {
        val createProject = createProject(token)
        assertThat(createProject).isNotNull
        val createBranch = gitlabRestClient.createBranch(
            token = token,
            projectId = createProject.id,
            targetBranch = "second",
            sourceBranch = "master",
        )
        assertThat(createBranch).isNotNull

        gitlabRestClient.deleteBranch(
            token = token,
            projectId = createProject.id,
            targetBranch = "second",
        )

        assertThrows<NotFoundException> {
            gitlabRestClient.deleteBranch(
                token = token,
                projectId = createProject.id,
                targetBranch = "second",
            )
        }
    }

    private fun createProject(
        token: String,
        name: String = "Name " + RandomUtils.generateRandomUserName(20),
        slug: String = Slugs.toSlug("slug-" + name),
        initializeWithReadme: Boolean = true,
        defaultBranch: String = "master",
    ): GitlabProject = gitlabRestClient.createProject(
        token = token,
        slug = slug,
        name = name,
        description = "description",
        defaultBranch = defaultBranch,
        visibility = "public",
        initializeWithReadme = initializeWithReadme,
    )

    @Test
    fun `adminGetProjects shall deliver all projects`() {
        val createProject1 = createProject(token)
        val createProject2 = createProject(token)

        val projects = gitlabRestClient.adminGetProjects()
        assertThat(projects).isNotNull
        assertThat(projects.map { it.id }).contains(createProject1.id)
        assertThat(projects.map { it.id }).contains(createProject2.id)
    }

    @Test
    fun `adminGetProject shall deliver certain projects`() {
        val createProject1 = createProject(token)

        val project = gitlabRestClient.adminGetProject(createProject1.id)
        assertThat(project).isNotNull
        assertThat(project).isEqualToIgnoringGivenFields(
            createProject1,
            "owner",
            "namespace",
            "buildCoverageRegex",
            "readmeUrl",
            "avatarUrl"
        )
    }

    @Test
    fun `adminGetProject must not deliver not-existing projects`() {
        assertThrows<NotFoundException> {
            gitlabRestClient.adminGetProject(Long.MAX_VALUE)
        }
    }

    @Test
    fun `adminGetProjectMembers must not deliver for not-existing projects`() {
        assertThrows<NotFoundException> {
            gitlabRestClient.adminGetProjectMembers(Long.MAX_VALUE)
        }
    }

    @Test
    fun `adminGetProjectMembers shall deliver creating user`() {
        val (user1, token1) = createRealUser()
        createRealUser()
        val project1 = createProject(token1)
        val members = gitlabRestClient.adminGetProjectMembers(project1.id)
        assertThat(members).isNotNull
        assertThat(members).hasSize(1)
        assertThat(members.map { it.id }).contains(user1.id)
    }

    @Test
    fun `adminGetProjectMember shall deliver owner`() {
        val (user1, token1) = createRealUser()
        val project1 = createProject(token1)
        val member = gitlabRestClient.adminGetProjectMember(project1.id, user1.id)
        assertThat(member).isNotNull
        assertThat(member.id).isEqualTo(user1.id)
    }

    @Test
    fun `adminGetProjectMember must not deliver unconnected user`() {
        val (_, token1) = createRealUser()
        val (user2, _) = createRealUser()
        val project1 = createProject(token1)
        assertThrows<NotFoundException> {
            gitlabRestClient.adminGetProjectMember(project1.id, user2.id)
        }
    }

    @Test
    fun `adminAddUserToProject must not accept not-existing user`() {
        val (_, token1) = createRealUser()
        val project1 = createProject(token1)
        assertThrows<NotFoundException> {
            gitlabRestClient.adminAddUserToProject(project1.id, Long.MAX_VALUE)
        }
    }

    @Test
    fun `adminAddUserToProject must not accept already included user`() {
        val (user1, token1) = createRealUser()
        val project1 = createProject(token1)
        assertThrows<ConflictException> {
            gitlabRestClient.adminAddUserToProject(project1.id, user1.id)
        }
    }

    @Test
    fun `adminAddUserToProject shall accept existing user`() {
        val (_, token1) = createRealUser()
        val (user2, _) = createRealUser()
        val project1 = createProject(token1)
        val member = gitlabRestClient.adminAddUserToProject(project1.id, user2.id)
        assertThat(member).isNotNull
        assertThat(member.id).isEqualTo(user2.id)
    }

    @Test
    fun `adminEditUserInProject must not accept not-existing user`() {
        val (_, token1) = createRealUser()
        val project1 = createProject(token1)
        assertThrows<NotFoundException> {
            gitlabRestClient.adminEditUserInProject(project1.id, Long.MAX_VALUE)
        }
    }

    @Test
    fun `adminEditUserInProject must not accept already included user`() {
        val (user1, token1) = createRealUser()
        val project1 = createProject(token1)
        assertThrows<GitlabAuthenticationFailedException> {
            gitlabRestClient.adminEditUserInProject(project1.id, user1.id)
        }
    }

    @Test
    fun `adminEditUserInProject shall accept existing user`() {
        val (_, token1) = createRealUser()
        val (user2, _) = createRealUser()
        val project1 = createProject(token1)
        gitlabRestClient.adminAddUserToProject(project1.id, user2.id)

        val member = gitlabRestClient.adminEditUserInProject(project1.id, user2.id)
        assertThat(member.id).isEqualTo(user2.id)
    }

    @Test
    fun `adminEditUserInProject must not transform owner to guest`() {
        val (user1, token1) = createRealUser()
        val project1 = createProject(token1)
        assertThrows<GitlabAuthenticationFailedException> {
            gitlabRestClient.adminEditUserInProject(project1.id, user1.id, accessLevel = GitlabAccessLevel.GUEST)
        }
    }

    @Test
    fun `adminEditUserInProject shall accept multiple conversions`() {
        val (_, token1) = createRealUser()
        val (user2, _) = createRealUser()
        val project1 = createProject(token1)
        val member1 = gitlabRestClient.adminAddUserToProject(project1.id, user2.id, GitlabAccessLevel.GUEST)
        val member2 = gitlabRestClient.adminEditUserInProject(project1.id, user2.id, GitlabAccessLevel.DEVELOPER)
        val member3 = gitlabRestClient.adminEditUserInProject(project1.id, user2.id, GitlabAccessLevel.GUEST)
        assertThat(member1.id).isEqualTo(user2.id)
        assertThat(member2.id).isEqualTo(user2.id)
        assertThat(member3.id).isEqualTo(user2.id)
    }

    @Test
    fun `userEditUserInProject must not accept already included user`() {
        val (user1, token1) = createRealUser()
        val project1 = createProject(token1)
        assertThrows<GitlabAuthenticationFailedException> {
            gitlabRestClient.userEditUserInProject(token1, project1.id, user1.id)
        }
    }

    @Test
    fun `userEditUserInProject must not accept token of not owner`() {
        val (user1, token1) = createRealUser()
        val (_, token2) = createRealUser()

        val project1 = createProject(token1)
        assertThrows<GitlabAuthenticationFailedException> {
            gitlabRestClient.userEditUserInProject(token2, project1.id, user1.id)
        }
    }

    @Test
    fun `userEditUserInProject shall accept existing user`() {
        val (_, token1) = createRealUser()
        val (user2, _) = createRealUser()
        val project1 = createProject(token1)
        gitlabRestClient.adminAddUserToProject(project1.id, user2.id)
        val member2 = gitlabRestClient.userEditUserInProject(token1, project1.id, user2.id)
        assertThat(member2.id).isEqualTo(user2.id)
    }

    @Test
    fun `Can give user GUEST access to project as maintainer`() {
        val (_, ownerToken) = createRealUser()
        val (maintainer, maintainerToken) = createRealUser()
        val (user, _) = createRealUser()
        val project = createProject(ownerToken)
        gitlabRestClient.adminAddUserToProject(
            projectId = project.id,
            userId = maintainer.id,
            accessLevel = GitlabAccessLevel.MAINTAINER,
        )
        val member = gitlabRestClient.userEditUserInProject(
            token = maintainerToken,
            projectId = project.id,
            userId = user.id,
            accessLevel = GitlabAccessLevel.GUEST,
        )
        assertThat(member.id).isEqualTo(user.id)
    }

    @Test
    fun `userEditUserInProject must not accept change by guest`() {
        val (_, token1) = createRealUser()
        val (user2, token2) = createRealUser()
        val (user3, _) = createRealUser()
        val project1 = createProject(token1)
        gitlabRestClient.adminAddUserToProject(
            projectId = project1.id,
            userId = user2.id,
            accessLevel = GitlabAccessLevel.GUEST
        )
        assertThrows<GitlabAuthenticationFailedException> {
            gitlabRestClient.userEditUserInProject(token2, project1.id, user3.id, GitlabAccessLevel.GUEST)
        }
    }

    @Test
    fun `userEditUserInProject must not transform owner to guest`() {
        val (user1, token1) = createRealUser()
        val project1 = createProject(token1)
        assertThrows<GitlabAuthenticationFailedException> {
            gitlabRestClient.userEditUserInProject(token1, project1.id, user1.id, accessLevel = GitlabAccessLevel.GUEST)
        }
    }

    @Test
    fun `userEditUserInProject shall accept multiple conversions`() {
        val (_, token1) = createRealUser()
        val (user2, _) = createRealUser()
        val project1 = createProject(token1)
        val member1 = gitlabRestClient.adminAddUserToProject(project1.id, user2.id, GitlabAccessLevel.GUEST)
        val member2 = gitlabRestClient.userEditUserInProject(
            token = token1,
            projectId = project1.id,
            userId = user2.id,
            accessLevel = GitlabAccessLevel.DEVELOPER
        )
        val member3 = gitlabRestClient.userEditUserInProject(
            token = token1,
            projectId = project1.id,
            userId = user2.id,
            accessLevel = GitlabAccessLevel.GUEST
        )
        assertThat(member1.id).isEqualTo(user2.id)
        assertThat(member2.id).isEqualTo(user2.id)
        assertThat(member3.id).isEqualTo(user2.id)
    }
}
