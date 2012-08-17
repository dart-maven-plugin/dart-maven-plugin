#Maven Dart Plugin

The Dart Maven Plugin provides integration of Dart into a maven build process.

##Goals Overview

The Dart Plugin has one goal (besides the help goal). It is already bound to his proper phase within the Maven Lifecycle (compile) and is therefore, automatically executed during his respective phase.

* `dart:dart2js` Goal which compile dart files to javascript.
* `dart:help`  Display help information on maven-dart-plugin. Call mvn dart:help -Ddetail=true -Dgoal=<goal-name> to display parameter details.

##Usage

The dart2js goal uses the dart2js compiler from the dart project to compile any dart file to javascript (js file) and a map.js file. It will download the latest dart SDK into the dartOutputDirectory [${project.build.directory}/dependency/dart] for the used OS and architecture. By default it will look for dart files in src/main/dart and will place the javascript files under ${project.build.directory}/dart.

More configuration details:

* `dart:dart2js` Goal which compile dart files to javascript. 

	Available parameters:

    ** `checkedMode (Default: false)` Insert runtime type checks and enable assertions (checked mode).

    ** `dartOutputDirectory` The directory for downloading the dart SDK.

    ** `dartServerUrl` The base URL for Downloading the dart SDK from

    ** `dartVersion`  The Version of the dart SDK

    ** `excludes` A list of exclusion filters for the dart2js compiler.

    ** `executable` provide a dart2js executable

    ** `includes` A list of inclusion filters for the dart2js compiler.

    ** `outputDirectory` The directory to place the js files after compiling.

    ** `serverId` settings.xml's server id for the URL. This is used when wagon needs extra authentication information.

    ** `skip (Default: false)` Skip the execution of dart2js.
    
    ** `skipVM (Default: false)` Skip downloading dart VM.

    ** `staleMillis (Default: 0)` Sets the granularity in milliseconds of the last modification date for testing whether a dart source needs recompilation.
    
##Example

An example can be found in the example folder in the git repository.

##Maven repository

The Dart Maven Plugin is provided in the In2Experience repository https://dev.in2experience.com/nexus/content/repositories/public-release/
