#!/bin/bash

source ./init.sh

curl -H "username: $1" \
	 -H "password: $2" \
	 $URL_CLIENT"/getUsers"

echo	