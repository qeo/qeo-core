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

# Sets the target folders and the final framework product.
# Name must be the same as Product name of the target
FMK_NAME=Qeo
FMK_VERSION=0.0.0

if [ -n "${QEO_VERSION}" ]
then 
FMK_VERSION=${QEO_VERSION}
fi

# Follows the config of the Scheme:
# XCode: Switch between Debug and Release: left click on active scheme
#        select Edit scheme, change combo-box to Release/Debug


# Install dir will be the final output to the framework.
# The following line creates it in the root folder of the current
# project.
INSTALL_DIR=${SRCROOT}/Products/${CONFIGURATION}/${FMK_NAME}.framework

# Working dir will be deleted after the framework creation.
WRK_DIR=${SRCROOT}/build
DEVICE_DIR=${WRK_DIR}/${CONFIGURATION}-iphoneos/${FMK_NAME}.framework
SIMULATOR_DIR=${WRK_DIR}/${CONFIGURATION}-iphonesimulator/${FMK_NAME}.framework

echo "******************************************************"
echo "DEVICE_DIR = ${DEVICE_DIR}"
echo "SIMULATOR_DIR = ${SIMULATOR_DIR}"
echo "******************************************************"
echo "SYMROOT = ${SYMROOT}"
echo "OBJROOT = ${OBJROOT}"
echo "PROJECT_DIR = ${PROJECT_DIR}"
echo "CONFIGURATION_BUILD_DIR = ${CONFIGURATION_BUILD_DIR}"
echo "CONFIGURATION = ${CONFIGURATION}"
echo "CONFIGURATION_TEMP_DIR = ${CONFIGURATION_TEMP_DIR}"
echo "DERIVED_FILE_DIR = ${DERIVED_FILE_DIR}"
echo "BUILD_PRODUCTS_DIR = ${BUILT_PRODUCTS_DIR}"
echo "BUILD_DIR = ${BUILD_DIR}"
echo "TARGET_TEMP_DIR = ${TARGET_TEMP_DIR}"
echo "PROJECT_TEMP_DIR = ${PROJECT_TEMP_DIR}"
echo "FMK_NAME = ${FMK_NAME}"
echo "FMK_VERSION = ${FMK_VERSION}"
echo "******************************************************"


# Building both architectures.
xcodebuild -configuration ${CONFIGURATION} -target "${FMK_NAME}" -sdk iphoneos
xcodebuild -configuration ${CONFIGURATION} -target "${FMK_NAME}" -sdk iphonesimulator

# Cleaning the oldest.
if [ -d "${INSTALL_DIR}" ]
then
rm -rf "${INSTALL_DIR}"
fi

# Creates and renews the final product folder.
mkdir -p "${INSTALL_DIR}"
mkdir -p "${INSTALL_DIR}/Versions"
mkdir -p "${INSTALL_DIR}/Versions/${FMK_VERSION}"


echo "Copy headers"
# Copies the headers and resources files to the final product folder.
cp -R "${DEVICE_DIR}/Headers/" "${INSTALL_DIR}/Versions/${FMK_VERSION}/Headers"
cp -R "${DEVICE_DIR}/" "${INSTALL_DIR}/Versions/${FMK_VERSION}/Resources"
mkdir -p "${INSTALL_DIR}/Versions/${FMK_VERSION}/Headers/dds"
cp "${SRCROOT}/../qeo-c/qeo-native/Products/qeo-c-core.framework/Headers/dds/dds_dcps.h" "${INSTALL_DIR}/Versions/${FMK_VERSION}/Headers/dds"
cp "${SRCROOT}/../qeo-c/qeo-native/Products/qeo-c-core.framework/Headers/dds/dds_tsm.h" "${INSTALL_DIR}/Versions/${FMK_VERSION}/Headers/dds"
cp "${SRCROOT}/../qeo-c/qeo-native/Products/qeo-c-core.framework/Headers/dds/dds_types.h" "${INSTALL_DIR}/Versions/${FMK_VERSION}/Headers/dds"
cp "${SRCROOT}/../qeo-c/qeo-native/Products/qeo-c-core.framework/Headers/dds/dds_seq.h" "${INSTALL_DIR}/Versions/${FMK_VERSION}/Headers/dds"
cp "${SRCROOT}/../qeo-c/qeo-native/Products/qeo-c-core.framework/Headers/dds/dds_error.h" "${INSTALL_DIR}/Versions/${FMK_VERSION}/Headers/dds"
cp "${SRCROOT}/../qeo-c/qeo-native/Products/qeo-c-core.framework/Headers/qeo/error.h" "${INSTALL_DIR}/Versions/${FMK_VERSION}/Headers"

echo "Replace qeo-c-core by Qeo in header files"
gsed -i "s/qeo-c-core/Qeo/" "${INSTALL_DIR}/Versions/${FMK_VERSION}/Headers/dds/"dds_*h "${INSTALL_DIR}/Versions/${FMK_VERSION}/Headers/Qeo.h"


# Removes the binary and header from the resources folder.
rm -r "${INSTALL_DIR}/Versions/${FMK_VERSION}/Resources/Headers" "${INSTALL_DIR}/Versions/${FMK_VERSION}/Resources/${FMK_NAME}"

# Creates the internal links.
# It MUST uses relative path, otherwise will not work when the folder is copied/moved.
ln -s "${FMK_VERSION}" "${INSTALL_DIR}/Versions/Current"
ln -s "Versions/Current/Headers" "${INSTALL_DIR}/Headers"
ln -s "Versions/Current/Resources" "${INSTALL_DIR}/Resources"
ln -s "Versions/Current/${FMK_NAME}" "${INSTALL_DIR}/${FMK_NAME}"


echo "Copy simulator (binary)"
lipo -create ${SIMULATOR_DIR}/${FMK_NAME} ${DEVICE_DIR}/${FMK_NAME} -output "${INSTALL_DIR}/Versions/${FMK_VERSION}/${FMK_NAME}"  

echo "Clean"
# Remove the working dir
rm -r "${WRK_DIR}"

# Tell the log we are done here !
echo "${FMK_NAME}.framework has been built successfully !"
