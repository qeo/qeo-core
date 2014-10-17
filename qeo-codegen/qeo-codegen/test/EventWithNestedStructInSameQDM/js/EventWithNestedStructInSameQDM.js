/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/
if (typeof Qeo === "undefined") {Qeo = {registerType: function(td) {this.ttr.push(td);},ttr: []};}

Qeo.registerType({
"topic": "org::qeo::test::MyInnerStructWithPrimitives",
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
"topic": "org::qeo::test::EventWithNestedStruct",
"behavior": "event",
"properties": {
"MyStructWithPrimitives": {
"type": "object", 
"item": "org::qeo::test::MyInnerStructWithPrimitives"
}, "MyBoolean": {
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

