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


SELF_DIR=$(cd $(dirname $0) > /dev/null; pwd)
PREFIX=${SELF_DIR}/usr/local

NUM=$1

export LD_LIBRARY_PATH=${PREFIX}/lib
export LD_PRELOAD=${PREFIX}/lib/libstoragepath.so
export QEO_STORAGE_DIR=${PREFIX}/share/home.qeo/user_${NUM}/

./output/gradle/sdk-src/sample-qsimplechat-c-policy-HOSTLINUX

