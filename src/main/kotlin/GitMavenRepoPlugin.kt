package com.lifecosys.gradle

import com.jcraft.jsch.Session
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.publish.PublishingExtension
import java.io.File

val logger = Logging.getLogger(GitMavenRepoPlugin::class.java)

class GitMavenRepoPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            logger.info("Apply GiMavenRepo Plugin....")
            extensions.create("gitMavenRepo", GitMavenRepoConfig::class.java)
            afterEvaluate {
                val gitMavenRepoConfig = extensions.getByName("gitMavenRepo") as GitMavenRepoConfig
                val repo = GitMavenRepository(gitMavenRepoConfig)
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
}


class GitMavenRepository(val config: GitMavenRepoConfig) {
    init {
        if (!File(config.repoDir).exists()) {
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
        }
        Git.open(File(config.repoDir)).pull().call()
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

