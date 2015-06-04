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
"topic": "module::first::MyStructWithPrimitives",
"properties": {
"MyBoolean": {
"type": "boolean"
}, "MyByte": {
"type": "byte"
}, "MyInt16": {
"type": "int16"
}, "MyInt32": {
"type": "int32"
}, "MyInt64": {
"type": "int64"
}, "MyFloat32": {
"type": "float32"
}, "MyString": {
"type": "string"
}
}
});
Qeo.registerType({
"topic": "module::first::Class1",
"properties": {
"upper": {
"type": "int64"
}, "lower": {
"type": "int64"
}, "MyUnbArrayOfStructWithPrimitives": {
"type": "array", 
"items": {
"type": "object",
"item": "module::first::MyStructWithPrimitives"
}
}
}
});
Qeo.registerType({
"topic": "module::second::MyStructWithPrimitives",
"properties": {
"MyBoolean": {
"type": "boolean"
}, "MyByte": {
"type": "byte"
}, "MyInt16": {
"type": "int16"
}, "MyInt32": {
"type": "int32"
}, "MyInt64": {
"type": "int64"
}, "MyFloat32": {
"type": "float32"
}, "MyString": {
"type": "string"
}
}
});
Qeo.registerType({
"topic": "module::second::Class1",
"properties": {
"upper2": {
"type": "int64"
}, "lower2": {
"type": "int64"
}
}
});

