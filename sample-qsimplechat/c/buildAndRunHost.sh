#!/bin/bash
# Copyright (c) 2015 - Qeo LLC
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


EXTRA_ARGS=""
if [ "$DEBUG" = "1" ]
then
    EXTRA_ARGS="${EXTRA_ARGS} -PcSdkSampleDebug=true"
fi
if [ "${NO_FORCE_32_BIT}" = "1" ]
then
    EXTRA_ARGS="${EXTRA_ARGS} -Pforce32bit=false"
fi

echo "gradle assemble  -PdisableMips=true -PdisableRpi=true ${EXTRA_ARGS}"
gradle assemble  -PdisableMips=true -PdisableRpi=true ${EXTRA_ARGS}
if [ $? -ne 0 ]; then
    exit 1
fi

echo "LD_LIBRARY_PATH=output/gradle/qeo-sdk/c/lib/i686-linux ./output/gradle/sdk-src/sample-qsimplechat-c-HOSTLINUX"
LD_LIBRARY_PATH=output/gradle/qeo-sdk/c/lib/i686-linux ./output/gradle/sdk-src/sample-qsimplechat-c-HOSTLINUX

