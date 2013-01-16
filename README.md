#Maven Dart Plugin

The Dart Maven Plugin provides integration of Dart into a maven build process.

#Important!!!!

Dart has moved to a GoogleStore server. This Server needs authentication to download the new SDK. 
The Plugin is not capable to login in. So you have to download and install the SDK to your maven repository manually, or use the `dartSdk` configuration setting 
to let maven know where the dart sdk has been unzipped to.

##Example upload command

`mvn deploy:deploy-file -DrepositoryId=in2ex-public-releases -DgeneratePom=true -DgroupId=com.google.dart -DartifactId=dart-sdk -D version=15699 -Dpackaging=zip -Dclassifier=macos-64 -Dfile=dartsdk-macos-64.zip -Durl=http://dev.in2experience.com/nexus/content/repositories/public-release`

##Please add the following configuration to your pom

* `<dartVersion>YOUR DOWNLOADED VERSION</dartVersion>`
* `<skipSDKDownload>true</skipSDKDownload>`

Or
 
* `<dartSdk>${env.DART_SDK}</dartSdk>` 
to use a pre-unzipped Dart SDK.  

Or look inside the example.

##Goals Overview

The Dart Plugin has one goal (besides the help goal). It is already bound to his proper phase within the Maven Lifecycle (compile) and is therefore, automatically executed during his respective phase.

* `dart:pub` Goal to invoke pub the dart package manager.
* `dart:dart2js` Goal to compile dart files to javascript.
* `dart:help`  Display help information on maven-dart-plugin. Call mvn dart:help -Ddetail=true -Dgoal=<goal-name> to display parameter details.

##Usage

The dart2js goal uses the dart2js compiler from the dart project to compile any dart file to javascript (js file) and a map.js file. It will download the latest dart SDK into the dartOutputDirectory [${project.build.directory}/dependency/dart] for the used OS and architecture. By default it will look for dart files in src/main/dart and will place the javascript files under ${project.build.directory}/dart.

More configuration details:

### `dart:pub` Goal to invoke pub the dart package manager.

Please use mvn dart:help for more information.

### `dart:dart2js` Goal to compile dart files to javascript. 

Please use mvn dart:help for more information.
    
##Example

An example can be found in the example folder in the git repository.

##Maven repository

The Dart Maven Plugin is provided in the In2Experience repository https://dev.in2experience.com/nexus/content/repositories/public-release/

## Authors and Contributors
Daniel Zwicker (@dzwicker) founded the project on GitHub.

### Comitter

* @youngm