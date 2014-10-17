/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/
if (typeof Qeo === "undefined") {Qeo = {registerType: function(td) {this.ttr.push(td);},ttr: []};}

Qeo.registerType({
"topic": "org::qeo::test::EventWithUnbArrayOfStruct",
"behavior": "event",
"properties": {
"MyUnbArrayOfStructWithPrimitives": {
"type": "array", 
"items": {
"type": "object",
"item": "org::qeo::test::MyStructWithPrimitives"
}
}
}
});

