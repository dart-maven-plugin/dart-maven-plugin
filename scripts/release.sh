#!/bin/bash -eu
# -e: Exit immediately if a command exits with a non-zero status.
# -u: Treat unset variables as an error when substituting.

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

mvn -f $script_dir/../pom.xml release:clean
echo
read -p "release:clean finished press [Enter] key to go to release:prepare..."
echo

mvn -f $script_dir/../pom.xml release:prepare
echo
read -p "release:prepare finished press [Enter] key to go to release:perform..."
echo

mvn -f $script_dir/../pom.xml release:perform
echo
read -p "release:perform finished press [Enter] key to go to nexus:staging-close..."
echo

mvn -f $script_dir/../pom.xml nexus:staging-close
echo
read -p "nexus:staging-close finished press [Enter] key to finish..."
echo
