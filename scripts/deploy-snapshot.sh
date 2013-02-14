#!/bin/bash -eu
# -e: Exit immediately if a command exits with a non-zero status.
# -u: Treat unset variables as an error when substituting.

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

mvn -f $script_dir/../pom.xml  clean deploy
echo
read -p "deploy finished press [Enter] key to finish..."
echo
