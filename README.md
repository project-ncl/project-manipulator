# project-manipulator

Project Manipulator is a tool used to manipulate project definition files. Current focus is on NPM support, but other formats can be added in the future.
Various manipulations can be performed on a project and their execution can be controlled by provided arguments.


# Usage

```
usage: ...
 -t,--type                The project type. Can be only NPM for now and is not mandatory.
                          It is not case-sensitive.
 -f,--file <arg>          Project definition file
 -r,--result <arg>        Json file to be generated at the end of manipulation containing
                          the results. Is not mandatory.
 -D <arg>                 Java properties
 -h,--help                Print help
 -l,--log <arg>           Log file to output logging to
    --log-context <arg>   Add log-context ID
 -d,--debug               Enable debug
 -c,--trace               Enable trace
```
e.g.
```
java -jar project-manipulator-cli.jar -f npm-project/package.json
```

# Java Properties

You can specify java system properties in the CLI by using `-D<prop>=<value>`

| Java Property | Description |
| --- | --- |
| restURL | Dependency Analysis REST URL (e.g http://da.url.com/da/rest/v-1). It is used and required when user wants to perform automatic version increment. |
| restConnectionTimeout | Optional connection timeout to set for the underlying HTTP client library responsible for calling the REST endpoints. Defaults to 30 seconds.|
| restSocketTimeout | Optional socket timeout to set for the underlying HTTP client library responsible for calling the REST endpoints. Defaults to 10 minutes.|
| repositoryGroup | A repository group used by Dependency Analysis to read existing versions with the same base version and suffix. It is used and required when user wants to perform automatic version increment. |
| versionIncrementalSuffix | The version suffix to append to version of current project. It is used when automatic version increment is performed. |
| versionIncrementalSuffixPadding | With automatic version increment, it is possible to configure zero-padding for the incremented number in the suffix. Defaults to 5. |
| versionOverride | Desired version used as the output of version manipulation overriding all the logic. If set, the manipulation only updates project version to this and does not do anything else (no communication with Dependency Analysis, no suffix computation etc). |
| versionSuffixOverride | Desired version suffix, that will be appended to the current version. It overrides the logic computing the suffix number automatically. |
| manipulation.disable | default: false, specify whether you want to disable the manipulation of the version or not |
| dependencyOverride.$package_name | Desired version(s) to apply to the specified package(s), if listed inside the _dependencies_ in package.json. Does not replace the values in lock files. Example: `-DdependencyOverride.keycloak-admin-client=^0.12.0 -DdependencyOverride.async=1.5.2`|
| devDependencyOverride.$package_name | Desired version(s) to apply to the specified package(s), if listed inside the _devDependencies_ in package.json. Does not replace the values in lock files. Example: `-DdevDependencyOverride.keycloak-admin-client=^0.12.0`|


# Notes

The project is inspired by and partially based on
[POM Manipulation Extension](https://github.com/release-engineering/pom-manipulation-ext)
