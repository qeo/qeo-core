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

/**
/ Helper Function for creating a factory and then writing an event. 
/ After writing the event it will verify if the received data is
/ equal to the one that is expected.
**/
var genericExpects = 8;

function testEventWrite(_factoryOptions, type, data, expectedData,
		onEventWriterCreated, onEventReaderCreated, onFactoryCreated, expects)
{
	if (typeof expects !== "undefined") {
    	expect(expects); //number of assertions in this test
	}
	else {
		expect(genericExpects);
	}
    QUnit.stop(); //start async test

    var reader;
    var writer;
    var factory;

    Qeo.createFactory(_factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            if (typeof onFactoryCreated !== "undefined") onFactoryCreated(factory);
            var promisedWriter = factory.createEventWriter(type);
            var promisedReader = factory.createEventReader(type);

            return promisedWriter.then(function(_writer) {
                ok(true, "writer created");
                writer = _writer;
                if (typeof onEventWriterCreated !== "undefined") onEventWriterCreated(writer);
                return promisedReader.then(function(_reader) {
                    ok(true, "reader created");
                    reader = _reader;
                    if (typeof onEventReaderCreated !== "undefined") onEventReaderCreated(reader);
                });
            });
        }
     ).then(
         function() {
             ok(true, "readers/writers created");
             var dataArrived = false;
             reader.on("data",function (data) {
            	 dataArrived = true;
                 if (expectedData) {
                   ok(true, "Got some data");
                   deepEqual(data, expectedData);
                 } else {
                   ok(false, "Received data, expected nothing: " + JSON.stringify(data));
                 }
             });
             reader.on("noMoreData",function (data) {
            	 equal(dataArrived, true, "Got some data before onMoreData");
               if (expectedData) { 
                 ok(true, "Got noMoreData callback");
                 //closing
                 reader.close();
                 writer.close();
                 factory.close();
                 QUnit.start();
               } else {
                 ok(false, "Got noMoreData callback... Didn't expect this");
               }
             });
             if (!expectedData) {
             setTimeout(function() {
            	 equal(dataArrived, false, "Did not get onData");
               ok(true, "Received nothing... Expected nothing!");
                   writer.close();
               reader.close();
               factory.close();
                       ok(true, "Factory + readers/writers are closed");
                       ok(true);
               QUnit.start();
              },1000);
             }
             writer.write(data);
         },
         function(error) {
             ok(false, "Caught error: " + error);
             QUnit.start();
         }
     );    
}

function testStateWrite(_factoryOptions, type, data, expectedData, onStateWriterCreated, onStateChangeReaderCreated, onFactoryCreated) {
    expect(genericExpects); //number of assertions in this test
    QUnit.stop(); //start async test

    var reader;
    var writer;
    var factory;

    Qeo.createFactory(_factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            if (typeof onFactoryCreated !== "undefined") onFactoryCreated(factory);
            var promisedWriter = factory.createStateWriter(type);
            var promisedReader = factory.createStateChangeReader(type);

            return promisedWriter.then(function(_writer) {
                ok(true, "writer created");
                writer = _writer;
                if (typeof onStateWriterCreated !== "undefined") onStateWriterCreated(writer);
                return promisedReader.then(function(_reader) {
                    ok(true, "reader created");
                    reader = _reader;
                    if (typeof onStateReaderCreated !== "undefined") onStateReaderCreated(reader);
                });
            });
        }
     ).then(
         function() {
             ok(true, "readers/writers created");
             var dataArrived = false;
             reader.on("data",function (data) {
            	 dataArrived = true;
                 if (expectedData) {
                   ok(true, "Got some data");
                   deepEqual(data, expectedData);
                 } else {
                   ok(false, "Received data, expected nothing: " + JSON.stringify(data));
                 }
             });
             reader.on("noMoreData",function (data) {
            	 equal(dataArrived, true, "Got some data before onMoreData");
               if (expectedData) { 
                 ok(true, "Got noMoreData callback");
                 //closing
                 reader.close();
                 writer.close();
                 factory.close();
                 QUnit.start();
               } else {
                 ok(false, "Got noMoreData callback... Didn't expect this");
               }
             });
             if (!expectedData) {
             setTimeout(function() {
            	 equal(dataArrived, false, "Did not get onData");
               ok(true, "Received nothing... Expected nothing!");
                   writer.close();
               reader.close();
               factory.close();
                       ok(true, "Factory + readers/writers are closed");
                       ok(true);
               QUnit.start();
              },1000);
             }
             writer.write(data);
         }
     );    
}