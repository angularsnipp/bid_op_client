#!/bin/bash

#mode: test, dev, prod
MODE="test"

URL_DEV_CLIENT="http://localhost:9000/api"
URL_PROD_CLIENT="http://bid-op-client.herokuapp.com/api"

URL_DEV_SERVER="http://localhost:9001"
URL_PROD_SERVER="http://bid-op-service.herokuapp.com"

case $MODE in
	"prod") 
		URL_CLIENT=$URL_PROD_CLIENT 
		URL_SERVER=$URL_PROD_SERVER
		;;
	"dev") 
		URL_CLIENT=$URL_DEV_CLIENT
		URL_SERVER=$URL_DEV_SERVER 
		;;
	"test") 
		URL_CLIENT=$URL_DEV_CLIENT
		URL_SERVER=$URL_DEV_SERVER 
		;;
esac

echo mode: $MODE
echo url_client: $URL_CLIENT
echo url_server: $URL_SERVER