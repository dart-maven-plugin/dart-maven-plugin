#!/bin/bash -eu
# -e: Exit immediately if a command exits with a non-zero status.
# -u: Treat unset variables as an error when substituting.

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

mvn -f $script_dir/../pom.xml nexus:staging-close
read -p "Press [Enter] key to go ahead..."

mvn -f $script_dir/../pom.xml nexus:staging-promote
read -p "Press [Enter] key to finish staging..."
