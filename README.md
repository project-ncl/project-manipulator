# project-manipulator

Project Manipulator is a tool used to manipulate project definition files. Current focus is on NPM support, but other formats can be added in the future.
Various manipulations can be performed on a project and their execution can be controlled by provided arguments.


# Usage

```
usage: ...
 -t,--type                The project type. Can be only NPM for now and is not mandatory.
                          It is not case-sensitive.
 -d,--debug               Enable debug
 -D <arg>                 Java Properties
 -f,--file <arg>          Project definition file
 -h,--help                Print help
 -l,--log <arg>           Log file to output logging to
    --log-context <arg>   Add log-context ID
 -r,--trace               Enable trace
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
| versionIncrementalSuffix | The version suffix to append to version of current project. It is used when automatic version increment is performed. |
| versionIncrementalSuffixPadding | With automatic version increment, it is possible to configure zero-padding for the incremented number in the suffix. |
| versionOverride | Desired version used as the output of version manipulation overriding all the logic. If set, the manipulation only updates project version to this and does not do anything else (no communication with Dependency Analysis, no suffix computation etc). |
| versionSuffixOverride | Desired version suffix, that will be appended to the current version. It overrides the logic computing the suffix number automatically. |


# Notes

The project is partially based on
[POM Manipulation Extension](https://github.com/release-engineering/pom-manipulation-ext)
