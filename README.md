#Maven Dart Plugin

The Dart Maven Plugin provides integration for Google Dart into a maven build process. It looks for folders with [dart package layout](http://pub.dartlang.org/doc/package-layout.html)

#Changenotes

###3.0.0
* `dart:dart2js`  Has now the possibility to span mutliple compiler sessions in parallel with ```threadCount``` 
* `dart:pub`      Is now full flexible by providing ```pubCommand``` e.g. ```get``` and a list of ```pubOptions``` e.g. ```--no-offline```

#Setup

The plugin needs a dart-sdk installed on the system it should be executed. By default it will look for the environment variable DART_SDK. So please set this variable.
It is possible to overwrite this in the plugin configuration section.

#Artifact coordinates

The plugin is released as com.github.dzwicker.dart:dart-maven-plugin

#Requirements

The Dart Maven Plugin needs maven 3.0+ and JDK 7.

#Goals Overview

The Dart Plugin has one goal (besides the help goal). It is already bound to his proper phase within the Maven Lifecycle (compile) and is therefore, automatically executed during his respective phase.

* `dart:dart` Goal to invoke the dart scripts.
* `dart:pub` Goal to invoke pub the dart package manager.
* `dart:dart2js` Goal to compile dart files to javascript.
* `dart:dwc` Goal to invoke the dart web compiler.
* `dart:test` Goal to invoke the dart scripts.
* `dart:help` Display help information on dart-maven-plugin. Call mvn dart:help -Ddetail=true -Dgoal=<goal-name> to display parameter details.

#Usage

The dart2js goal uses the dart2js compiler from the dart project to compile any dart file to javascript (js file) and a map.js file. It will download the latest dart SDK into the dartOutputDirectory [${project.build.directory}/dependency/dart] for the used OS and architecture. By default it will look for dart files in src/main/dart and will place the javascript files under ${project.build.directory}/dart.

More configuration details:

Please use mvn dart:help for more information.

#Example

An example can be found in the dart-maven-plugin-example folder inside the git repository.

#Maven repository

The Dart Maven Plugin is deployed to the central repository.

# Authors and Contributors
[Daniel Zwicker]() founded the project on GitHub.

### Committer

* [Oliver Lietz](https://github.com/oliverlietz)

* [@youngm](https://github.com/youngm)

* [Chris Buckett](https://github.com/chrisbu)

* [@magnayn](https://github.com/magnayn)

* [@mrwonderman](https://github.com/mrwonderman) 

