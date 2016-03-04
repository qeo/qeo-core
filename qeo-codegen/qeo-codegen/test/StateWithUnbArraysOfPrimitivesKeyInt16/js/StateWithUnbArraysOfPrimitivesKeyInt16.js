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
if (typeof Qeo === "undefined") {Qeo = {registerType: function(td) {this.ttr.push(td);},ttr: []};}

Qeo.registerType({
"topic": "org::qeo::test::StateWithUnbArraysOfPrimitivesKeyInt16",
"behavior": "state",
"properties": {
"MyUnbArrayOfBoolean": {
"type": "array", 
"items": {
"type": "boolean"
} 
}, "MyUnbArrayOfByte": {
"type": "array", 
"items": {
"type": "byte"
} 
}, "MyUnbArrayOfInt16": {
"type": "array", "key": true,
"items": {
"type": "int16"
} 
}, "MyUnbArrayOfInt32": {
"type": "array", 
"items": {
"type": "int32"
} 
}, "MyUnbArrayOfInt64": {
"type": "array", 
"items": {
"type": "int64"
} 
}, "MyUnbArrayOfFloat32": {
"type": "array", 
"items": {
"type": "float32"
} 
}, "MyUnbArrayOfString": {
"type": "array", 
"items": {
"type": "string"
} 
}
}
});

