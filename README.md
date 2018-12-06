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

# System Properties

You can specify system properties in the CLI by using `-D<prop>=<value>`

| System Properties | Description |
| --- | --- |
| restURL | Dependency Analysis REST URL (e.g http://da.url.com/da/rest/v-1) |
| versionIncrementalSuffix | The version suffix to append to version of current project |
| versionIncrementalSuffixPadding | With autoincrement, it is possible to configure padding for the increment |
| versionOverride | TBD |
| versionSuffixOverride |  TBD |

# Notes

The project is partially based on
[POM Manipulation Extension](https://github.com/release-engineering/pom-manipulation-ext)

