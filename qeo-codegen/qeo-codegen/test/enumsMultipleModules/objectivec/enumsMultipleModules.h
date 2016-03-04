/*
 * Copyright (c) 2016 - Qeo LLC
 *
 * The source code form of this Qeo Open Source Project component is subject
 * to the terms of the Clear BSD license.
 *
 * You can redistribute it and/or modify it under the terms of the Clear BSD
 * License (http://directory.fsf.org/wiki/License:ClearBSD). See LICENSE file
 * for more details.
 *
 * The Qeo Open Source Project also includes third party Open Source Software.
 * See LICENSE file for more details.
 */

/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#import <Foundation/Foundation.h>
#import <Qeo/Qeo.h>

typedef NS_ENUM(int32_t, org_qeo_test_EnumName_t) {
  ORG_QEO_TEST_ENUMNAME_ENUM1,
  ORG_QEO_TEST_ENUMNAME_ENUM2
};

typedef NS_ENUM(int32_t, org_qeo_test_EnumNameBis_t) {
  ORG_QEO_TEST_ENUMNAMEBIS_ENUM1,
  ORG_QEO_TEST_ENUMNAMEBIS_ENUM2
};

/**
 * Struct containing enums.
 */
@interface org_qeo_test_MyStructWithEnums : QEOType

  @property (nonatomic) BOOL MyBoolean;
  @property (nonatomic) int8_t MyByte;
  @property (nonatomic) int16_t MyInt16;
  @property (nonatomic) org_qeo_test_EnumName MyEnum;

@end

/**
 * Struct containing enums.
 */
@interface org_qeo_test_MyStructWithEnumsBis : QEOType

  @property (nonatomic) int32_t MyInt32;
  @property (nonatomic) int64_t MyInt64;
  @property (nonatomic) org_qeo_test_EnumNameBis MyEnumBis;

@end

typedef NS_ENUM(int32_t, org_qeo_testo_EnumName_t) {
  ORG_QEO_TESTO_ENUMNAME_ENUM3,
  ORG_QEO_TESTO_ENUMNAME_ENUM4
};

typedef NS_ENUM(int32_t, org_qeo_testo_EnumNameBis_t) {
  ORG_QEO_TESTO_ENUMNAMEBIS_ENUM1,
  ORG_QEO_TESTO_ENUMNAMEBIS_ENUM2
};

/**
 * Struct containing enums.
 */
@interface org_qeo_testo_MyStructWithEnums : QEOType

  @property (nonatomic) float MyFloat32;
  @property (strong,nonatomic) NSString * MyString;
  @property (nonatomic) org_qeo_testo_EnumName MyEnum;

@end

/**
 * Struct containing enums.
 */
@interface org_qeo_testo_MyStructWithEnumsBis : QEOType

  @property (nonatomic) BOOL MyBooleanBis;
  @property (nonatomic) int8_t MyByteBis;
  @property (nonatomic) int16_t MyInt16Bis;
  @property (nonatomic) org_qeo_testo_EnumNameBis MyEnumBis;

@end


