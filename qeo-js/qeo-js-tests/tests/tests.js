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

var manifest = {
    "meta": {
        "appname": "qeo-js-tests",
        "version": "1"
    },
    "application": {
        "org::qeo::junit::Event": "rw",
        "org::qeo::junit::State": "rw",
        "org::qeo::system::DeviceInfo": "r",
        "org::qeo::system::RegistrationRequest": "rw",
        "org::qeo::system::RegistrationCredentials": "rw",
        "org::qeo::junit::SubstructTwice": "rw",
        "org::qeo::junit::Substruct1": "rw",
        "org::qeo::junit::Substruct2": "rw",
        "org::qeo::junit::Incomplete": "rw",
        "org::qeo::junit::StateBadType": "rw",
        "org::qeo::junit::EventBadType": "rw",
        "org::qeo::junit::eventTestAllTypes::TestAllTypes": "rw",
        "org::qeo::junit::eventTestAllTypes::A": "rw",
        "org::qeo::junit::eventTestAllTypes::B": "rw",
        "org::qeo::junit::eventTestAllTypes::TestTypeFloat": "rw",
        "org::qeo::junit::eventTestSequences::BasicSequence": "rw",
        "org::qeo::junit::eventTestSequences::StructSequence": "rw"
    }
}

var factoryOptions = {
    "manifest" : manifest
}


var statetype = {
    "topic" : "org::qeo::junit::State",
    "behavior" : "state",
    "properties" : {
        "id" : {
            "type" : "int32",
            "key" : true
        },
        "value" : {
            "type" : "string"
        }
    }
};
Qeo.registerType(statetype);

Qeo.registerType({
    "topic": "org::qeo::junit::Event",
    "behavior": "event",
    "properties": {
        "data": {
            "type": "string"
        },
        "nr": {
            "type": "int32"
        }
    }
});

Qeo.registerType({
    "topic": "org::qeo::junit::Incomplete",
    "behavior": "event",
    "properties": {
        "data": {
            "type": "object",
            "item": "org::qeo::junit::DoesNotExist"
        }
    }
});


test("TC21020_FactoryOpenDomainiReaderWriter", function() {
    expect(5); //number of assertions in this test
    QUnit.stop(); //start async test
    
    var reader;
    var writer;
    var factory;
    var openFactoryOptions = {
        "manifest" : manifest,
        "identity" : "open"
    }

    Qeo.createFactory(openFactoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            var promisedWriter = factory.createStateWriter("org::qeo::junit::State");
    
            promisedWriter.then(
                function(_writer) {
                    writer = _writer;
                    
                    ok(true, "StateWriter created");
                    var promisedReader = factory.createStateReader("org::qeo::junit::State");
                    promisedReader.then(
                    	function(_reader) {
                    	ok(true, "StateReader created");
                        var promisedRegWriter = factory.createStateWriter("org::qeo::system::RegistrationRequest");
                        promisedRegWriter.then(
                            function(_writer) {
                                writer = _writer;
                                ok(true, "StateWriter is created");
                                writer.close();
                                var promisedRegReader = factory.createStateReader("org::qeo::system::RegistrationCredentials");
                                promisedRegReader.then(
                                    function(_reader) {
                                        reader=_reader;
                                        ok(true, "StateReader is created");
                                        reader.close();
                                        factory.close();
                                        QUnit.start();
                                    },function(error) {
                                        ok(false, "StateReader RegistrationCredentials creation failed");
                                        factory.close();
                                        QUnit.start();
                                    }

                                );
                            },function(error) {
                                ok(false, "StateWriter RegistrationRequest creation failed");
                                factory.close();
                                QUnit.start();
                            }

                        );
                        reader.close();
                    },function(error) {
                    	ok(false, "StateReader cannot be created");
                        factory.close();
                        QUnit.start();
                    });
                    writer.close();
                },function(error) {
                	ok(false, "StateWriter not created");
                    factory.close();
                    QUnit.start();
                }
            );
    });
});


test("TC20970_EventTestLongString", function() {
    expect(7); //number of assertions in this test
    QUnit.stop(); //start async test
    
    var reader;
    var writer;
    var factory;
    var readerCount = 0;
    var writerCount = 0;
    var i = 0;
    var dataOrig = { 'data' : '1234567890', 'nr': 1};
    
    function onData123(data) {
            ok(true, "data received");
            deepEqual(data, dataOrig);
            //closing
            reader.close();
            writer.close();
            factory.close();
            QUnit.start(); //continue with next test
        }

    Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            var promisedWriter = factory.createEventWriter("org::qeo::junit::Event");
            var promisedReader = factory.createEventReader("org::qeo::junit::Event", {"on": {"data": onData123}});

            return promisedWriter.then(function(_writer) {
                ok(true, "writer created");
                writer = _writer;
                return promisedReader.then(function(_reader) {
                    ok(true, "reader created");
                    reader = _reader;
                });
            });
        }
     ).then(
         function() {
             ok(true, "readers/writers created");
            var iterations = 6;
            for (var i = 0; i < iterations; i++) {
                dataOrig.data += dataOrig.data.concat(dataOrig.data);
            }
             ok(true, "long string created:" + dataOrig.data);
             writer.write(dataOrig);
         }
     );
});

test("TC20978_StateTestMissingKeyParameter", function() {
    expect(5); //number of assertions in this test

    QUnit.stop();

    var reader;
    var writer;
    var factory;

    Qeo.createFactory(factoryOptions).then(function(_factory) {
        ok(true, "factory created");
        factory = _factory;
        var promisedWriter = factory.createStateWriter("org::qeo::junit::State");
        var promisedReader = factory.createStateReader("org::qeo::junit::State");

        return promisedWriter.then(function(_writer) {
            ok(true, "writer created");
            writer = _writer;
            return promisedReader.then(function(_reader) {
                ok(true, "reader created");
                reader = _reader;
            });
        });
    }).then(function() {
        ok(true, "readers/writers created");
        var dataOrig = {
            'value' : 'value123'
        };
        reader.on("update", function() {
            ok(false, "We should not receive something");
        });

        //write data with missing fields, should not arrive
        //however write function cannot return an error
        writer.write(dataOrig);
        
        setTimeout(function() {
            //sleep a little to check if the data did not arrive
            ok(true, "Received nothing... Expected nothing!");
            writer.close();
            reader.close();
            factory.close();
            QUnit.start();
        },100);     
    },
    function(error) {
        ok(false, "should not come here");
        QUnit.start();
    }
    );

});

/*
// to be enabled when we support multiple closed domain factories in qeo-c-core
test("factoryNotClosedTest", function() {
    expect(2);
    QUnit.stop();
    Qeo.createFactory(factoryOptions2).then(
    function(_factory) {
       ok(true, "First factory created");
       Qeo.createFactory(factoryOptions2).then(
               function(_factory2) {
             ok(true, "Second factory created!");
         _factory2.close();
                 _factory.close();
         QUnit.start();
               }
               , function(error2) {
                 ok(false, "Cannot create second factory");
                 _factory.close();
         QUnit.start();
               }
            ); 
    }
     ,  function(error) {
           ok(false, "First factory failed to create");
       QUnit.start();
        }
    );
     
});
*/
/*
test("StressFactoryCreation", function() {

    var iterations = 3;
    var factories = 0;
    expect(iterations);
    QUnit.stop(); //start async test
    var reader;
    var writer;
    var factory;
    for (var i=0;i<iterations;i++) {
        Qeo.createFactory(factoryOptions).then(
        function(_factory){
          factory = _factory;
          ok(true, "factory created");
          factories++;
          factory.close();
          if (factories == iterations) {
         QUnit.start();
          }
        }
      );
    }
});
*/

var expectWriteErrorHandler =  function(writer) {
  QUnit.expect(genericExpects + 1);
  writer.on("error", function(error) {
    QUnit.ok(true, error.stack || error);
  });
};

test("TC20976_StateTestMissingParameter", function() {
     var dataOrig = { "id": 976};
     testStateWrite(factoryOptions, "org::qeo::junit::State", dataOrig, null, expectWriteErrorHandler);

});

test("TC20977_EventTestMissingParameter", function() {
     var dataOrig = { "nr": 976};
     testEventWrite(factoryOptions, "org::qeo::junit::Event", dataOrig, null, expectWriteErrorHandler);

});

test("TC20957_EventTest", function() {
     var dataOrig = { 'data' : 'TC20957_EventTest', "nr": 456};
     testEventWrite(factoryOptions, "org::qeo::junit::Event", dataOrig, dataOrig);

});

test("TC20958_EventTestWrongType", function() {
    var dataSend = { 'data' : { 'innerData' : 1234 },"nr": 'wrongString'};
    testEventWrite(factoryOptions, "org::qeo::junit::Event", dataSend, null, expectWriteErrorHandler);

});

test("TC20959_EventTestWriterClosed", function() {
    var dataOrig = { 'data' : 'TC20959_EventTestWriterClosed', "nr": 456};
    testEventWrite(factoryOptions, "org::qeo::junit::Event", dataOrig, null, function(writer) { writer.close(); });
});

test("TC20960_EventTestReaderClosed", function() {
    var dataOrig = { 'data' : 'TC20960_EventTestReaderClosed', "nr": 456};
    testEventWrite(factoryOptions, "org::qeo::junit::Event", dataOrig, null, undefined, function(reader) { reader.close(); });
});

test("TC20972_EventTestStress", function() {
	var loops = 30;
        var iterations = 0;
	expect(8*loops); //number of assertions in this test
	QUnit.stop(); //start async test

	function stress() {
	    var reader;
	    var writer;
	    var factory;
	    var data = { 'data' : 'TC20957_EventTest', "nr": 456};
	    var expectedData = { 'data' : 'TC20957_EventTest', "nr": 456};

	    Qeo.createFactory(factoryOptions).then(
		function(_factory){
		    ok(true, "factory created");
		    factory = _factory;
		    var promisedWriter = factory.createEventWriter("org::qeo::junit::Event");
		    var promisedReader = factory.createEventReader("org::qeo::junit::Event");

		    return promisedWriter.then(function(_writer) {
		        ok(true, "writer created");
		        writer = _writer;
		        return promisedReader.then(function(_reader) {
		            ok(true, "reader created");
		            reader = _reader;
		        });
		    });
		}
	     ).then(
		 function() {
		     ok(true, "readers/writers created");
		     reader.on("data",function (data) {
	 	        ok(true, "Got some data");
		        deepEqual(data, expectedData);
		     });
		     reader.on("noMoreData",function (data) {
		         ok(true, "Got noMoreData callback");
		         //closing
		         reader.close();
		         writer.close();
		         factory.close();
		    	 ok(true, "iteration " + iterations + " succesful!");
			 if (iterations++ < loops-1) {
				stress();
			 } else {
		         	QUnit.start();
			 }
		     });
		     writer.write(data);
		 }
	     ); 

	};
	stress();

});

test("TC20975_EventTestBadType", function() {
    expect(3); //number of assertions in this test

    QUnit.stop();

    var reader;
    var writer;
    var factory;

    Qeo.registerType({
        "topic": "org::qeo::junit::EventBadType",
        "behavior": {},
        "properties": {
           "data": {
              "type": { "garble" : 44 }
           }
        }
    });

    Qeo.createFactory(factoryOptions).then(function(_factory) {
        ok(true, "factory created");
        factory = _factory;
        var promisedWriter = factory.createEventWriter("org::qeo::junit::EventBadType");
        var promisedReader = factory.createEventReader("org::qeo::junit::EventBadType");

        promisedWriter.then(function(_writer) {
            ok(false, "Writer was created. Should not happen");
        }, function(error) {
           ok(true, "Could not create writer for this type");
	   promisedReader.then(function(_reader) {
              ok(false, "Reader was created. Should not happen");
              factory.close();
	      QUnit.start();
           }, function(error) {
              ok(true, "Could not create reader for this type");
              factory.close();
	      QUnit.start();
           });

        });
    });

});

test("TC20974_StateTestBadType", function() {
    expect(3); //number of assertions in this test

    QUnit.stop();

    var reader;
    var writer;
    var factory;

    Qeo.registerType({
        "topic": "org::qeo::junit::StateBadType",
        "behavior": {},
        "properties": {
           "data": {
              "type": { "garble" : 44 }
           }
        }
    });

    Qeo.createFactory(factoryOptions).then(function(_factory) {
        ok(true, "factory created");
        factory = _factory;
        var promisedWriter = factory.createStateWriter("org::qeo::junit::StateBadType");
        var promisedReader = factory.createStateReader("org::qeo::junit::StateBadType");

        promisedWriter.then(function(_writer) {
            ok(false, "Writer was created. Should not happen");
        }, function(error) {
           ok(true, "Could not create writer for this type");
	   promisedReader.then(function(_reader) {
              ok(false, "Reader was created. Should not happen");
              factory.close();
	      QUnit.start();
           }, function(error) {
              ok(true, "Could not create reader for this type");
              factory.close();
	      QUnit.start();
           });

        });
    });

});

test("TC20961_EventTestInvalidType", function() {
    expect(3); //number of assertions in this test
    QUnit.stop(); //start async test
    
    var reader;
    var writer;
    var factory;

    Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            Qeo.registerType({
                        "topic": "org::qeo::junit::notinmanifest",
                        "behavior": "event",
                        "properties": {
                             "data": {
                                 "type": "megacooltype"
                             }
                        }
                    });
            var promisedWriter = factory.createEventWriter("org::qeo::junit::notinmanifest");
    
            promisedWriter.then(
                function(_writer) {
                    writer = _writer;
                    ok(false, "EventWriter is created and shouldn't be");
                    writer.close();
                    factory.close();
                    QUnit.start();
                },function(error) {
                    ok(true, "EventWriter not created (" + error + ")");
                    var promisedReader = factory.createEventReader("org::qeo::junit::notinmanifest");
                    promisedReader.then(
                    function(_reader) {
                        ok(false, "EventWriter is created and shouldn't be");
                        reader.close();
                        factory.close();
                        QUnit.start();
                    },function(error) {
                        ok(true, "EventReader not created (" + error + ")");
                        factory.close();
                        QUnit.start();
                    }

                    );
                }
            );
        }
    )    
});

test("TC20962_EventTestNotInManifest", function() {
    if (TestReporter.hasManifestSupport()) {
        expect(3); //number of assertions in this test
        QUnit.stop(); //start async test
        
        var reader;
        var writer;
        var factory;
        
        Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            Qeo.registerType({
                        "topic": "org::qeo::junit::notinmanifest",
                        "behavior": "event",
                        "properties": {
                             "data": {
                                 "type": "string"
                             }
                        }
                    });
            var promisedWriter = factory.createEventWriter("org::qeo::junit::notinmanifest");

            promisedWriter.then(
                function(_writer) {
                    ok(false, "EventWriter shouldn't have been created!");
                    writer = _writer;
                    writer.close();
                    factory.close();
                    QUnit.start();
                    
                },function(error) {
                    ok(true, "EventWriter not created");
                    var promisedReader = factory.createEventReader("org::qeo::junit::notinmanifest");
                    promisedReader.then(
                        function(_reader){
                            ok(false, "EventReader shouldn't have been created!");
                            reader.close();
                            factory.close();
                            QUnit.start();

                        }, function(error){
                            ok(true, "EventReader not created");
                            factory.close();
                            QUnit.start();

                        }

                    );
                });
        }

        );
     } else {
             ok(true, "No Manifest Support"); 
     }
});

test("TC20963_StateChangeTest", function() {
    expect(7); //number of assertions in this test

    QUnit.stop();

    var reader;
    var writer;
    var factory;

    Qeo.createFactory(factoryOptions).then(function(_factory) {
        ok(true, "factory created");
        factory = _factory;
        var promisedWriter = factory.createStateWriter("org::qeo::junit::State");
        var promisedReader = factory.createStateChangeReader("org::qeo::junit::State");
    

        return promisedWriter.then(function(_writer) {
            ok(true, "writer created");
            writer = _writer;
            return promisedReader.then(function(_reader) {
                ok(true, "reader created");
                reader = _reader;
            });
        });
    }).then(function() {
        ok(true, "readers/writers created");
        var dataOrig = {
            'id' : 123,
            'value' : 'value123'
        };
        reader.on("data", function(data) {
            ok(true, "Got some data");
            deepEqual(data, dataOrig);
        });
        reader.on("noMoreData",function (data) {
            ok(true, "Got noMoreData callback");
            
            //closing
            reader.close();
            writer.close();
            factory.close();
            
            QUnit.start(); //continue with next test
        });

        writer.write(dataOrig);
    });
});

test("TC20964_StateTestIterator", function() {
    expect(8); //number of assertions in this test

    QUnit.stop();

    var reader;
    var writer;
    var factory;

    Qeo.createFactory(factoryOptions).then(function(_factory) {
        ok(true, "factory created");
        factory = _factory;
        var promisedWriter = factory.createStateWriter("org::qeo::junit::State");
        var promisedReader = factory.createStateReader("org::qeo::junit::State");

        return promisedWriter.then(function(_writer) {
            ok(true, "writer created");
            writer = _writer;
            return promisedReader.then(function(_reader) {
                ok(true, "reader created");
                reader = _reader;
            });
        });
    }).then(function() {
        ok(true, "readers/writers created");
        var dataOrig1 = {
            'id' : 123,
            'value' : 'value123'
        };
        var dataOrig2 = {
            'id' : 456,
            'value' : 'value456'
        };
        var count = 0;
        
        reader.on("update", function() {
            count++;
            if (count == 1) {
            ok(true, "Got some data");
            setTimeout(function() {
                var iterateCount = 0;
                reader.iterate(function(data) {
                    iterateCount++;
                    if (data.id == 123) {
                        deepEqual(data, dataOrig1);
                    }
                    else {
                        deepEqual(data, dataOrig2);
                    }
                }).then(function() {
                    equal(iterateCount, 2);
                    
                    //closing
                    reader.close();
                    writer.close();
                    factory.close();
                    
                    QUnit.start(); //continue with next test
                  
                },
                function(error) {
                    console.log("Iterator error: " + error);
                    ok(false, "Iterator error: " + error);
                    QUnit.start();
                }
                );


            },1000);
            }
        });

        writer.write(dataOrig1);
        writer.write(dataOrig2);
    },
    function(error) {
        ok(false, error);
        QUnit.start();
    }
    );
});


test("TC20965_EventTestPolicy", function() {
    expect(11); //number of assertions in this test
    QUnit.stop(); //start async test
    
    var reader;
    var writer;
    var factory;
    var readerCount = 0;
    var writerCount = 0;
    var i = 0;
    var dataOrig = { 'data' : 'EventTestPolicy', 'nr': 1};
    
    function sendPolicyUpdates() {
        reader.requestPolicy();
        writer.requestPolicy();
        
        writer.updatePolicy({"users":[]});
        writer.requestPolicy();
    };
    function validateState() {
        if (i == 1) {
            //readers writers ready
            if (readerCount == 1 && writerCount == 1) {
                //both reader and writer got initial onpolicyupdate
                i = 2; //goto next state
                sendPolicyUpdates();
            }
        }
        else if (i == 2){
            //receiving other policyupdates
            writer.write(dataOrig);
        }
    }
    function onData123(data) {
        console.log("counters: " + readerCount + " -- " + writerCount);
        if (readerCount == 2 && writerCount == 4) {
            ok(true, "got correct number of policy callbacks");
            //closing
            reader.close();
            writer.close();
            factory.close();
            QUnit.start(); //continue with next test
        }
    };
    function policyUpdateReader(data) {
        console.log("policyUpdate for reader " + JSON.stringify(data));
        ok(true, "Got reader policy update");
        readerCount++;
        validateState();
    };
    function policyUpdateWriter(data) {
        console.log("policyUpdate for writer " + JSON.stringify(data));
        ok(true, "Got writer policy update");
        writerCount++;
        validateState();
    };
    
    Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            var promisedWriter = factory.createEventWriter("org::qeo::junit::Event", {"enablePolicy": true, "on": {"policyUpdate": policyUpdateWriter}});
            var promisedReader = factory.createEventReader("org::qeo::junit::Event", {"enablePolicy": true, "on": {"policyUpdate": policyUpdateReader, "data": onData123}});

            return promisedWriter.then(function(_writer) {
                ok(true, "writer created");
                writer = _writer;
                return promisedReader.then(function(_reader) {
                    ok(true, "reader created");
                    reader = _reader;
                });
            });
        }
     ).then(
         function() {
             ok(true, "readers/writers created");
             i = 1; //readers/writers ready
             validateState();
         }
     );
});

test("TC20967_StateTestPolicy", function() {
    expect(8); //number of assertions in this test
    QUnit.stop(); //start async test

    var reader;
    var writer;
    var factory;
    var readerCount = 0;
    var writerCount = 0;
    var i = 0;
    var receivedWriterPolicy;
    var receivedReaderPolicy;

    var updatedPolicy = {"users":[]};

    function sendWriterPolicyUpdate() {
	console.log("sendWriterPolicyUpdate " + JSON.stringify(updatedPolicy));
        writer.updatePolicy(updatedPolicy);
    };

    function validateState() {
        if (i == 1) {
            //readers writers ready
            if (readerCount == 1 && writerCount == 1) {
                //both reader and writer got initial onpolicyupdate
                i = 2; //goto next state
                sendWriterPolicyUpdate();
            }
        }
        else if (i == 2){
            //receiving other policyupdates
      	    ok(true, "Got the update after updatePolicy");
	    console.log(JSON.stringify(receivedWriterPolicy));
            factory.close();
	    QUnit.start();
        }
    }

    function policyUpdateReader(data) {
        console.log("policyUpdate for reader " + JSON.stringify(data));
        ok(true, "Got reader policy update");
        readerCount++;
	receivedReaderPolicy = data;
        validateState();
    };
    function policyUpdateWriter(data) {
        console.log("policyUpdate for writer " + JSON.stringify(data));
        ok(true, "Got writer policy update");
        writerCount++;
	receivedWriterPolicy = data;
        validateState();
    };

    Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            var promisedWriter = factory.createStateWriter("org::qeo::junit::State", {"enablePolicy": true, "on": {"policyUpdate": policyUpdateWriter}});
            var promisedReader = factory.createStateReader("org::qeo::junit::State", {"enablePolicy": true, "on": {"policyUpdate": policyUpdateReader}});

            return promisedWriter.then(function(_writer) {
                ok(true, "writer created");
                writer = _writer;
                return promisedReader.then(function(_reader) {
                    ok(true, "reader created");
                    reader = _reader;
                });
            });
        }
     ).then(
         function() {
             ok(true, "readers/writers created");
             i = 1; //readers/writers ready
             validateState();
         }
     );    
});

test("TC20968_StateChangeTestPolicy", function() {
    expect(11); //number of assertions in this test
    QUnit.stop(); //start async test

    var reader;
    var writer;
    var factory;
    var readerCount = 0;
    var writerCount = 0;
    var i = 0;
    var receivedWriterPolicy;
    var receivedReaderPolicy;
    var dataOrig = { 'id' : 1, 'value' : 'StateChangeTest'};

    function sendPolicyUpdates() {
        reader.requestPolicy();
        writer.requestPolicy();
        
        writer.updatePolicy({"users":[]});
        writer.requestPolicy();
    };

    function validateState() {
        if (i == 1) {
            //readers writers ready
            if (readerCount == 1 && writerCount == 1) {
                //both reader and writer got initial onpolicyupdate
                i = 2; //goto next state
                sendPolicyUpdates();
            }
        }
        else if (i == 2){
            //receiving other policyupdates
	    console.log(JSON.stringify(dataOrig) + "written");
	    writer.write(dataOrig);
        }
    }
    function onData123(data) {
        console.log("counters: " + readerCount + " -- " + writerCount);
        if (readerCount == 2 && writerCount == 4) {
            ok(true, "got correct number of policy callbacks");
            //closing
            reader.close();
            writer.close();
            factory.close();
            QUnit.start(); //continue with next test
        }
    };
    function policyUpdateReader(data) {
        console.log("policyUpdate for reader " + JSON.stringify(data));
        ok(true, "Got reader policy update");
        readerCount++;
	receivedReaderPolicy = data;
        validateState();
    };
    function policyUpdateWriter(data) {
        console.log("policyUpdate for writer " + JSON.stringify(data));
        ok(true, "Got writer policy update");
        writerCount++;
	receivedWriterPolicy = data;
        validateState();
    };

    Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            var promisedWriter = factory.createStateWriter("org::qeo::junit::State", {"enablePolicy": true, "on": {"policyUpdate": policyUpdateWriter}});
            var promisedReader = factory.createStateChangeReader("org::qeo::junit::State", {"enablePolicy": true, "on": {"policyUpdate": policyUpdateReader, "data": onData123}});

            return promisedWriter.then(function(_writer) {
                ok(true, "writer created");
                writer = _writer;
                return promisedReader.then(function(_reader) {
                    ok(true, "reader created");
                    reader = _reader;
                });
            });
        }
     ).then(
         function() {
             ok(true, "readers/writers created");
             i = 1; //readers/writers ready
             validateState();
         }
     );    
});
/*
test("TC20967_StateTestPolicy", function() {
    expect(8); //number of assertions in this test
    QUnit.stop(); //start async test

    var reader;
    var writer;
    var factory;
    var readerCount = 0;
    var writerCount = 0;
    var i = 0;
    var receivedWriterPolicy;
    var receivedReaderPolicy;

    var updatedPolicy = {"users":[]};

    function sendWriterPolicyUpdate() {
	console.log("sendWriterPolicyUpdate " + JSON.stringify(updatedPolicy));
        writer.updatePolicy(updatedPolicy);
    };

    function validateState() {
        if (i == 1) {
            //readers writers ready
            if (readerCount == 1 && writerCount == 1) {
                //both reader and writer got initial onpolicyupdate
                i = 2; //goto next state
                sendWriterPolicyUpdate();
            }
        }
        else if (i == 2){
            //receiving other policyupdates
      	    ok(true, "Got the update after updatePolicy");
	          console.log(JSON.stringify(receivedWriterPolicy));
            closedFactory = _factory;
            openFactory.close();
            closedFactory.close();
	          QUnit.start();
        }
    }

    function policyUpdateReader(data) {
        console.log("policyUpdate for reader " + JSON.stringify(data));
        ok(true, "Got reader policy update");
        readerCount++;
	receivedReaderPolicy = data;
        validateState();
    };
    function policyUpdateWriter(data) {
        console.log("policyUpdate for writer " + JSON.stringify(data));
        ok(true, "Got writer policy update");
        writerCount++;
	receivedWriterPolicy = data;
        validateState();
    };

    Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            var promisedWriter = factory.createStateWriter("org::qeo::junit::State", {"enablePolicy": true, "on": {"policyUpdate": policyUpdateWriter}});
            var promisedReader = factory.createStateReader("org::qeo::junit::State", {"enablePolicy": true, "on": {"policyUpdate": policyUpdateReader}});

            return promisedWriter.then(function(_writer) {
                ok(true, "writer created");
                writer = _writer;
                return promisedReader.then(function(_reader) {
                    ok(true, "reader created");
                    reader = _reader;
                });
            });
        }
     ).then(
         function() {
             ok(true, "readers/writers created");
             i = 1; //readers/writers ready
             validateState();
         }
     );    
});
*/  
test("TC20966_FactoryOpenAndClosedDomainTest", function() {
    expect(2); //number of assertions in this test
    QUnit.stop(); //start async test
    
    var reader;
    var writer;
    var openFactory;
    var closedFactory;
    var openFactoryOptions = {
        "manifest" : manifest,
        "identity" : "open"
    }
    var closedFactoryOptions = {
        "manifest" : manifest,
    }

    Qeo.createFactory(openFactoryOptions).then(
        function(_factory){
            ok(true, "openFactory created");
            openFactory = _factory;
            Qeo.createFactory(closedFactoryOptions).then(
                function(_factory) {
                ok(true, "closedFactory created");
                closedFactory = _factory;
                openFactory.close();
                closedFactory.close();
                QUnit.start();
                }, function(error) {
                 ok(false, "Cannot create closedFactory");
                 closedFactory.close();
                });
            }, function(error) {
                 ok(false, "Cannot create openFactory");
                 openFactory.close();
            });
});


//test that writes a state and creates a reader after that. Reader should receive this state.
test("TC21035_StateWriteBeforeReaderCreation", function() {
    expect(6); //number of assertions in this test

    QUnit.stop();

    var reader;
    var writer;
    var factory;
    var lastSample;
    var sample1 = {"id": 8888, value: "writeBeforeReaderCreation"};
    var sample2 = {"id": 8889, value: "writeAfterReaderCreation"};
    
    Qeo.createFactory(factoryOptions).then(function(_factory) {
        ok(true, "factory created");
        factory = _factory;
        var promisedWriter = factory.createStateWriter("org::qeo::junit::State");
        return promisedWriter.then(function(_writer) {
            ok(true, "writer created");
            writer = _writer;
            
        });
    }).then(function() {
        //writer created, write something
        writer.write(sample1);
        
        //now create reader, the sample should arrive here!
        var promisedReader = factory.createStateChangeReader("org::qeo::junit::State", {on: {"data" : function(data){
            lastSample = data;
        }}});
        return promisedReader.then(function(_reader) {
            ok(true, "reader created");
            reader = _reader;
        });
        
    }).then(function() {
        ok(true, "reader created");
        setTimeout(function() {
            //give the sample time to arrive
            deepEqual(lastSample, sample1);
    
            //writing another sample, now this should arrive
            writer.write(sample2);
            
            setTimeout(function() {
                //give the 2nd sample some time to arrive
                deepEqual(lastSample, sample2);    
                writer.close();
                reader.close();
                factory.close();
                QUnit.start();
            },100);
        },100);
    });
});

test("TC20988_EventTestDuplicateParameter", function() {
    expect(6); //number of assertions in this test
    QUnit.stop(); //start async test
    
    var reader;
    var writer;
    var factory;
    var i = 0;
    var dataOrig = { 'data' : 'EventTestDuplicateParameter', 'nr': 1, 'nr': 2};
    
    function onData123(data) {
        ok(true, "data received");
        console.log("data" + JSON.stringify(data));
        var expectedData = { "data" : "EventTestDuplicateParameter", "nr":2};
        console.log("expectedData" + JSON.stringify(data));
        deepEqual(data, expectedData);
        //closing
        reader.close();
        writer.close();
        factory.close();
        QUnit.start(); //continue with next test
    };
    Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            var promisedWriter = factory.createEventWriter("org::qeo::junit::Event");
            var promisedReader = factory.createEventReader("org::qeo::junit::Event", {"on": {"data": onData123}});

            return promisedWriter.then(function(_writer) {
                ok(true, "writer created");
                writer = _writer;
                return promisedReader.then(function(_reader) {
                    ok(true, "reader created");
                    reader = _reader;
                });
            });
        }
     ).then(
         function() {
             ok(true, "readers/writers created");
             i = 1; //readers/writers ready
             writer.write(dataOrig);
         }
     );
});

Qeo.registerType({
    "topic": "org::qeo::junit::Substruct1",
    "behavior": "event",
    "properties": {
        "sub": {
            "type": "object",
	    "item": "org::qeo::junit::Event"
        }
    }
});

Qeo.registerType({
    "topic": "org::qeo::junit::Substruct2",
    "behavior": "event",
    "properties": {
        "sub": {
            "type": "object",
	    "item": "org::qeo::junit::Event"
        }
    }
});

test("TC21102_CreateTwoReadersWithSameSubstruct", function() {
    expect(3); //number of assertions in this test
    QUnit.stop(); //start async test
    
    var reader1;
    var reader2;
    var factory;

    Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
	    var promisedReader = factory.createEventReader("org::qeo::junit::Substruct1");
	    promisedReader.then(function(r1) {
		ok(true, "creation of reader 1 succeeded");
	    	reader1 = r1;
		var promisedReader2 = factory.createEventReader("org::qeo::junit::Substruct2");
		promisedReader2.then(function(r2) {
			ok(true, "creation of reader 2 succeeded");
			reader2 = r2;

			reader2.close();
			reader1.close();
			factory.close();
			QUnit.start();
		}, function(error) {
			ok(false, "Reader creation for SubStruct2 failed");
			reader1.close();
			factory.close();
			QUnit.start();
		});

	    }, function(error) {
	    	ok(false, "Reader creation for SubStruct1 failed");
		factory.close();
		QUnit.start();
	    });
    });
});

Qeo.registerType({
    "topic": "org::qeo::junit::SubstructTwice",
    "behavior": "event",
    "properties": {
        "sub": {
            "type": "object",
	    "item": "org::qeo::junit::Event"
        },
        "subsub": {
            "type": "object",
	    "item": "org::qeo::junit::Event"
        }
    }
});

test("TC21103_CreateOneReaderWithTwiceTheSameSubstruct", function() {
    expect(2); //number of assertions in this test
    QUnit.stop(); //start async test
    
    var reader;
    var factory;

    Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
	    var promisedReader = factory.createEventReader("org::qeo::junit::SubstructTwice");
	    promisedReader.then(function(r) {
		ok(true, "creation of reader succeeded");
	    	reader = r;
		reader.close();
		factory.close();
		QUnit.start();
	    }, function(error) {
	    	ok(false, "reader creation for SubStructTwice failed: " + error);
		factory.close();
		QUnit.start();
	    });
    });
});

test("TCDominique_IterateOverEmptyStateReader", function() {
    expect(4); //number of assertions in this test
    QUnit.stop(); //start async test
    
    var reader;
    var factory;

    // build a "copy" of statetype, but assign it a random topic name
    // this way we can ensure that there will never be any publications
    // on the topic
    var randomized_statetype = {};
    for (var key in statetype) {
    	randomized_statetype[key] = statetype[key];
    }
    randomized_statetype.topic = randomized_statetype.topic + Math.floor(Math.random() * 1000000);
    Qeo.registerType(randomized_statetype);
    // don't forget to add it to the manifest 
    manifest.application[randomized_statetype.topic] = "rw";

    Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
	    var promisedReader = factory.createStateReader(randomized_statetype.topic);
	    promisedReader.then(function(r) {
		ok(true, "creation of reader succeeded");
	    	reader = r;

		reader.iterate().then(function(values) {
		    ok(Object.prototype.toString.call(values) === "[object Array]", "values is of type array");
		    ok(values.length == 0, "values is empty");
		    reader.close();
		    factory.close();
		    QUnit.start();
		});
	    }, function(error) {
	    	ok(false, "reader creation for IterateOverEmptyStateReader failed: " + error);
		factory.close();
		QUnit.start();
	    });
    });
});

test("TCDominique_CreateReaderWithIncompleteTypeShouldReject", function() {
    expect(2); //number of assertions in this test
    QUnit.stop(); //start async test

    var reader;
    var factory;

    Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            try {
                var promisedReader = factory.createEventReader("org::qeo::junit::Incomplete");
            } catch (e) {
                ok(false, "reader creation threw an exception: " + e);
                factory.close();
                QUnit.start();
                return;
            }
            promisedReader.then(function(r) {
                ok(false, "creation of reader must not succeed");
                reader = r;
                reader.close();
                factory.close();
                QUnit.start();
            }, function(error) {
                ok(true, "reader creation for incomplete type failed as expected: " + error);
                factory.close();
                QUnit.start();
            });
        });
});

test("TCDominique_StateChangeReaderCallbacks", function() {
    expect(4); //number of assertions in this test
    QUnit.stop(); //start async test

    var writer;
    var reader;
    var factory;

    var sample = { id: 42, value: "Life, the Universe and Everything" };
    var invocationcount = { data: 0, remove: 0, nomoredata: 0 };

    function ondata(data) {
        if (data.id == sample.id && data.value == sample.value)
            invocationcount.data++;

        checkFinished();
    }
    function onremove(data) {
        if (data.id == sample.id)
            invocationcount.remove++;
        checkFinished();
    }
    function onnomoredata() {
        invocationcount.nomoredata++;
        checkFinished();
    }

    function checkFinished() {
        if (invocationcount.data == 1 && 
            invocationcount.remove == 1 &&
            invocationcount.nomoredata == 2) {
            ok(true, "got the required number of invocations");
            reader.close();
            writer.close();
            factory.close();
            QUnit.start();
        }
    }

    Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            factory.createStateChangeReader("org::qeo::junit::State").then(
                function(r) {
                    ok(true, "creation of reader succeeded");
                    reader = r;
                    reader.on("data", ondata);
                    reader.on("remove", onremove);
                    reader.on("noMoreData", onnomoredata);
                    factory.createStateWriter("org::qeo::junit::State").then(
                        function(w) {
                            ok(true, "creation of writer succeeded");
                            writer = w;
                            setTimeout(function() {writer.write(sample)}, 1000);
                            setTimeout(function() {writer.remove(sample)}, 2000);
                        }, function(error) {
                            ok(false, "writer creation failed: " + error);
                            reader.close();
                            factory.close();
                            QUnit.start();
                        });
                }, function(error) {
                    ok(false, "reader creation failed: " + error);
                    factory.close();
                    QUnit.start();
                });
        });
});

function factoryCreationTestConcurrent(identities, successExpected, allDone, level) {
    if (level === undefined)
        level = 0;

    if (identities.length == 0) {
        allDone();
        return;
    }

    var ids = identities.shift();
    var exp = successExpected.shift();

    var factories = [];
    var count = 0;

    for (var i = 0; i < ids.length; ++i) {
        (function(i) { // getting our closures right... 
            var options = { "identity" : ids[i], "manifest" : manifest };
            if (ids[i] === undefined) delete options.identity;
            Qeo.createFactory(options).then(
                function(fac) {
                    factories.push(fac);
                    success(i);
                }, function(error) {
                    failure(i);
                }
            );
        })(i);
    }

    function success(num) {
        ok(exp[num], "expected successful factory creation "+level+"."+num);
        check_done();
    }
    function failure(num) {
        ok(!exp[num], "expected failed factory creation "+level+"."+num);
        check_done();
    }

    function check_done() {
        count += 1;
        if (count === ids.length) {
            while (factories.length)
                factories.shift().close();
            factoryCreationTestConcurrent(identities, successExpected, allDone, level+1);
        }
    }
}

test("TCDominique_MultiFactoryCreation1", function() {
    expect(2);
    QUnit.stop();

    var ids = [["open", undefined]];
    var exp = [[true, true]];

    factoryCreationTestConcurrent(ids, exp, done);

    function done() {
        QUnit.start();
    }
});

test("TCDominique_MultiFactoryCreation2", function() {
    expect(2);
    QUnit.stop();

    var ids = [[undefined, "open"]];
    var exp = [[true, true]];

    factoryCreationTestConcurrent(ids, exp, done);

    function done() {
        QUnit.start();
    }
});

test("TCDominique_MultiFactoryCreation3", function() {
    expect(2);
    QUnit.stop();

    var ids = [["open", "open"]];
    var exp = [[true, true]];

    factoryCreationTestConcurrent(ids, exp, done);

    function done() {
        QUnit.start();
    }
});

test("TCDominique_MultiFactoryCreation4", function() {
    expect(2);
    QUnit.stop();

    var ids = [[undefined, undefined]];
    var exp = [[true, true]];

    factoryCreationTestConcurrent(ids, exp, done);

    function done() {
        QUnit.start();
    }
});

test("TCDominique_MultiFactoryCreation5", function() {
    expect(3);
    QUnit.stop();

    var ids = [["magic", "default", undefined]];
    var exp = [[false, false, false]];

    factoryCreationTestConcurrent(ids, exp, done);

    function done() {
        QUnit.start();
    }
});

test("TCDominique_MultiFactoryCreation6", function() {
    expect(2);
    QUnit.stop();

    var ids = [["default", undefined]];
    var exp = [[true, true]];

    factoryCreationTestConcurrent(ids, exp, done);

    function done() {
        QUnit.start();
    }
});

test("TCDominique_MultiFactoryCreation7", function() {
    expect(5);
    QUnit.stop();

    var ids = [["magic", "magic"], [undefined], ["open", undefined]];
    var exp = [[false, false], [true], [true, true]];

    factoryCreationTestConcurrent(ids, exp, done);

    function done() {
        QUnit.start();
    }
});

//Create 2 factories with different manifests, should be rejected.
test("TC_differentManifests", function() {
    expect(1); //number of assertions in this test
    QUnit.stop(); //start async test

    var mf1 = {
    	    "meta": {
    	        "appname": "qeo-js-tests-mf",
    	        "version": "1"
    	    },
    	    "application": {
    	        "org::qeo::topic1": "rw"
    	    }
    };
    var mf2 = {
    	    "meta": {
    	        "appname": "qeo-js-tests-mf",
    	        "version": "1"
    	    },
    	    "application": {
    	        "org::qeo::topic2": "rw"
    	    }
    };
    Qeo.createFactory({manifest: mf1}).then(function(factory1) {
    	//factory created
    	
    	//add a reader to the manifest
    	manifest.application["org::qeo::an::extra::Topic"] = "rw";
    	console.log("foptions: " + JSON.stringify(factoryOptions));
    	Qeo.createFactory({manifest: mf2}).then(function(factory2) {
    		ok(false, "should not be possible to create 2nd factory with other manifest");
    		QUnit.start();	
    	},
    	function(error) {
    		ok(true);
    		factory1.close();
    		QUnit.start();
    	});
    },
    function(error){
    	ok(false, "error creating factory: " + error);
    	QUnit.start();
    });
});

test("TC20973_FactoryBadIdentityTest", function() {
    expect(1); //number of assertions in this test
    QUnit.stop(); //start async test
    
    var reader;
    var writer;
    var magicFactoryOptions = {
        "manifest" : manifest,
        "identity" : "magic"
    }
    var closedFactoryOptions = {
        "manifest" : manifest,
    }

    Qeo.createFactory(magicFactoryOptions).then(
        function(_factory){
            ok(false, "factory with identity 'magic' created.");
	    _factory.close();
	    QUnit.start();
        }, function(error) {
            ok(true, "Cannot create 'magic' factory.");
            QUnit.start();
        }
    );
});

test("TC20979_StateTestIteratorClose", function() {
    expect(6); //number of assertions in this test

    QUnit.stop();

    var reader;
    var writer;
    var factory;

    var iterateCount = 0;

    Qeo.createFactory(factoryOptions).then(function(_factory) {
        ok(true, "factory created");
        factory = _factory;
        var promisedWriter = factory.createStateWriter("org::qeo::junit::State");
        var promisedReader = factory.createStateReader("org::qeo::junit::State");

        return promisedWriter.then(function(_writer) {
            ok(true, "writer created");
            writer = _writer;
            return promisedReader.then(function(_reader) {
                ok(true, "reader created");
                reader = _reader;
            });
        });
    }).then(function() {
        ok(true, "readers/writers created");
        var dataOrig1 = {
            'id' : 123,
            'value' : 'value123'
        };
        var dataOrig2 = {
            'id' : 456,
            'value' : 'value456'
        };
        var count = 0;
        
        reader.on("update", function() {
            count++;
            if (count == 1) {
            ok(true, "Got some data");
            setTimeout(function() {
                reader.iterate(function(data) {
                    iterateCount++;
		    console.log(iterateCount);
                    reader.close();
                });
            },1000);
            }
        });

        writer.write(dataOrig1);
        writer.write(dataOrig2);

	setTimeout(function() {
	   if (iterateCount == 1) {
		ok(true, "Iterator only received one because of close.");
		factory.close();
		QUnit.start();
	   } else {
		ok(false, "expected iterateCount == 1");
	   }
	},2000);
    },
    function(error) {
        ok(false, error);
        QUnit.start();
    }
    );
});

test("TC20980_StateTestRemoveInstance", function() {
    expect(5); //number of assertions in this test

    QUnit.stop();

    var reader;
    var writer;
    var factory;

    Qeo.createFactory(factoryOptions).then(function(_factory) {
        ok(true, "factory created");
        factory = _factory;
        var promisedWriter = factory.createStateWriter("org::qeo::junit::State");
        var promisedReader = factory.createStateChangeReader("org::qeo::junit::State");

        return promisedWriter.then(function(_writer) {
            ok(true, "writer created");
            writer = _writer;
            return promisedReader.then(function(_reader) {
                ok(true, "reader created");
                reader = _reader;
            });
        });
    }).then(function() {
        ok(true, "readers/writers created");
        var instance1 = {
	    'id' : 1,
            'value' : 'value1'
        };
        var instance2 = {
	    'id' : 2,
            'value' : 'value2'
        };
        reader.on("data", function(data) {
		if (data.id == 1) {
			writer.write(instance2);
		}
		if (data.id == 2) {
			writer.remove(instance1);
		}
        });
	reader.on("remove", function(data) {
		if (data.id == 1) {
			// Correct data removed.
			ok(true, "Disposed instance with key 1");
			reader.close();
			writer.close();
			factory.close();
			QUnit.start();
		} else {
			ok(false, "Disposed the wrong instance");
			reader.close();
			writer.close();
			factory.close();
			QUnit.start();
		}
	});

        writer.write(instance1);

    },
    function(error) {
	console.log(error);
        ok(false, "should not come here");
        QUnit.start();
    }
    );

});

test("TC21156_EventTestAllTypesFloat", function() {
    Qeo.registerType({
        "topic": "org::qeo::junit::eventTestAllTypes::TestTypeFloat",
        "behavior": "event",
        "properties": {
            "float": {
                "type": "float32"
            }
        }
    });

    var dataOrig = { "float": 1.2345 };

    expect(6); //number of assertions in this test
    QUnit.stop(); //start async test

    var reader;
    var writer;
    var factory;

    Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            var promisedWriter = factory.createEventWriter("org::qeo::junit::eventTestAllTypes::TestTypeFloat");
            var promisedReader = factory.createEventReader("org::qeo::junit::eventTestAllTypes::TestTypeFloat");

            return promisedWriter.then(function(_writer) {
                ok(true, "writer created");
                writer = _writer;
                return promisedReader.then(function(_reader) {
                    ok(true, "reader created");
                    reader = _reader;
                });
            });
        }
     ).then(
         function() {
             ok(true, "readers/writers created");
             reader.on("data",function (data) {
                   ok(true, "Got some data");
                   if (Math.abs(data.float - dataOrig.float) < 0.00001) {
			         ok(true, "Float correctly received");
   		         } else {
                        ok(false, "Invalid value (" + data.float + " != " + dataOrig.float + ")");
		         }
                   //closing
                   reader.close();
                   writer.close();
                   factory.close();
                   QUnit.start();
             });
             writer.write(dataOrig);
         }
     );    
});

test("TC20976_EventTestComplexType", function() {
     Qeo.registerType({
     "topic": "org::qeo::junit::eventTestAllTypes::B",
     "properties": {
     "BInt": {
     "type": "int32"
     }
     }
     });

     Qeo.registerType({
     "topic": "org::qeo::junit::eventTestAllTypes::A",
     "properties": {
     "B": {
     "type": "object", 
     "item": "org::qeo::junit::eventTestAllTypes::B"
     },
     "AInt": {
     "type": "int32"
     }
     }
     });

     Qeo.registerType({
    	 "enum": "org::qeo::junit::eventTestAllTypes::enum",
    	 "values": {
    		 "ZERO": 0,
    		 "ONE": 1,
    		 "TWO": 2,
    	 }
     });

     Qeo.registerType({
    	 "topic": "org::qeo::junit::eventTestAllTypes::TestAllTypes",
    	 "behavior": "event",
    	 "properties": {
    		 "string": {
    			 "type": "string"
    		 },
    		 "boolean": {
    			 "type": "boolean"
    		 },
    		 "byte": {
    			 "type": "byte"
    		 },
    		 "int16": {
    			 "type": "int16"
    		 },
    		 "int32": {
    			 "type": "int32"
    		 },
    		 "int64": {
    			 "type": "int64"
    		 },
    		 "A": {
    			 "type": "object", 
    			 "item": "org::qeo::junit::eventTestAllTypes::A"
    		 },
    		 "enum": {
    			 "type": "enum",
    			 "item": "org::qeo::junit::eventTestAllTypes::enum"
    		 }
    	 }
     });

     var sampleEnum = Qeo.getEnum("org::qeo::junit::eventTestAllTypes::enum");
     ok(sampleEnum != null, "sampleEnum should not be null");
     var dataOrig = { "string": "bla", "boolean": true, "byte": 1, "int16": 12345,"int32":12345,"int64": "12345", 
		       "A" : {
			 "B" : {
			      "BInt" : 5678
			 },
			 "AInt" : 1234
                       },
              "enum" : sampleEnum.ONE
		    };
     testEventWrite(factoryOptions, "org::qeo::junit::eventTestAllTypes::TestAllTypes", dataOrig, dataOrig,
    		 undefined, undefined, undefined, genericExpects + 1);
});

test("TC20976_EventTestBasicSequence", function() {
     Qeo.registerType({
     "topic": "org::qeo::junit::eventTestSequences::BasicSequence",
     "behavior": "event",
     "properties": {
          "stringsequence": {
               "type": "array",
               "items": {
               "type": "string"
               }
          },
          "int16sequence": {
               "type": "array",
               "items": {
               "type": "int16"
               }
          },     
          "int32sequence": {
               "type": "array",
               "items": {
               "type": "int32"
               }
          },       
          "int64sequence": {
               "type": "array",
               "items": {
               "type": "int64"
               }
          },
     }
     });

     var dataOrig = { "stringsequence": [ "test1", "test2", "test3" ]
                      , "int16sequence" : [1,2,3] 
                      , "int32sequence" : [1,2,3]                       
                      , "int64sequence" : ["1","2","3"]                                      
                      };
     testEventWrite(factoryOptions, "org::qeo::junit::eventTestSequences::BasicSequence", dataOrig, dataOrig);
});

test("TC20976_EventTestStructSequence", function() {
     Qeo.registerType({
          "topic": "org::qeo::junit::eventTestSequences::B",
          "properties": {
               "B": {
                    "type": "int32"
               }
          }
     });
     Qeo.registerType({
          "topic": "org::qeo::junit::eventTestSequences::StructSequence",
          "behavior": "event",
          "properties": {
               "structsequence": {
                    "type": "array", 
                    "items": {
                         "type": "object",
                         "item": "org::qeo::junit::eventTestSequences::B"
                    }
               }
          }
     });

     var dataOrig = { "structsequence": [
			{ "B" : 1}, { "B" : 2 }, { "B" : 3 }
		      ]
		     };
     testEventWrite(factoryOptions, "org::qeo::junit::eventTestSequences::StructSequence", dataOrig, dataOrig);
});

function stateTestUnicode(start, stop) {
    var UniTop = stop;
    var delta = stop-start;
    if (8232 <= stop && 8232 >= start) {
     delta--;
    }
    if (0 <= stop && 0 >= start) {
     delta--;
    }    
    if (8233 <= stop && 8233 >= start) {
     delta--;
    }
        
    expect(5); //number of assertions in this test

    QUnit.stop();

    var reader;
    var writer;
    var factory;

    var currentUni = start;
    var received = 0;

    function writeNextChar() {
       currentUni++;
       if (currentUni == 0 || currentUni == 8232 || currentUni == 8233) {
            writeNextChar();
       } else {
            var dataOrig = {
                 "id": currentUni,
                 "value" : String.fromCharCode(currentUni)
            };
            writer.write(dataOrig);                
       }
    }

    Qeo.createFactory(factoryOptions).then(function(_factory) {
        ok(true, "factory created");
        factory = _factory;
        var promisedWriter = factory.createStateWriter("org::qeo::junit::State");
        var promisedReader = factory.createStateChangeReader("org::qeo::junit::State");

        return promisedWriter.then(function(_writer) {
            ok(true, "writer created");
            writer = _writer;
            return promisedReader.then(function(_reader) {
                ok(true, "reader created");
                reader = _reader;
            });
        });
    }).then(function() {
        ok(true, "readers/writers created");
        
        reader.on("data",function (data) {
            if (String.fromCharCode(currentUni)==data.value) {
                received++;
            }
     	  if (currentUni == UniTop) {
        	     reader.close();
		     writer.close();
		     factory.close();
		     if (received == delta) {
		          ok(true, "All unicode characters received");
		     }
	          QUnit.start();
     	 } else {
		     writeNextChar();
	      } 
        });
        writeNextChar();
    },
    function(error) {
        ok(false, "should not come here");
        QUnit.start();
    }
    );
}

for (i = 0 ; i < 10000 ; i=i+500) {
     test("TC20976_StateTestUnicode_" + i + "_" + (i+500), function() {
          stateTestUnicode(i,i+500);
     });
}


test("TC21156_TestTypeFloatNaN", function() {
    Qeo.registerType({
        "topic": "org::qeo::junit::eventTestAllTypes::TestTypeFloat",
        "behavior": "event",
        "properties": {
            "float": {
                "type": "float32"
            }
        }
    });

    var dataOrig = { "float": Number.NaN };

    testEventWrite(factoryOptions, "org::qeo::junit::eventTestAllTypes::TestTypeFloat", dataOrig, null);
});

test("TC21156_TestTypeFloatNumberInfinity", function() {
    Qeo.registerType({
        "topic": "org::qeo::junit::eventTestAllTypes::TestTypeFloat",
        "behavior": "event",
        "properties": {
            "float": {
                "type": "float32"
            }
        }
    });

    var dataOrig = { "float": 1.7976931348623157E+10308 };

    testEventWrite(factoryOptions, "org::qeo::junit::eventTestAllTypes::TestTypeFloat", dataOrig, null);
});

test("TC21156_TestTypeFloatInfinity", function() {
    Qeo.registerType({
        "topic": "org::qeo::junit::eventTestAllTypes::TestTypeFloat",
        "behavior": "event",
        "properties": {
            "float": {
                "type": "float32"
            }
        }
    });

    var dataOrig = { "float": Infinity };

    testEventWrite(factoryOptions, "org::qeo::junit::eventTestAllTypes::TestTypeFloat", dataOrig, null);
});

test("TC21156_TestTypeFloatZero", function() {
    Qeo.registerType({
        "topic": "org::qeo::junit::eventTestAllTypes::TestTypeFloat",
        "behavior": "event",
        "properties": {
            "float": {
                "type": "float32"
            }
        }
    });

    var dataOrig = { "float": 0.0 };

    testEventWrite(factoryOptions, "org::qeo::junit::eventTestAllTypes::TestTypeFloat", dataOrig, {"float": 0});
});

test("TC21021_getDeviceIdTest", function() {
    expect(7); //number of assertions in this test
    QUnit.stop(); //start async test
    
    var reader;
    var factory;
    
    Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            var promisedReader = factory.createStateReader("org::qeo::system::DeviceInfo");

            return promisedReader.then(function(_reader) {
                ok(true, "reader created");
                reader = _reader;
            });
        }
    ).then(
        function() {
            ok(true, "readers/writers created");

            factory.getDeviceId().then(
                function(deviceId){
                    ok(true, "deviceId fetched");
                    ok(typeof deviceId.lower === "string"); //check that int64 are returned as string
                    ok(typeof deviceId.upper === "string");

                    var foundOwnDeviceId = false;
                    reader.iterate().then(function(elements){
                        for (var i = 0; i < elements.length; ++i) {
                            var deviceId2 = elements[i].deviceId;
                            if (deviceId.upper == deviceId2.upper && deviceId.lower == deviceId2.lower) {
                                foundOwnDeviceId = true;
                                break;
                            }
                        }
                        ok(foundOwnDeviceId, "could not find own deviceId");
                        reader.close();
                        factory.close();
                        QUnit.start();
                    });
                }, function(error) {
                    ok(false, "Cannot get deviceId: " + error);
                    reader.close();
                    factory.close();
                    QUnit.start();
                }
            );
          }, function(error) {
            ok(false, "Cannot create stateReader: " + error);
            reader.close();
            factory.close();
            QUnit.start();
        }
    );

});

//Create 2 writers from same type, close 1 and check if 2nd keeps on working
test("CloseWriterFromSameType", function() {
    expect(8); //number of assertions in this test
    QUnit.stop(); //start async test
    
    var reader;
    var writer1;
    var writer2;
    var factory;
    var dataOrig = { 'data' : '456875125', 'nr': 554};
    function onData123(data)
    {
    	ok(true, "data received");
        deepEqual(data, dataOrig);
        //closing
        reader.close();
        writer2.close();
        factory.close();
        QUnit.start(); //continue with next test
    }
    
        Qeo.createFactory(factoryOptions).then(
        function(_factory){
            ok(true, "factory created");
            factory = _factory;
            var promisedWriter1 = factory.createEventWriter("org::qeo::junit::Event");
            var promisedWriter2 = factory.createEventWriter("org::qeo::junit::Event");
            var promisedReader = factory.createEventReader("org::qeo::junit::Event", {"on": {"data": onData123}});

            return promisedWriter1.then(function(_writer) {
                ok(true, "writer1 created");
                writer1 = _writer;
                return promisedWriter2.then(function(_writer) {
                    ok(true, "writer2 created");
                    writer2 = _writer;
                    return promisedReader.then(function(_reader) {
                        ok(true, "reader created");
                        reader = _reader;
                    });
                });
                
            });
        }
     ).then(
         function() {
        	 ok(true, "readers/writers created");
             writer1.close();
             
             //give the writer a bit of time to close
             setTimeout(function() {
            	 ok(true, "Writing on 2nd writer");
            	 writer2.write(dataOrig);
          	 },200);
             
         }
     );
});
