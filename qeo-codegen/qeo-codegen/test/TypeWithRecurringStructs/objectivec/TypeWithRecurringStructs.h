/*
 * Copyright (c) 2015 - Qeo LLC
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

@interface org_qeo_dynamic_qdm_test_Substruct1 : QEOType

  @property (strong,nonatomic) NSString * msubstring;
  @property (nonatomic) int32_t msubint32;

@end

@interface org_qeo_dynamic_qdm_test_Substruct2 : QEOType

  @property (nonatomic) int16_t msubshort;
  @property (strong,nonatomic) NSString * msubstring;
  /**
   * Array of org_qeo_dynamic_qdm_test_Substruct1
   */
  @property (strong,nonatomic) NSArray * msubstruct1;

@end

@interface org_qeo_dynamic_qdm_test_Substruct3 : QEOType

  @property (strong,nonatomic) NSString * msubstring;
  /**
   * Array of org_qeo_dynamic_qdm_test_Substruct2
   */
  @property (strong,nonatomic) NSArray * msubstruct2;
  /**
   * Array of org_qeo_dynamic_qdm_test_Substruct1
   */
  @property (strong,nonatomic) NSArray * msubstruct1;
  @property (nonatomic) float msubfloat;

@end

@interface org_qeo_dynamic_qdm_test_House : QEOType

  /**
   * Array of org_qeo_dynamic_qdm_test_Substruct1
   */
  @property (strong,nonatomic) NSArray * msubstruct1;
  /**
   * Array of org_qeo_dynamic_qdm_test_Substruct3
   */
  @property (strong,nonatomic) NSArray * msubstruct3;
  /**
   * Array of org_qeo_dynamic_qdm_test_Substruct2
   */
  @property (strong,nonatomic) NSArray * msubstruct2;
  @property (nonatomic) float mfloat32;
  @property (strong,nonatomic) NSString * mstring;

@end


