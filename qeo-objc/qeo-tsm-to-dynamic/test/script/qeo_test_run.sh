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

export QEO_FWD_DISABLE_LOCATION_SERVICE=1

SELF_DIR=$(cd $(dirname $0) > /dev/null; pwd)

. ${SELF_DIR}/addon-valgrind.source

VG_ARGS="/tmp qeo"
VG=$(valgrind_cmd ${VG_ARGS} ${SELF_DIR}/valgrind.supp)

# Avoid interference from other apps by using non-default domain
if [ -z "${QEO_DOMAIN_ID}" ]; then
    export QEO_DOMAIN_ID=37
fi

# run unit tests
echo "=== UNIT TESTS ==="

# need credentials for unit tests
export QEO_STORAGE_DIR=${SELF_DIR}/../share/home.qeo/

# remove lock file
rm -f /tmp/.qeo*.lock

if [ -d ${SELF_DIR}/../lib/unittests ]; then
    clean_valgrind ${VG_ARGS}
    CK_DEFAULT_TIMEOUT=20 ${VG} ${SELF_DIR}/unittest --suitedir ${SELF_DIR}/../lib/unittests --all --nml || exit 1
    check_valgrind ${VG_ARGS} || exit 1
fi

if [ -d ${SELF_DIR}/../lib/unittests_novg ]; then
    # don't abort on Valgrind error
    CK_DEFAULT_TIMEOUT=20 ${VG} ${SELF_DIR}/unittest --suitedir ${SELF_DIR}/../lib/unittests_novg --all || exit 1
fi


echo "=== DONE ==="

exit 0
