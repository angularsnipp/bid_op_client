#!/bin/bash

source ./../init.sh

curl -H "username: $1" \
	 -H "password: $2" \
	 -X POST \
	 $BASE_URL"/bid/postWordstatReport"

echo	