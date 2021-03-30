#!/bin/bash

JAR=`find ./ -name 'distbtsync-demo-*'`
PORT=$1

if [ "${PORT}" = "" ] ;then
	PORT="8080"
fi

nohup java -jar ${JAR} --server.port=${PORT} > log-distbtsync-demo-${PORT}.log 2>&1 &
