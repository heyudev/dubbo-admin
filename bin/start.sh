#!/bin/bash
profile=$1
if [[ ${profile} != "" ]]; then
    profile="-Dspring.profiles.active=${profile}"
fi
echo "starting ..."
nohup java -jar ${profile} dubbo-admin-distribution/target/dubbo-admin-0.1.jar > /dev/null 2>&1 &
echo "start completed"

