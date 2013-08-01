#!/bin/bash

source ./../init.sh

curl -H "mode: $MODE" \
	 -H "Content-Type: application/json" \
	 -H "username: $1" \
	 -H "password: $2" \
	 -H "login: $3" \
	 -H "campaignID: $4" \
	 $URL_CLIENT"/bid/runoptimization"

echo	