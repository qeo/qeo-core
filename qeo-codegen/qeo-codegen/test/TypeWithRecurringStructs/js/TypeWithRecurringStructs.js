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
"topic": "org::qeo::dynamic::qdm::test::Substruct1",
"properties": {
"msubstring": {
"type": "string"
}, "msubint32": {
"type": "int32"
}
}
});
Qeo.registerType({
"topic": "org::qeo::dynamic::qdm::test::Substruct2",
"properties": {
"msubshort": {
"type": "int16"
}, "msubstring": {
"type": "string"
}, "msubstruct1": {
"type": "array", 
"items": {
"type": "object",
"item": "org::qeo::dynamic::qdm::test::Substruct1"
}
}
}
});
Qeo.registerType({
"topic": "org::qeo::dynamic::qdm::test::Substruct3",
"properties": {
"msubstring": {
"type": "string"
}, "msubfloat": {
"type": "float32"
}, "msubstruct2": {
"type": "array", 
"items": {
"type": "object",
"item": "org::qeo::dynamic::qdm::test::Substruct2"
}
}, "msubstruct1": {
"type": "array", 
"items": {
"type": "object",
"item": "org::qeo::dynamic::qdm::test::Substruct1"
}
}
}
});
Qeo.registerType({
"topic": "org::qeo::dynamic::qdm::test::House",
"behavior": "event",
"properties": {
"mfloat32": {
"type": "float32"
}, "mstring": {
"type": "string"
}, "msubstruct1": {
"type": "array", 
"items": {
"type": "object",
"item": "org::qeo::dynamic::qdm::test::Substruct1"
}
}, "msubstruct3": {
"type": "array", 
"items": {
"type": "object",
"item": "org::qeo::dynamic::qdm::test::Substruct3"
}
}, "msubstruct2": {
"type": "array", 
"items": {
"type": "object",
"item": "org::qeo::dynamic::qdm::test::Substruct2"
}
}
}
});

