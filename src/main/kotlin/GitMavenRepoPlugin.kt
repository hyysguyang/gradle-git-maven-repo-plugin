package com.lifecosys.gradle

import org.eclipse.jgit.api.Git
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import java.io.File

class GitMavenRepoPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        with(project){
            extensions.create("gitMavenRepo", GitMavenRepoConfig::class.java)

            afterEvaluate {
                val gitMavenRepoConfig = extensions.getByName("gitMavenRepo") as GitMavenRepoConfig
                val repo = GitMavenRepository(gitMavenRepoConfig.url)

                repositories.maven {
                    it.setUrl(repo.repoDir)
                }
                val publishingExtension = extensions.findByType(PublishingExtension::class.java)



                if (publishingExtension != null) {
                    publishingExtension.repositories.maven {
                        it.setUrl(repo.repoDir)
                    }
                    if (gitMavenRepoConfig.release) {
                        tasks.getByName("publish").doLast {
                            val message = "Release ${group}:${name}:${version} "
                            repo.addAndPush(message)
                        }
                    }
                }
            }
        }


    }
}

open class GitMavenRepoConfig{
    lateinit var url: String
    var release: Boolean = false
}

class GitMavenRepository(val url: String) {
    val repoDir = "${System.getProperty("user.home")}/.ehome/maven-repo/ehome-base"
    val git = initGit()
    private fun initGit(): Git {
        return if (!File(repoDir).exists()) {
            Git.cloneRepository().setURI(url).setDirectory(File(repoDir)).call()
        } else Git.open(File(repoDir))
    }

    fun addAndPush(message: String) {
        git.add().addFilepattern(".").call()
        git.commit().setMessage(message).call()
        git.push().call()
    }
}

