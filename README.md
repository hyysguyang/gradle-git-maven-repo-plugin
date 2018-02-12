## Usage

Apply plugin

`apply plugin: "com.lifecosys.gradle.git-maven-repo"`

and config with the git url

```gradle
gitMavenRepo {
    url = "https://github.com/USERNAME/PROJECT"
    release = true
}
```

Please note that you must config your git with ssh key to clone or push change.