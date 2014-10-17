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
"ENUM1": 0,
"ENUM2": 1
}                                                                                                                                                                                                      
});
Qeo.registerType({
"enum": "org::qeo::testo::EnumName",
"values" : {
"ENUM3": 0,
"ENUM4": 1
}                                                                                                                                                                                                      
});
Qeo.registerType({
"enum": "org::qeo::testo::EnumNameBis",
"values" : {
"ENUM1": 0,
"ENUM2": 1
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
"MyInt32": {
"type": "int32"
}, "MyInt64": {
"type": "int64"
}, "MyEnumBis": {
"type": "enum", 
"item": "org::qeo::test::EnumNameBis"
}
}
});
Qeo.registerType({
"topic": "org::qeo::testo::MyStructWithEnums",
"properties": {
"MyFloat32": {
"type": "float32"
}, "MyString": {
"type": "string"
}, "MyEnum": {
"type": "enum", 
"item": "org::qeo::testo::EnumName"
}
}
});
Qeo.registerType({
"topic": "org::qeo::testo::MyStructWithEnumsBis",
"properties": {
"MyBooleanBis": {
"type": "boolean"
}, "MyByteBis": {
"type": "byte"
}, "MyInt16Bis": {
"type": "int16"
}, "MyEnumBis": {
"type": "enum", 
"item": "org::qeo::testo::EnumNameBis"
}
}
});

