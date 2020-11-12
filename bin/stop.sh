#!/bin/bash
echo "stopping ..."
for (( i = 0; i < 3; ++i )); do
    pid=`ps ax | grep java | grep 'dubbo-admin-0.1.jar' | awk '{print $1}'`
    if [[ ${pid} != "" ]]; then
        echo "kill ${pid}"
        kill ${pid}
        sleep 3s
    else
        break
    fi
done

pid=`ps ax | grep java | grep 'dubbo-admin-0.1.jar' | awk '{print $1}'`
if [[ ${pid} != "" ]]; then
    echo "kill -9 ${pid}"
    kill -9 ${pid}
fi
echo "Stop completed"

