#!/bin/bash

source ./../init.sh

curl -H "Mode: $MODE" \
	 -H "username: $1" \
	 -H "password: $2" \
	 -X POST \
	 $URL_SERVER"/bid/postWordstatReport"

echo	