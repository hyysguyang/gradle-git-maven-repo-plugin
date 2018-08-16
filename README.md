## Usage

Apply plugin

```groovy
plugins {
  id "com.lifecosys.gradle.git-maven-repo" version "0.7"
}
```

and config with the git url

```groovy
gitMavenRepo {
    url = "https://github.com/USERNAME/PROJECT"
    gitUsername = "GIT USERNAME" //If use http authentication, such as gitlab or github.
    gitPassword = "GIT USER PASSWORD" //If use http authentication, such as gitlab or github.
    repoDir = "LOCAL REPOSITORY" //Default is ~/.gitMavenRepo
    release = true // To deploy the artifact to the Remote GitMavenRepo 
}
```

Or

```groovy
gitMavenRepo {
        url = "git@gitlab.com:360ehome/maven-repo.git"
        release = project.hasProperty("release")
        sshConfig = [StrictHostKeyChecking:'no']
}
```

Please note that you must config your git with ssh key to clone or push change.