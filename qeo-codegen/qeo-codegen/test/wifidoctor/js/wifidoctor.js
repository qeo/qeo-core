/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/
if (typeof Qeo === "undefined") {Qeo = {registerType: function(td) {this.ttr.push(td);},ttr: []};}

Qeo.registerType({
"topic": "com::technicolor::wifidoctor::accesspoint::StationStats",
"properties": {
"MACAddress": {
"type": "string"
}, "maxPhyRate": {
"type": "int32"
}, "RSSIuplink": {
"type": "int32"
}, "avgSpatialStreamsUplink": {
"type": "float32"
}, "avgSpatialStreamsDownlink": {
"type": "float32"
}, "trainedPhyRateUplink": {
"type": "int32"
}, "trainedPhyRateDownlink": {
"type": "int32"
}, "dataRateUplink": {
"type": "int32"
}, "dataRateDownlink": {
"type": "int32"
}, "pctPowerSave": {
"type": "int32"
}
}
});
Qeo.registerType({
"topic": "com::technicolor::wifidoctor::accesspoint::BSSID",
"behavior": "state",
"properties": {
"testId": {
"type": "int32", "key": true
}, "MACAddress": {
"type": "string", "key": true
}, "radio": {
"type": "int32"
}, "mediumBusyIBSS": {
"type": "byte"
}, "mediumBusyOBSS": {
"type": "byte"
}, "stationStats": {
"type": "array", 
"items": {
"type": "object",
"item": "com::technicolor::wifidoctor::accesspoint::StationStats"
}
}
}
});
Qeo.registerType({
"topic": "com::technicolor::wifidoctor::station::Statistics",
"behavior": "state",
"properties": {
"testId": {
"type": "int32", "key": true
}, "MACAddress": {
"type": "string", "key": true
}, "radio": {
"type": "int32"
}, "RSSIdownlink": {
"type": "int32"
}
}
});
Qeo.registerType({
"topic": "com::technicolor::wifidoctor::Radio",
"behavior": "state",
"properties": {
"testId": {
"type": "int32", "key": true
}, "id": {
"type": "int32", "key": true
}, "device": {
"type": "object", 
"item": "org::qeo::DeviceId"
}, "frequency": {
"type": "int32"
}, "mediumBusy": {
"type": "byte"
}
}
});
Qeo.registerType({
"topic": "com::technicolor::wifidoctor::ScanListEntry",
"properties": {
"BSSID": {
"type": "string"
}, "SSID": {
"type": "string"
}, "capabilities": {
"type": "string"
}, "frequency": {
"type": "int32"
}, "level": {
"type": "int32"
}
}
});
Qeo.registerType({
"topic": "com::technicolor::wifidoctor::ScanList",
"behavior": "state",
"properties": {
"radio": {
"type": "int32", "key": true
}, "timestamp": {
"type": "int64"
}, "list": {
"type": "array", 
"items": {
"type": "object",
"item": "com::technicolor::wifidoctor::ScanListEntry"
}
}
}
});
Qeo.registerType({
"topic": "com::technicolor::wifidoctor::ScanListRequest",
"behavior": "event",
"properties": {
"radio": {
"type": "int32"
}
}
});
Qeo.registerType({
"topic": "com::technicolor::wifidoctor::TestRequest",
"behavior": "state",
"properties": {
"id": {
"type": "int32", "key": true
}, "tx": {
"type": "string"
}, "rx": {
"type": "string"
}, "type": {
"type": "int32"
}, "count": {
"type": "int32"
}, "size": {
"type": "int32"
}, "interval": {
"type": "int32"
}, "timeout": {
"type": "int32"
}, "duration": {
"type": "int32"
}, "packetSize": {
"type": "int32"
}, "modulation": {
"type": "int32"
}, "rateIndex": {
"type": "int32"
}, "priority": {
"type": "byte"
}, "AMPDU": {
"type": "boolean"
}
}
});
Qeo.registerType({
"topic": "com::technicolor::wifidoctor::TestState",
"behavior": "state",
"properties": {
"id": {
"type": "int32", "key": true
}, "participant": {
"type": "string", "key": true
}, "state": {
"type": "int32"
}
}
});

