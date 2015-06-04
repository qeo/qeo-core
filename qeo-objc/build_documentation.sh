#!/bin/sh
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

# Abort script on errors
set -e

# Use homebrew stuff
export PATH=/usr/local/bin:$PATH

INSTALL_DIR=${SRCROOT}/Products/${CONFIGURATION}/Help

# Create output dir
rm -rf "${INSTALL_DIR}"
mkdir -p "${INSTALL_DIR}"

# Product name and version
FMK_NAME=Qeo
FMK_VERSION=0.0.0

if [ -n "${QEO_VERSION}" ]
then 
FMK_VERSION=${QEO_VERSION}
fi

# Generate docset (will also automatically be installed in: ~/Library/Developer/Shared/Documentation/DocSets, unless you provide --no-install-docset)
# Goal is to take only Qeo.h to generate documentation
# the --ignore option uses only suffix as pattern matching
/usr/local/bin/appledoc \
--preprocess-headerdoc \
--project-name "${FMK_NAME}" \
--project-version "${FMK_VERSION}" \
--project-company "Technicolor" \
--company-id "com.technicolor" \
--output "${INSTALL_DIR}" \
--docset-feed-name "${FMK_NAME} ${FMK_VERSION} doc set" \
--docset-bundle-name "${FMK_NAME} ${FMK_VERSION} doc set" \
--docset-min-xcode-version "5.0" \
--create-docset \
--install-docset \
--logformat xcode \
--keep-undocumented-objects \
--keep-undocumented-members \
--keep-intermediate-files \
--no-repeat-first-par \
--no-warn-invalid-crossref \
--merge-categories \
--exit-threshold 2 \
--docset-platform-family iphoneos \
--ignore "*.m" \
--ignore "*y.h" \
--ignore "*r.h" \
--ignore "*m.h" \
--ignore "*pe.h" \
--ignore "*o.h" \
--ignore "*e.h" \
--ignore "*t.h" \
--index-desc "${SRCROOT}/Qeo/documentation/readme.markdown" \
"${SRCROOT}/Qeo"

# Rename the docset dir
mv "${INSTALL_DIR}/docset" "${INSTALL_DIR}/com.technicolor.${FMK_NAME}.${FMK_VERSION}.docset"

# Tell the log we are done here !
echo "com.technicolor.${FMK_NAME}.${FMK_VERSION}.docset has been built successfully !"
