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


var manifestEnum = {
    "meta": {
        "appname": "qeo-js-tests-enum",
        "version": "1"
    },
    "application": {
    	"org::qeo::junit::EventTestEnum": "rw",
    	"org::qeo::junit::StateTestEnum": "rw"
    }
}

var factoryOptionsEnum = {
    "manifest" : manifestEnum
}

// simple test, just write and read event
test("EventTestEnum", function()
{
	Qeo.registerType({
	     "enum": "org::qeo::junit::enums::enum1",
	     "values": {
	        "ZERO": 0,
	        "ONE": 1,
	        "TWO": 2,
	     }
     });
     Qeo.registerType({
    	 "topic": "org::qeo::junit::EventTestEnum",
    	 "behavior": "event",
    	 "properties": {
    		 "intType": {
    			 "type": "int32"
		 	 },
     		"enumType": {
     			"type": "enum",
     			"item": "org::qeo::junit::enums::enum1"     			
     		}
    	 }
     });

     var sampleEnum = Qeo.getEnum("org::qeo::junit::enums::enum1");
     ok(sampleEnum != null, "sampleEnum should not be null");
     var dataOrig = {
    		 "intType": 123456789,
    		 "enumType" : sampleEnum.ONE
     };
     testEventWrite(factoryOptionsEnum, "org::qeo::junit::EventTestEnum", dataOrig, dataOrig,
    		 undefined, undefined, undefined, genericExpects + 1);
});

//same as EventTestEnum, but register the enum type after the Qeo type
test("EventTestEnumRegisterTypeOrder", function()
{
     Qeo.registerType({
    	 "topic": "org::qeo::junit::EventTestEnum",
    	 "behavior": "event",
    	 "properties": {
    		 "intType": {
    			 "type": "int32"
		 	 },
     		"enumType": {
     			"type": "enum",
     			"item": "org::qeo::junit::enums::enum1"     			
     		}
    	 }
     });
     Qeo.registerType({
	     "enum": "org::qeo::junit::enums::enum1",
	     "values": {
	        "ZERO": 0,
	        "ONE": 1,
	        "TWO": 2,
	     }
     });

     var sampleEnum = Qeo.getEnum("org::qeo::junit::enums::enum1");
     ok(sampleEnum != null, "sampleEnum should not be null");
     var dataOrig = {
    		 "intType": 123456789,
    		 "enumType" : sampleEnum.ONE
     };
     testEventWrite(factoryOptionsEnum, "org::qeo::junit::EventTestEnum", dataOrig, dataOrig,
    		 undefined, undefined, undefined, genericExpects + 1);
});

test("StateTestEnum", function()
{
	Qeo.registerType({
	     "enum": "org::qeo::junit::enums::enum1",
	     "values": {
	        "ZERO": 0,
	        "ONE": 1,
	        "TWO": 2,
	     }
	});
    Qeo.registerType({
    	 "topic": "org::qeo::junit::StateTestEnum",
    	 "behavior": "state",
    	 "properties": {
    		 "intType": {
    			 "type": "int32",
    			 "key": true
		 	 },
     		"enumType": {
     			"type": "enum",
     			"item": "org::qeo::junit::enums::enum1"     			
     		}
    	 }
    });

    var sampleEnum = Qeo.getEnum("org::qeo::junit::enums::enum1");
    var dataOrig = {
    		"intType": 123456789,
    		"enumType" : sampleEnum.ONE
    };
    testStateWrite(factoryOptionsEnum, "org::qeo::junit::StateTestEnum", dataOrig, dataOrig);
});

//enum is key now
test("StateTestEnumKey", function()
{
	Qeo.registerType({
	     "enum": "org::qeo::junit::enums::enum1",
	     "values": {
	        "ZERO": 0,
	        "ONE": 1,
	        "TWO": 2,
	     }
	});
    Qeo.registerType({
    	 "topic": "org::qeo::junit::StateTestEnum",
    	 "behavior": "state",
    	 "properties": {
    		 "intType": {
    			 "type": "int32" 
		 	 },
     		"enumType": {
     			"type": "enum",
     			"item": "org::qeo::junit::enums::enum1",
     			"key": true
     		}
    	 }
    });

    var sampleEnum = Qeo.getEnum("org::qeo::junit::enums::enum1");
    var dataOrig = {
    		"intType": 123456789,
    		"enumType" : sampleEnum.ONE
    };
    testStateWrite(factoryOptionsEnum, "org::qeo::junit::StateTestEnum", dataOrig, dataOrig);
});