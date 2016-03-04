#!/bin/bash
# Copyright (c) 2016 - Qeo LLC
#
# The source code form of this Qeo Open Source Project component is subject
# to the terms of the Clear BSD license.
#
# You can redistribute it and/or modify it under the terms of the Clear BSD
# License (http://directory.fsf.org/wiki/License:ClearBSD). See LICENSE file
# for more details.
#
# The Qeo Open Source Project also includes third party Open Source Software.
# See LICENSE file for more details.


#NOTE: this script is not fully automatic and assumes the following
#1. An androVM image is alreay installed (api at least 18)
#2. Name of the image is "QeoAndroidUI_1"
#3. Device has host-only interface, configured as following:
#   - ip address 192.168.68.1
#   - DHCP server 192.168.68.100
#   - DHCP start/end pool 192.168.68.101


ADB=${ANDROID_HOME}/platform-tools/adb
VMNAME="QeoAndroidUI_1"
VMIP="192.168.68.101"

function startvm()
{
    ${ADB} kill-server
    vboxmanage startvm --type headless ${VMNAME}
    sleep 5
}

function connectvm()
{
    ${ADB} connect ${VMIP}:5555
    for i in `seq 1 30`
    do
        echo "Checking for device to boot (try ${i})"
        ${ADB} shell getprop dev.bootcomplete | grep 1 > /dev/null
        if [ $? = 0 ]; then
            echo "Boot completed"
            break
        fi
        if [ ${i} = 30 ]; then
            echo "Device did not boot" >&2
            exit 1
        fi
        sleep 1
    done
}

function stopvm()
{
    vboxmanage controlvm ${VMNAME} poweroff
    ${ADB} kill-server
}

case "$1" in
    start)
        startvm
        connectvm
        ;;
    stop)
        stopvm
        ;;
    connect)
        connectvm
        ;;
    *)
        echo "start script with start|stop|connect" >&2
        exit 1
        ;;
esac

