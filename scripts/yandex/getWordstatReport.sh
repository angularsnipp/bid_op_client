#!/bin/bash

source ./../init.sh

curl -H "Content-Type: application/json" \
	 -H "username: $1" \
	 -H "password: $2" \
	 -H "login: $3" \
	 -H "campaignID: $4" \
	 -X POST \
	 -d @words.json \
	 $BASE_URL"/yandex/getWordstatReport"

echo	