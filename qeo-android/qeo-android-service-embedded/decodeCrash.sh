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


if [[ -z "${2}" ]]
then
    echo "Script to decode crash dump from a release version of the service"
    echo "Usage: \"$0 arch version crashdump\" where:"
    echo "  arch: architecture used on the crashed device (either \"x86\" or \"armeabi\""
    echo "  version: either \"debug\" or \"release\""
    echo "  crashdump: a file containing logcat output of the crash"
    exit 1
fi

if [[ -z "${ANDROID_NDK}" ]]
then
    ANDROID_NDK=/home/users/cpeqeo/tools/android-ndk-linux
fi

if [[ "${1}" != "x86" && "${1}" != "armeabi" ]]
then
    echo "ERROR: first parameter must be \"x86\" or \"armeabi\""
    exit 1
fi

if [[ "${2}" != "debug" && "${2}" != "release" ]]
then
    echo "ERROR: 2nd parameter must be \"debug\" or \"release\""
    exit 1
fi

echo "Decoding crash"
echo "WARNING: this output will only be accurate if the crash lines up EXACTLY with these so files!"
DIR=`dirname $0`
if [ "${2}" = "debug" ]
then
    VERSION="gdb"
else
    VERSION="release-symbols"
fi

SYMBOLS="${DIR}/build/${VERSION}/lib/${1}"
echo "Using symbols from ${SYMBOLS}"
${ANDROID_NDK}/ndk-stack -sym ${SYMBOLS} -dump ${3}

