#!/bin/sh
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

# Abort script on errors
set -e

# Use homebrew stuff
export PATH=/usr/local/bin:$PATH

# Product name and version
TARBALL_NAME=qeo-objc
FMK_NAME=Qeo
FMK_VERSION=0.0.0

if [ -n "${QEO_VERSION}" ]
then 
FMK_VERSION=${QEO_VERSION}
fi

INSTALL_DIR=${SRCROOT}/Products/${CONFIGURATION}

# Remove old tarball/dir
rm -f "${INSTALL_DIR}/*.tgz"
rm -rf "${INSTALL_DIR}/ios"

# Store current path
ROOTPATH=`pwd`

#-----------------------------------------
# Create directory layout
#-----------------------------------------
mkdir -p "${INSTALL_DIR}/ios"
mkdir -p "${INSTALL_DIR}/ios/framework"
mkdir -p "${INSTALL_DIR}/ios/doc"
mkdir -p "${INSTALL_DIR}/ios/samples"
mkdir -p "${INSTALL_DIR}/ios/samples/sample-qgaugereader"
mkdir -p "${INSTALL_DIR}/ios/samples/sample-qsimplechat"
mkdir -p "${INSTALL_DIR}/ios/samples/sample-voip-qsimplechat"

#-----------------------------------------
# Copy framework files
#-----------------------------------------
cp -R "${INSTALL_DIR}/Qeo.framework" "${INSTALL_DIR}/ios/framework/Qeo.framework"
cp -R "${INSTALL_DIR}/Qeo.bundle" "${INSTALL_DIR}/ios/framework/Qeo.bundle"

#-----------------------------------------
# Copy documentation
#-----------------------------------------
cp -R "${INSTALL_DIR}/Help/com.technicolor.${FMK_NAME}.${FMK_VERSION}.docset" "${INSTALL_DIR}/ios/doc/com.technicolor.${FMK_NAME}.${FMK_VERSION}.docset"

#-----------------------------------------
# Copy Sample: qsimplechat 
#-----------------------------------------
# copy contents of simple chat
cd "${INSTALL_DIR}/ios/samples/sample-qsimplechat"
cp -R "../../../../../../sample-qsimplechat/obj-c/qsimplechat/" qsimplechat
cp -R "../../../../../../sample-qsimplechat/obj-c/qsimplechatTests/" qsimplechatTests
cp -R "../../../../../../sample-qsimplechat/obj-c/qsimplechat.xcodeproj/" qsimplechat.xcodeproj
rm -rf "${INSTALL_DIR}/ios/samples/sample-qsimplechat/qsimplechat.xcodeproj/project.xcworkspace"
rm -rf "${INSTALL_DIR}/ios/samples/sample-qsimplechat/qsimplechat.xcodeproj/xcuserdata"

# Adapt project file to point to the new location of framework and bundle
gsed -i 's/\$(SRCROOT)\/qsimplechat\/Qeo/\$(SRCROOT)\/\.\.\/\.\.\/framework/g' qsimplechat.xcodeproj/project.pbxproj
gsed -i 's/\$(SRCROOT)\/\.\.\/\.\.\/qeo-objc\/Products\/Debug/\$(SRCROOT)\/\.\.\/\.\.\/framework/g' qsimplechat.xcodeproj/project.pbxproj
gsed -i 's/\$(SRCROOT)\/\.\.\/\.\.\/qeo-objc\/Products\/Release/\$(SRCROOT)\/\.\.\/\.\.\/framework/g' qsimplechat.xcodeproj/project.pbxproj
gsed -i 's/\.\.\/\.\.\/\.\.\/qeo-objc\/Products\/Debug\/Qeo.bundle/\.\.\/\.\.\/\.\.\/framework\/Qeo.bundle/g' qsimplechat.xcodeproj/project.pbxproj
gsed -i 's/\.\.\/\.\.\/\.\.\/qeo-objc\/Products\/Release\/Qeo.bundle/\.\.\/\.\.\/\.\.\/framework\/Qeo.bundle/g' qsimplechat.xcodeproj/project.pbxproj
gsed -i 's/\.\.\/\.\.\/qeo-objc\/Products\/Debug\/Qeo.framework/\.\.\/\.\.\/framework\/Qeo.framework/g' qsimplechat.xcodeproj/project.pbxproj
gsed -i 's/\.\.\/\.\.\/qeo-objc\/Products\/Release\/Qeo.framework/\.\.\/\.\.\/framework\/Qeo.framework/g' qsimplechat.xcodeproj/project.pbxproj

# Remove DevelopmentTeam line (still contains some dummy info, so deleting it is no problem)
gsed -i '/DevelopmentTeam/d' qsimplechat.xcodeproj/project.pbxproj

# remove old references to framework and bundle 
rm -rf  qsimplechat/Qeo

#-----------------------------------------
# Copy Sample: voip-qsimplechat 
#-----------------------------------------
# copy contents of simple chat
cd "${INSTALL_DIR}/ios/samples/sample-voip-qsimplechat"
cp -R "../../../../../../sample-qsimplechat/obj-c-voip/qsimplechat/" qsimplechat
cp -R "../../../../../../sample-qsimplechat/obj-c-voip/qsimplechatTests/" qsimplechatTests
cp -R "../../../../../../sample-qsimplechat/obj-c-voip/qsimplechat.xcodeproj/" qsimplechat.xcodeproj
rm -rf "${INSTALL_DIR}/ios/samples/sample-voip-qsimplechat/qsimplechat.xcodeproj/project.xcworkspace"
rm -rf "${INSTALL_DIR}/ios/samples/sample-voip-qsimplechat/qsimplechat.xcodeproj/xcuserdata"

# Adapt project file to point to the new location of framework and bundle
gsed -i 's/\$(SRCROOT)\/qsimplechat\/Qeo/\$(SRCROOT)\/\.\.\/\.\.\/framework/g' qsimplechat.xcodeproj/project.pbxproj
gsed -i 's/\$(SRCROOT)\/\.\.\/\.\.\/qeo-objc\/Products\/Debug/\$(SRCROOT)\/\.\.\/\.\.\/framework/g' qsimplechat.xcodeproj/project.pbxproj
gsed -i 's/\$(SRCROOT)\/\.\.\/\.\.\/qeo-objc\/Products\/Release/\$(SRCROOT)\/\.\.\/\.\.\/framework/g' qsimplechat.xcodeproj/project.pbxproj
gsed -i 's/\.\.\/\.\.\/\.\.\/qeo-objc\/Products\/Debug\/Qeo.bundle/\.\.\/\.\.\/\.\.\/framework\/Qeo.bundle/g' qsimplechat.xcodeproj/project.pbxproj
gsed -i 's/\.\.\/\.\.\/\.\.\/qeo-objc\/Products\/Release\/Qeo.bundle/\.\.\/\.\.\/\.\.\/framework\/Qeo.bundle/g' qsimplechat.xcodeproj/project.pbxproj
gsed -i 's/\.\.\/\.\.\/qeo-objc\/Products\/Debug\/Qeo.framework/\.\.\/\.\.\/framework\/Qeo.framework/g' qsimplechat.xcodeproj/project.pbxproj
gsed -i 's/\.\.\/\.\.\/qeo-objc\/Products\/Release\/Qeo.framework/\.\.\/\.\.\/framework\/Qeo.framework/g' qsimplechat.xcodeproj/project.pbxproj

# Remove DevelopmentTeam line (still contains some dummy info, so deleting it is no problem)
gsed -i '/DevelopmentTeam/d' qsimplechat.xcodeproj/project.pbxproj

# remove old references to framework and bundle 
rm -rf  qsimplechat/Qeo

#-----------------------------------------
# Copy Sample: qgauge 
#-----------------------------------------
# Create temp dir with contents of qgauge reader
cd "${INSTALL_DIR}/ios/samples/sample-qgaugereader"
cp -R "../../../../../../sample-qgauge/qgaugereader/" .
rm -rf "${INSTALL_DIR}/ios/samples/sample-qgaugereader/qgaugereader.xcodeproj/project.xcworkspace"
rm -rf "${INSTALL_DIR}/ios/samples/sample-qgaugereader/qgaugereader.xcodeproj/xcuserdata"


# Adapt project file to point to the new location of framework and bundle
gsed -i 's/\.\.\/\.\.\/qeo-objc\/Products\/Debug/\.\.\/\.\.\/framework/g' qgaugereader.xcodeproj/project.pbxproj
gsed -i 's/\.\.\/\.\.\/qeo-objc\/Products\/Release/\.\.\/\.\.\/framework/g' qgaugereader.xcodeproj/project.pbxproj
gsed -i 's/\$(SRCROOT)\/\.\.\/\.\.\/qeo-objc\/Products\/Debug/\$(SRCROOT)\/\.\.\/\.\.\/framework/g' qgaugereader.xcodeproj/project.pbxproj
gsed -i 's/\$(SRCROOT)\/\.\.\/\.\.\/qeo-objc\/Products\/Release/\$(SRCROOT)\/\.\.\/\.\.\/framework/g' qgaugereader.xcodeproj/project.pbxproj
gsed -i '/suchi/d' qgaugereader.xcodeproj/project.pbxproj

# Hack: replace DevelopmentTeam line with dummy content (Xcode cannot handle empty info)
# SystemCapabilities = {com.apple.BackgroundModes={enabled = 0;};};
gsed -i 's/DevelopmentTeam.*/SystemCapabilities=\{com.apple.BackgroundModes=\{enabled = 0;\};\};/g' qgaugereader.xcodeproj/project.pbxproj

#----------------------------------------
# compress tar file
#----------------------------------------
cd "${INSTALL_DIR}"

if [ "${CONFIGURATION}" = "Release" ]
then
    tar czf "${TARBALL_NAME}.tgz" --exclude="xcuserdata" --exclude ".*" --exclude "*.gradle" "ios"
else
    tar czf "${TARBALL_NAME}-debug.tgz" --exclude="xcuserdata" --exclude ".*" --exclude "*.gradle" "ios"
fi;

# Remove temp dir
rm -rf "${INSTALL_DIR}/ios"

# restore
cd ${ROOTPATH}

# Tell the log we are done here !
echo "${TARBALL_NAME}.tgz has been built successfully !"