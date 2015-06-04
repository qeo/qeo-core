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

// ---[ topic definition ]----------------------------------------------------

var namespace = "com.technicolor.demo.chat";
var topic = {
    "ChatMessage" : {
      "chatbox" : {
        "_type": "18string"
      },
      "from" : {
        "_type": "18string"
      },
      "message" : {
        "_type": "18string"
      }
    }
};

//---[ Global variables ]-----------------------------------------------------

var chatboxes = {"DDS" : ""}; // a dictionary of known chatboxes and their content
var reader, writer;

//---[ Qeo interfacing logic ]------------------------------------------------

function initializeConnection()
{
    // Init global variables used by the Qeo library
	Qeo.DefaultURI = "localhost:8888";
	Qeo.DebugLog = log;
    updateChatboxes("DDS");
    messages = document.getElementsByName('messages');
    messages[0].value = chatboxes['DDS'];
    log('document.ready');
    reader = new Qeo.eventReader(namespace, topic,
    {
        'onData': rxData,
        'onReady': function (rc) {
            if (Qeo.QeoRetCode.OK != rc) {
                log('reader creation failed');
            } else {
                log('reader created');
            }
        }
    });
    writer = new Qeo.eventWriter(namespace, topic,
    {
    	'onReady' : function (rc) {
            if (Qeo.QeoRetCode.OK != rc) {
                log('writer creation failed');
            } else {
                log('writer created');
            }
    	}
    });
}

function doWrite()
{
    chatboxes_dropdown = document.getElementsByName("chatboxes")[0];
    message = {"chatbox" : chatboxes_dropdown.value,
               "from" : document.getElementById('username').value,
               "message" : document.getElementById('message').value};
    writer.write(message);
    document.getElementById('message').value = "";
}

function rxData(data) {
    if (!(data['chatbox'] in chatboxes)) {
        updateChatboxes(data['chatbox']);
    }
    chatboxes[data['chatbox']] += "<" + data['from'] + ">: " + data['message'] + "\n";
    chatboxes_dropdown = document.getElementsByName("chatboxes")[0];
    if (chatboxes_dropdown.value == data['chatbox']) {
        messages = document.getElementsByName('messages');
        messages[0].value = chatboxes[data['chatbox']];
        messages[0].scrollTop = messages[0].scrollHeight;
    }
}

//---[ application logic ]----------------------------------------------------

function newChatbox(room)
{
    if (!(room.value in chatboxes)) {
        updateChatboxes(room.value);
    }
}

function log(msg)
{
    logmsg=document.getElementById('logmsg');
    logmsg.innerHTML = msg + '<br/>' + logmsg.innerHTML;
}

function updateChatboxes(newbox)
{
    chatboxes[newbox] = "";
    chatboxes_dropdown = document.getElementsByName("chatboxes")[0];
    var option = document.createElement("option");
    option.text = newbox;
    chatboxes_dropdown.add(option, null);
}

function chatBoxChange(box)
{
        messages = document.getElementsByName('messages');
        messages[0].value = chatboxes[box.value];
        messages[0].scrollTop = messages[0].scrollHeight;
}
