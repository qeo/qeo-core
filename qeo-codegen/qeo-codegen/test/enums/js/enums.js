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

