/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/
if (typeof Qeo === "undefined") {Qeo = {registerType: function(td) {this.ttr.push(td);},ttr: []};}

Qeo.registerType({
"topic": "org::qeo::test::StateWithUnbArrayOfStructKeyIsInt16",
"behavior": "state",
"properties": {
"MyBoolean": {
"type": "boolean"
}, "MyByte": {
"type": "byte"
}, "MyInt16": {
"type": "int16", "key": true
}, "MyInt32": {
"type": "int32"
}, "MyInt64": {
"type": "int64"
}, "MyFloat32": {
"type": "float32"
}, "MyString": {
"type": "string"
}, "MyUnbArrayOfStructWithPrimitives": {
"type": "array", 
"items": {
"type": "object",
"item": "org::qeo::test::MyStructWithPrimitives"
}
}
}
});

