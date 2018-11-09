# project-manipulator
Project Manipulator is a tool used to manipulate project definition files. Current focus is on NPM support, but other formats can be added in the future.
Various manipulations can be performed on a project and their execution can be controlled by provided arguments.

# Usage

```
usage: ...
 -d,--debug               Enable debug
 -D <arg>                 Java Properties
 -f,--file <arg>          Project definition file
 -h,--help                Print help
 -l,--log <arg>           Log file to output logging to
    --log-context <arg>   Add log-context ID
 -t,--trace               Enable trace
```
e.g.
```
java -jar project-manipulator-cli.jar -f npm-project/package.json
```
