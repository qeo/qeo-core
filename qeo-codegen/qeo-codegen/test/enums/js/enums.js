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
if (typeof Qeo === "undefined") {Qeo = {registerType: function(td) {this.ttr.push(td);},ttr: []};}

Qeo.registerType({
"enum": "org::qeo::test::EnumName",
"values" : {
"ENUM1": 0,
"ENUM2": 1
}                                                                                                                                                                                                      
});
Qeo.registerType({
"enum": "org::qeo::test::EnumNameBis",
"values" : {
"ENUM1BIS": 0,
"ENUM2BIS": 1
}                                                                                                                                                                                                      
});
Qeo.registerType({
"topic": "org::qeo::test::MyStructWithEnums",
"properties": {
"MyBoolean": {
"type": "boolean"
}, "MyByte": {
"type": "byte"
}, "MyInt16": {
"type": "int16"
}, "MyEnum": {
"type": "enum", 
"item": "org::qeo::test::EnumName"
}
}
});
Qeo.registerType({
"topic": "org::qeo::test::MyStructWithEnumsBis",
"properties": {
"MyFloat32": {
"type": "float32"
}, "MyString": {
"type": "string"
}, "MyEnumBis": {
"type": "enum", 
"item": "org::qeo::test::EnumNameBis"
}
}
});

