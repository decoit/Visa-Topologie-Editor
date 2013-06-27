#!/bin/bash
DIR="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
OPT_FORCE="-f"
OPT_REMOVE="-r"

if [ ! -L /usr/bin/visabackend ];
then
	echo "Installing symbolic link '/usr/bin/visabackend'"
	sudo ln -s "$DIR/visabackend.sh" /usr/bin/visabackend
else
	if [[ -n $1 && "$1" == "$OPT_FORCE" ]]
	then
		echo "Symbolic link '/usr/bin/visabackend' already exists"

		echo "Force install selected, removing existing symbolic link"
		sudo rm /usr/bin/visabackend

		echo "Installing symbolic link '/usr/bin/visabackend'"
		sudo ln -s "$DIR/visabackend.sh" /usr/bin/visabackend
	else
		if [[ -n $1 && "$1" == "$OPT_REMOVE" ]]
		then
			echo "Removing existing symbolic link"
			sudo rm /usr/bin/visabackend
		else
			echo "Symbolic link '/usr/bin/visabackend' already exists."
			echo "Use -f to replace or -r to remove the existing symbolic link."
		fi
	fi
fi
