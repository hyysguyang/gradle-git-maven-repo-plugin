package com.lifecosys.gradle

import com.jcraft.jsch.Session
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.publish.PublishingExtension
import java.io.File

class GitMavenRepoPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            logger.info("Apply GiMavenRepo Plugin....")
            extensions.create("gitMavenRepo", GitMavenRepoConfig::class.java)
            afterEvaluate {
                val gitMavenRepoConfig = extensions.getByName("gitMavenRepo") as GitMavenRepoConfig
                logger.info("gitMavenRepo: $gitMavenRepoConfig")
                val repo = GitMavenRepository(gitMavenRepoConfig)
                repo.logger = logger
                repositories.maven { it.setUrl(gitMavenRepoConfig.repoDir) }
                addPublishConfiguration(repo)
            }
        }
    }

    private fun Project.addPublishConfiguration(repo: GitMavenRepository) {
        val publishingExtension = extensions.findByType(PublishingExtension::class.java)

        publishingExtension?.apply {
            repositories.maven { it.setUrl(repo.config.repoDir) }

            if (repo.config.release) {
                tasks.getByName("publish").doLast {
                    val message = "Release ${group}:${name}:${version} "
                    repo.addAndPush(message)
                }
            }
        }
    }
}

open class GitMavenRepoConfig {
    var url: String = ""
    var gitUsername: String = ""
    var gitPassword: String = ""
    var repoDir: String = "${System.getProperty("user.home")}/.gitMavenRepo"
    var release: Boolean = false

    override fun toString(): String {
        return "GitMavenRepoConfig(url='$url', gitUsername='$gitUsername', gitPassword='$gitPassword', repoDir='$repoDir', release=$release)"
    }

}


class GitMavenRepository(val config: GitMavenRepoConfig) {
    var logger = Logging.getLogger(GitMavenRepository::class.java)

    init {
        require(config.url.isNotBlank(), { "Url must not be empty" })
        if (!File(config.repoDir).exists()) {
            cloneRepo()
        }
        val git = Git.open(File(config.repoDir))
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/master").call()
        git.pull().call()
    }

    private fun cloneRepo() {
        logger.info("Cloning repository ${config.url} to ${config.repoDir}")
        val cloneCommand = Git.cloneRepository().setURI(config.url).setDirectory(File(config.repoDir))

        if (config.url.startsWith("http")) {
            logger.info("Process http repository")
            cloneCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(config.gitUsername, config.gitPassword))
        } else {
            logger.info("Process ssh repository")
            cloneCommand.setTransportConfigCallback {
                it as SshTransport
                it.sshSessionFactory = object : JschConfigSessionFactory() {
                    override fun configure(hc: OpenSshConfig.Host?, session: Session?) {}
                }
            }
        }
        cloneCommand.call()
        logger.info("Clone repository completed.")
    }

    fun addAndPush(message: String) {
        logger.info("Pushing to remote git repository...")
        val git = Git.open(File(config.repoDir))
        git.add().addFilepattern(".").call()
        git.commit().setMessage(message).call()
        git.push().call()
        logger.info("Push artifact completed.")
    }
}

