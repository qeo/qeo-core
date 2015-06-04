<a name=Top></a>
#Table of Contents

[Introduction](#Introduction)

[How to Use the iOS SDK](#HowToUse)

[Authentication Procedure](#Authentication)

[Programmer's Guide](#ProgGuide)

[QSimpleChat Sample App Documentation](#QSimpleChat)

[VoIP QSimpleChat Sample App Documentation](#VoIPQSimpleChat)

[QGaugeReader Sample App Documentation](#QGaugeReader)

<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>

<a name=Introduction> </a> 
[Back to Top](#Top)
#A Short Introduction to Qeo 



##What is Qeo?

#### About Qeo
The Qeo framework aims to improve the user experience accross multiple devices and platforms by providing a data-centric publish-subscribe communication mechanism.
It allows devices to share information directly, regardless of manufacturer, device type or operating system.

The most interesting features of Qeo are:

- Communication across platforms.
- User security based on certificates.
- Easy user and device configuration.
- No need for a central controlling service or continuous Internet access (due to its distributed communication mechanism).

<br>
<br>
<br>
<br>

##Qeo Architecture

#### Qeo Hierarchy 

Qeo provides a security framework that defines the boundaries of Qeo interactions between users, their devices and their applications.

This framework is centered around the following hierarchy of entities:


- **Realm**: a set of users, devices and applications that can exchange information using Qeo information units (Topics).
- **User** : groups one of more devices.
- **Device**: runs applications that can read or write Qeo Topics.

#### The Qeo Data Model

The Qeo framework securely exchanges information Qeo Topics. These Topics have a certain structure. You can describe this structure using the Qeo Data Model (QDM). These can then be automatically translated into Objective C code objects using the Code Generator. 

#### Factories. Readers and Writers

The Qeo framework defines 3 types of entities that you can use in your App:

- Qeo Factory: encapsulates your application's connection to a Qeo Realm. Once the Qeo Factory is initialized, you can create Readers and Writers.
- Writer: provides data on a Topic within your Realm.
- Reader: consumes data on a Topic within your Realm by means of listeners.

Readers and writers are created for a specific Topic and for a specific Qeo behavior (State or Event).

####More Information

For more information, refer to:

- [Qeo System Description](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+System+Description).
- [Introduction to Qeo Security](http://wiki.qeo-app-development.com/display/QeoCusDoc/Introduction+to+Qeo+Security).
- [QDM Developer Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+Data+Model+%28QDM%29+Developer+Guide).
- [Code Generator User Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Code+Generator+User+Guide).

<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>

<a name=HowToUse> </a>
[Back to Top](#Top)
#Using the Qeo SDK 


## Contents of the SDK

####Folder Structure

After unpacking the SDK, all iOS related files are located in the iOS folder:

*QeoHome* <br>
*--ios*<br>
*------framework*<br>
*--------qeo.bundle*<br>
*--------qeo.framework*<br>
*------doc*<br>
*--------com.technicolor.Qeo.[release number].docset*<br>
*------samples*<br>
*--------sample-qsimplechat*<br>
*--------sample-voip-qsimplechat*<br>
*--------sample-qgaugereader*<br>

####Description

The SDK contains:

- **The Qeo Framework**, which you need to add to your project in order to be able to allocate a Qeo Factory and create Readers and Writers.
- **The Qeo Bundle**, which contains the UI for the OTC popup and authentication.
- **The doc folder**, which contains the documentation.
- **The Samples folder**, which contains the sample Apps.
 
Available Sample Apps:

- **QSimpleChat** is a basic chat application. 
- **VoIP QSimpleChat** is a basic chat application, but it maintains a connection to a Qeo Background Notification Server. The VoIP feature is set as background mode in the App main plist file, which allows it to keep a TCP connection open in suspended state.
- **QgaugeReader** is an app that reads  network usage information published by other devices and presents it in a graphical way.

<br>
<br>
<br>
<br>

##Adding the Qeo Framework

The Qeo framework is composed of 2 sets of files:

- Qeo.framework
- Qeo.bundle

####Qeo.framework

To add it to your Xcode project:

1. Select your project in the source tree then select your target.
2. Go to the **Build Phases** section.
3. Extend **Link Binary With Libraries** and click the **+** icon.
4. From the popup dialog click the **Add Other...** button and navigate to the *Qeo.framework* file.
5. Click **Open** to add the framework to your project.<br>
   
####Qeo.bundle

To add it to your Xcode project:

1. Select the framework group in the source tree and right click on it to get a popup.
2. Select **Add Files to ...** and navigate to the *Qeo.bundle* file. 
3. Click **Add** button to add it to your project.


**Note:** Verify that the Qeo.bundle file is also added to the "Copy Bundle Resources" section under your project's target "Build Phases". 

		 
####Additional Frameworks

You need three additional frameworks from the standard set of available frameworks in Xcode (*AVFoundation.framework*, *Coregraphics.framework* and *Security.framework*:

1. In the **Build Phases** section, click the "+" icon.
2. In the list, select the above frameworks.
3. Click **Open** to add the frameworks to your project.

<br>
<br>
<br>
<br>


##Adding and Removing Documentation

####How to Install the Qeo documentation

Copy the *docset* file to:

     ~/Library/Developer/Shared/Documentation/DocSets.

**Note:**

The documentation can be viewed from within Xcode. Go to **Help > Documentation and API Reference**. Qeo documentation is versionized, this means that you can have multiple versions of the help next to each other.
	  
####How to Remove the Qeo documentation:

Go to:

    ~/Library/Developer/Shared/Documentation/DocSets
and delete the *docset* file .

**Note:** If the documentation was still selected in Xcode, deleting it can cause Xcode to crash. We therefore recommend closing Xcode before deleting the docset. 

<br>
<br>
<br>
<br>


##Installing the Samples

####Procedure

Proceed as follows to install a sample:

1. Open Xcode
2. Open the Sample project of your choice.

You can run the samples on the simulator or on a real device. 

<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>

<a name=Authentication></a>
[Back to Top](#Top)
#Authentication Procedure



##About Qeo Security

Qeo is a secure system. Communication between applications on a user's devices is only possible if those users and devices have been added to the same Qeo Realm. You can do this using the Security Management Server located here:
[http://my.qeo.org/](http://my.qeo.org)

When a device is authenticated, a certificate is downloaded from the Security Management Server (SMS). This is done the first time a Qeo application is run on the device.

For more details about Qeo Security, refer to: 
[Qeo Security User Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+Security+User+Guide).

<br>
<br>
<br>
<br>


##How to Register and Authenticate a Device

###Prerequisites

The following prerequisites must be met:

- You have already created a Qeo Realm
- You have already created a user
- You have already requested a One-Time-Code (OTC) to register your device.

For more details, refer to the [Qeo Security User Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+Security+User+Guide). Note that the lifetime of the OTC is 2 minutes. Make sure that you are ready to perform the steps below immediately after requesting the OTC.<br>

###Procedure

Proceed as follows:

1. Start any Qeo application. The OTC pop-up appears.
2. Fill in the OTC you requested. You can also scan the QR Code on the SMS.
3. Click **Validate**.

The device will now contact the SMS, which will validate the OTC and provide a certificate. Note that for this step, your device needs to be connected to the Internet. Once credentials are verified, the associated certificates are stored in the App sandbox. 
When the App is re-opened at a later point of time it will reuse the existing credentials.

<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>

<a name=ProgGuide></a>
[Back to Top](#Top)
#Basic Qeo Programmer's Guide


##Introduction

#### About Qeo

The Qeo framework aims to improve the user experience accross multiple devices and platforms by providing a data-centric publish-subscribe communication mechanism.

It allows devices to share information directly, regardless of manufacturer, device type or operating system.

For an introduction to Qeo, read the *Short Introduction to Qeo* in this documentation set. For more information, read the [Qeo System Description](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+System+Description). 

####About the Qeo SDK

The Qeo SDK contains all necessary files to start developing Qeo-enabled iOS applications. These files are located in:
*[QeoSDK Home Folder]/ios*.

The SDK contains three sample applications that will help you to understand how a Qeo application works:

- **QSimpleChat:** a basic chat application that allows devices within a Qeo Realm to exchange chat messages between devices in your Realm.
- **QSimpleChat with VoIP:** same basic chat application, but this time it maintains a connection to a Qeo Background Notification Server. The VoIP feature is set as background mode in the App main plist file, which allows it to keep a TCP connection open in suspended state. 
- **QGauge Reader:** an application that shows network interface information published by other devices in your Realm.

The SDK is designed to work with Xcode version 6 and up. 


####Using the SDK

The SDK contains two objects you need to add to your project in Xcode:

- **The Qeo Framework**, which you need to add to your project in order to be able to allocate a Qeo Factory and create Readers and Writers.
- **The Qeo Bundle**, which contains the UI for the OTC popup and authentication.

For more information, refer to the section *How to Use the Qeo SDK* in this docset.

<br>
<br>
<br>
<br>

## Using the Code Generator

####About the Code Generator

You can represent a Qeo topic using the xml-based [Qeo Data Model (QDM)](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+Data+Model+%28QDM%29+Developer+Guide). The purpose of the Qeo code generator is to translate a QDM file into one or more obj-c source files. These source files can then be used in your Xcode projects. The executable is located in qeo-codegen/bin. It generates a .h and .m file for your QDM.

The Code Generator is located in the tools subfolder of the SDK.

For More information, refer to:

- [Code Generator User Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Code+Generator+User+Guide)
- [Qeo Data Model Developer Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+Data+Model+%28QDM%29+Developer+Guide)

####Usage 

The usage of the command is:

    qeo-codegen [options ...] [file ...]

The following options are available:

- [-a <namespace::prefix=abbreviation>]: Abbreviate  namespace::prefix as abbreviation
- [-h] : Display command help.
- [-l] <arg>: Specify the language for which to generate code. Specify "objectiveC"
- [-o] <arg> : Specify the output directory for the generated code. Default is the current directory.

**Note**:  Alternatively, you can use *--abbrev*, *--help* or *--output*.

####Example
Below is an example:

    qeo-codegen -l objectiveC <MyQdmFile.xml> 

This command generates *MyQdmFile.h* and *MyQdmFile.m*. Since no input or output path is specified, the MyQdmFile.xml must be located in the /bin folder. The generated files are also saved in that folder.

<br>
<br>
<br>
<br>

##Using the Qeo Framework


####Initializing the Factory

A factory needs to be created in a background thread.If not, it will block the UI and your app will hang. To do this, we use [Grand Central Dispatch (GCD)](https://developer.apple.com/library/mac/documentation/performance/reference/gcd_libdispatch_ref/Reference/reference.html). The code snippet below shows how to do this:

    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        self.factory = [[QEOFactory alloc] initWithError:nil];
        if (self.factory == nil){
            NSLog(@"Could not make factory");
            return;
        }
    });


#### Qeo Credentials

The first time a Factory is initialised for your application, the user is presented with a screen to enter Qeo credentials (OTC and SMS URL). Once credentials are verified, the associated certificates are stored in the App sandbox. When the Qeo Factory is re-created at a later point of time it will reuse the existing credentials.

After a Factory is setup, you can allocate Readers and Writers on the desired topics.

#### Qeo Behaviours

A Qeo Topic can have two possible behaviours:

- Event Topics
- State Topics

Refer to the [Qeo System Description](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+System+Description) for more information about behaviours. 

<br>
<br>
<br>
<br>

## Creating Readers and Writers

#### Creating an Event Reader
The code snippet below shows how to initialize an Event Reader for the ChatMessage topic in the QSimpleChat sample App:


    self.eventReader = [[QEOEventReader alloc] initWithType:[org_qeo_sample_simplechat_ChatMessage class]
                                                    factory:factory
                                                   delegate:self
                                             entityDelegate:nil
                                                      error:&error];
    if (nil == self.eventReader){
        dispatch_async(dispatch_get_main_queue(), ^{
            // Show Alert box
            self.alert = [[UIAlertView alloc] initWithTitle:@"Qeo Event Writer"
                                                    message:[error localizedDescription]
                                                   delegate:self
                                          cancelButtonTitle:@"Cancel"
                                          otherButtonTitles: nil];
            [self.alert show];
        });
        return;
    }

#### Creating an Event Writer
The code snippet below shows how to initialize an Event Writer for the ChatMessage topic in the QSimpleChat sample App:

    self.eventWriter = [[QEOEventWriter alloc] initWithType:[org_qeo_sample_simplechat_ChatMessage class]
                                                    factory:factory
                                             entityDelegate:nil
                                                      error:&error];
    if (nil == self.eventWriter){
        dispatch_async(dispatch_get_main_queue(), ^{
            // Show Alert box
            self.alert = [[UIAlertView alloc] initWithTitle:@"Qeo ChatParticipant Writer"
                                                    message:[error localizedDescription]
                                                   delegate:self
                                          cancelButtonTitle:@"Cancel"
                                          otherButtonTitles: nil];
            [self.alert show];
        });
        return;
    }

#### Creating a State Reader
The code snippet below shows an example of how to initialize a State Reader for the netStatMessage topic:

    _stateReader=[[QEOStateReader alloc]initWithType:
    [org_qeo_sample_gauge_NetStatMessage class] factory:_factory 
                                               delegate:self 
                                         entityDelegate:nil 
                                                  error:nil];
    if(_stateReader==nil) {
        NSLog(@"Could not make state Reader");
        return;
    } 

#### Creating a State Writer

The code snippet below shows an example of how you can initialize a State Writer:


     QEOStateWriter *stateWriter;
     stateWriter = [[QEOStateWriter alloc]initWithType:[TestState class]
                                               factory:_factory
                                        entityDelegate:self
                                                 error:&error];

The code snippet below shows an example of how you can write a Topic to the Writer: 

    [stateWriter write:test withError:error];

<br>
<br>
<br>
<br>

## Switching To and Returning From Background

####Handling Switchover to and From Background

The Qeo SDK is extended to handle foreground/background switchovers. It provides means to suspend and resume  the Qeo communication. There is no need to close factories, readers or writers when suspending Qeo. Default handling for Apps is to suspend Qeo when going to the background and resume it when entering the foreground. You will still miss incoming events in suspended state. However, when you return to the foreground, you do not need to republish any state instances. <br><br>
For VoIP Apps an addituonal server (Qeo Background Notification Server) is developed to cache state events on certain registered topics when Qeo is in suspended state. The Qeo layer will keep a TCP connection open in order to receive notifications about pending cached Qeo data on the server. To actually download the pending data you will need to resume the Qeo communication. In iOS one TCP connection can be kept alive by the OS (when in suspended background mode) if the App has enabled the VoIP feature in its main plist. 

####Checking for Qeo Reset

When returning from background, existing credentials are used to initialize the Qeo Factory. However, you can provide an option to clear the Qeo credentials, thus restarting the Qeo Authentication cycle the next time the app is started. Whenever the user has indicated he wants to reset Qeo credentials, it is the developer's responsibility to close all readers/writer and close the factory (and then create a new one). For an example of how you can accomplish this, refer to the *QSimpleChat* sample documentation.

<br>
<br>
<br>
<br>
<br>
<br>

<a name=QSimpleChat></a>
[Back to Top](#Top)
# QSimpleChat Sample Application Documentation 





## About QSimpleChat

#### What is QSimpleChat

QSimpleChat is a basic chat application that allows devices within a Qeo Realm to exchange chat messages.


#### Location Of the Files

The files related to this application are located in the *QeoSDKHome/ios/samples/sample-qsimplechat* folder of the SDK.

#### More Information
For more information, refer to the Qeo iOS Developer Guide.

<br>
<br>
<br>
<br>

## How to Install and Run the Sample

#### Installation Procedure
Proceed as follows to install the sample:

1. Start Xcode.
2. Open the QSimplechat project.
3. Note the *Qeo bundle* and *Qeo framework* references. These are necessary for a Qeo-enabled project. The Qeo Framework provides the ability to create and intialize a  Qeo Factory, which allows you to create Readers and Writers. The Qeo Bundle provides the UI for authentication.

You can now build and run the sample on a connected device or the simulator.

<br>
<br>
<br>
<br>

## Application Details

#### General Information

For general information about Qeo entities, the Qeo Data Model (QDM) and the Code Generator, go here:

- [Qeo System Description](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+System+Description).<br>
- [Introduction to Qeo Security](http://wiki.qeo-app-development.com/display/QeoCusDoc/Introduction+to+Qeo+Security).<br>
- [QDM Developer Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+Data+Model+%28QDM%29+Developer+Guide).<br>
- [Code Generator User Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Code+Generator+User+Guide).<br>

Also, refer to the Qeo iOS Developer Guide.

#### About the QDM and the Code Generator ###

QSimpleChat uses the Topic ChatMessage, which is an Event Topic. 

This topic is formalised in its Qeo Data Model (QDM) , an XML-based format. This XML file is then translated into *.h* and *.m* files using the Code Generator, and included in the project.

<br>
<br>
<br>
<br>

## Handling Qeo ##

The Qeo part is handled in the View Controllers (TabControllerViewController.m, ViewControllerChat.m and ParticipantViewController.m) and in the AppDelegate.m 

#### General Process

The process is as follows:

1. A Qeo Factory is initialized (see TabControllerViewController.m).
2. An Event Writer is created from the factory for the Chat Message Topic (see ViewControllerChat.m).
3. An Event Reader is created from the factory for the Chat Message Topic (see ViewControllerChat.m).
4. A State Writer is created from the factory for the Chat Participant Topic (see ViewControllerChat.m).
5. A State Change Reader is created from the factory for the Chat Participant Topic (see ParticipantViewController.m).
6. If the Event Reader receives a Chat Topic, the information (sender and message content) is retrieved and sent to the UI. Same thing happend when the State Reader receives a Participant Topic. 
7. If a user types a message in the text box and clicks **Send**, the name of the user and the text in the box are packed in a Chat Message Topic and sent over Qeo, where it will be received by all Event Readers subscribed on that Topic (including the state reader of the user who sent the message).
8. If a user modifies the state (available/busy/away/idle), the state and the name of the user are packed in a Participant Message Topic and send over Qeo, where it will be received by all State Readers subscribed on that Topic (including the state reader of the user who sent the message).

#### Initializing the Factory
A factory needs to be created in a background thread. This is shown (as an example) in the following code snippet:<br>

    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        self.factory = [[QEOFactory alloc] initWithError:nil];
        if (self.factory == nil){
            NSLog(@"Could not make factory");
            return;
        }
    });

#### Creating the Event Reader
The code snippet below shows how to create an Event Reader for the ChatMessage topic:<br>

    self.eventReader = [[QEOEventReader alloc] initWithType:[org_qeo_sample_simplechat_ChatMessage class]
                                                    factory:factory
                                                   delegate:self
                                             entityDelegate:nil
                                                      error:&error];
    if (nil == self.eventReader){
        dispatch_async(dispatch_get_main_queue(), ^{

            // Show Alert box
            self.alert = [[UIAlertView alloc] initWithTitle:@"Qeo Event Writer"
                                                    message:[error localizedDescription]
                                                   delegate:self
                                          cancelButtonTitle:@"Cancel"
                                          otherButtonTitles: nil];
            [self.alert show];
        });
        return;
    }

#### Creating the Event Writer
The code snippet below shows how to create an Event Writer for the ChatMessage topic:<br>

    self.eventWriter = [[QEOEventWriter alloc] initWithType:[org_qeo_sample_simplechat_ChatMessage class]
                                                    factory:factory
                                             entityDelegate:nil
                                                      error:&error];
    if (nil == self.eventWriter){
        dispatch_async(dispatch_get_main_queue(), ^{
            // Show Alert box
            self.alert = [[UIAlertView alloc] initWithTitle:@"Qeo ChatParticipant Writer"
                                                    message:[error localizedDescription]
                                                   delegate:self
                                          cancelButtonTitle:@"Cancel"
                                          otherButtonTitles: nil];
            [self.alert show];
        });
        return;
    }

#### Creating the State Change Reader
The code snippet below shows how to create an State Change Reader for the Participant topic:<br>

    self.participantReader = [[QEOStateChangeReader alloc] initWithType:[org_qeo_sample_simplechat_ChatParticipant class]
                                                                factory:factory
                                                               delegate:self
                                                         entityDelegate:nil
                                                                  error:&error];
    if (self.participantReader == nil) {
        if (UIApplicationStateBackground != [UIApplication sharedApplication].applicationState) {
            dispatch_async(dispatch_get_main_queue(), ^{
                // Show Alert box
                UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Qeo ChatParticipant Reader"
                                                                message:[error localizedDescription]
                                                               delegate:self
                                                      cancelButtonTitle:@"Cancel"
                                                      otherButtonTitles: nil];
                [alert show];
            });
        }
    }

#### Creating the State Writer
The code snippet below shows how to create an State Writer for the Participant topic:

    self.participantWriter = [[QEOStateWriter alloc] initWithType:[org_qeo_sample_simplechat_ChatParticipant class]
                                                          factory:factory
                                                   entityDelegate:nil
                                                            error:&error];
    if (self.participantWriter == nil){
        dispatch_async(dispatch_get_main_queue(), ^{
            // Show Alert box
            self.alert = [[UIAlertView alloc] initWithTitle:@"Qeo ChatParticipant Writer"
                                                    message:[error localizedDescription]
                                                   delegate:self
                                          cancelButtonTitle:@"Cancel"
                                          otherButtonTitles: nil];
            [self.alert show];
        });
        return;
    }

<br>
<br>
<br>
<br>

## Actions for Foreground/Background Switchover

#### Handling Qeo Foregorund/Background Switchover
This is done in the AppDelegate.m. Below is are code snippets showing this:

    - (void)applicationDidEnterBackground:(UIApplication *)application
    {
        // Background mode: Put Qeo to deep sleep
        [QEOFactory suspendQeoCommunication];
    }

    - (void)applicationWillEnterForeground:(UIApplication *)application
    {
        // Foreground mode: Wakeup Qeo
        [QEOFactory resumeQeoCommunication];
    }

#### Returning to Foreground
When the App returns to foreground, it is advised to check whether the user has indicated he wants to reset Qeo credentials:

    // Called when resuming from background mode
    -(void)willContinue
    {
        // Check the user defaults, the user may have set the reset the Qeo identities in
        // the General settings of the App.
        NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
        [standardUserDefaults synchronize];
    
        if (YES == [standardUserDefaults boolForKey:@"reset_Qeo"]) {
            // reset
            [QEOIdentity clearQeoIdentities];
        
            // reset the flag in the user defaults
            [standardUserDefaults setObject:@"NO" forKey:@"reset_Qeo"];
            [standardUserDefaults synchronize];
        
            //Close readers/writers and factory
            _factory = nil;
            _eventReader = nil;
            _eventWriter = nil;
        }
    
        if ((nil == _factory) && (NO == self.qeoRequestStarted)) {
            // re-init Qeo
            [self setupQeoCommunication];
        }
    }




#### Removing Qeo Credentials
A check is also performed here. The *QEOIdentity* object has a class called *clearQeoIdentities*. In the above code snippet, a value is read from the general settings of the App (included as a settings bundle). It checks if a user has requested to remove the Qeo credentials (by checking the value of the corresponding flag). If so, Qeo credentials are first remmoved, and the flag value is reset.

<br>
<br>
<br>
<br>

## User Instructions

#### Procedure
Run the App. When running, a chat window opens. Type your message and click **Send**. 

#### Qeo Credentials

If your device does not yet have Qeo credentials, you need to provide an OTC. Refer to the [Qeo Security Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+Security+User+Guide) for more information

<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>

<a name=VoIPQSimpleChat></a>
[Back to Top](#Top)
# VoIP QSimpleChat Sample Application Documentation 





## About VoIP QSimpleChat

#### What is VoIP QSimpleChat

VoIP QSimpleChat is a basic chat application that allows devices within a Qeo Realm to exchange chat messages. It maintains a TCP connection to a Qeo Background Notification Server. The VoIP feature is set as background mode in the App main plist file, which allows it to keep a TCP connection open in the background while in suspended state.


#### Location Of the Files

The files related to this application are located in the *QeoSDKHome/ios/samples/sample-voip-qsimplechat* folder of the SDK.

#### More Information
For more information, refer to the Qeo iOS Developer Guide.

<br>
<br>
<br>
<br>

## How to Install and Run the Sample

#### Installation Procedure
Proceed as follows to install the sample:

1. Start Xcode.
2. Open the QSimplechat project.
3. Note the *Qeo bundle* and *Qeo framework* references. These are necessary for a Qeo-enabled project. The Qeo Framework provides the ability to create and intialize a  Qeo Factory, which allows you to create Readers and Writers. The Qeo Bundle provides the UI for authentication.

You can now build and run the sample on a connected device or the simulator.<br>
**Note**: Full VoIP feature is only supported on a physical device, the simulator is not reliable.

<br>
<br>
<br>
<br>

## Application Details

#### General Information

For general information about Qeo entities, the Qeo Data Model (QDM) and the Code Generator, go here:

- [Qeo System Description](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+System+Description).<br>
- [Introduction to Qeo Security](http://wiki.qeo-app-development.com/display/QeoCusDoc/Introduction+to+Qeo+Security).<br>
- [QDM Developer Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+Data+Model+%28QDM%29+Developer+Guide).<br>
- [Code Generator User Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Code+Generator+User+Guide).<br>

Also, refer to the Qeo iOS Developer Guide.

#### About the QDM and the Code Generator ###

QSimpleChat uses the Topic ChatMessage, which is an Event Topic. 

This topic is formalised in its Qeo Data Model (QDM) , an XML-based format. This XML file is then translated into *.h* and *.m* files using the Code Generator, and included in the project.

<br>
<br>
<br>
<br>

## Handling Qeo ##

The Qeo part is handled in the View Controllers (TabControllerViewController.m, ViewControllerChat.m and ParticipantViewController.m) and in the AppDelegate.m 

#### General Process

The process is as follows:

1. A Qeo Factory is initialized (see TabControllerViewController.m). When succeeded you provide a callback delegate for Background Notification events. In the sample a BackgroundServiceManager class was created for this purpose.
2. An Event Writer is created from the factory for the Chat Message Topic (see ViewControllerChat.m).
3. An Event Reader is created from the factory for the Chat Message Topic (see ViewControllerChat.m).
4. A State Writer is created from the factory for the Chat Participant Topic (see ViewControllerChat.m).
5. A State Change Reader is created from the factory for the Chat Participant Topic (see ParticipantViewController.m).
6. Enable the Chat Participant Topic of the QEOChangeStateReader for Background Notifications.
7. If Qeo receives a Background Notification, a registered callback hanlder of the App will be called. The App can decide to resume the Qeo communication for a limited period of time to fetch the pending Qeo data. <br> 
   **Note**: Make sure to re-suspend Qeo again before the entire App gets suspended by the OS, otherwise Qeo will be residing in an undetermined state.
8. If the Event Reader receives a Chat Topic, the information (sender and message content) is retrieved and sent to the UI. Same thing happend when the State Reader receives a Participant Topic. 
9. If a user types a message in the text box and clicks **Send**, the name of the user and the text in the box are packed in a Chat Message Topic and sent over Qeo, where it will be received by all Event Readers subscribed on that Topic (including the state reader of the user who sent the message).
10. If a user modifies the state (available/busy/away/idle), the state and the name of the user are packed in a Participant Message Topic and send over Qeo, where it will be received by all State Readers subscribed on that Topic (including the state reader of the user who sent the message).

#### Initializing the Factory
A factory needs to be created in a background thread. This is shown (as an example) in the following code snippet:<br>

    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        self.factory = [[QEOFactory alloc] initWithError:nil];
        if (self.factory == nil){
            NSLog(@"Could not make factory");
            return;
        }
        // VoiP: Register the factory to the background notification server
        AppDelegate *appDelegate = (AppDelegate *)([UIApplication sharedApplication].delegate);
        self.factory.bgnsCallbackDelegate = appDelegate.qeoBackgroundServiceManager;
    });

#### Creating the Event Reader
The code snippet below shows how to create an Event Reader for the ChatMessage topic:<br>

    self.eventReader = [[QEOEventReader alloc] initWithType:[org_qeo_sample_simplechat_ChatMessage class]
                                                    factory:factory
                                                   delegate:self
                                             entityDelegate:nil
                                                      error:&error];
    if (nil == self.eventReader){
        dispatch_async(dispatch_get_main_queue(), ^{
            // Show Alert box
            self.alert = [[UIAlertView alloc] initWithTitle:@"Qeo Event Writer"
                                                    message:[error localizedDescription]
                                                   delegate:self
                                          cancelButtonTitle:@"Cancel"
                                          otherButtonTitles:nil];
            [self.alert show];
        });
        return;
    }

#### Creating the Event Writer
The code snippet below shows how to create an Event Writer for the ChatMessage topic:<br>

    self.eventWriter = [[QEOEventWriter alloc] initWithType:[org_qeo_sample_simplechat_ChatMessage class]
                                                    factory:factory
                                             entityDelegate:nil
                                                      error:&error];
    if (nil == self.eventWriter){
        dispatch_async(dispatch_get_main_queue(), ^{
            // Show Alert box
            self.alert = [[UIAlertView alloc] initWithTitle:@"Qeo ChatParticipant Writer"
                                                    message:[error localizedDescription]
                                                   delegate:self
                                          cancelButtonTitle:@"Cancel"
                                          otherButtonTitles:nil];
            [self.alert show];
        });
        return;
    }

#### Creating the State Change Reader
The code snippet below shows how to create an State Change Reader for the Participant topic.<br>
It enables the Topic for Background Notifications: <br>

    self.participantReader = [[QEOStateChangeReader alloc] initWithType:[org_qeo_sample_simplechat_ChatParticipant class]
                                                                factory:factory
                                                               delegate:self
                                                         entityDelegate:nil
                                                                  error:&error];
    if (self.participantReader == nil) {
        if (UIApplicationStateBackground != [UIApplication sharedApplication].applicationState) {
            dispatch_async(dispatch_get_main_queue(), ^{
                // Show Alert box
                UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Qeo ChatParticipant Reader"
                                                                message:[error localizedDescription]
                                                               delegate:self
                                                      cancelButtonTitle:@"Cancel"
                                                      otherButtonTitles:nil];
                [alert show];
            });
        }
    }
    // VoiP: We are interested in notifications for this readers topic
    self.participantReader.backgroundServiceNotification = YES;

#### Creating the State Writer
The code snippet below shows how to create an State Writer for the Participant topic:

    self.participantWriter = [[QEOStateWriter alloc] initWithType:[org_qeo_sample_simplechat_ChatParticipant class]
                                                          factory:factory
                                                   entityDelegate:nil
                                                            error:&error];
    if (self.participantWriter == nil){
        dispatch_async(dispatch_get_main_queue(), ^{
            // Show Alert box
            self.alert = [[UIAlertView alloc] initWithTitle:@"Qeo ChatParticipant Writer"
                                                    message:[error localizedDescription]
                                                   delegate:self
                                          cancelButtonTitle:@"Cancel"
                                          otherButtonTitles:nil];
            [self.alert show];
        });
        return;
    }

<br>
<br>
<br>
<br>

## Actions for Foreground/Background Switchover
For VoIP a lot more logic is required to handle Foreground/Background Switchovers together with maintaining the connection with the Qeo Background Notification Server (BGNS). <br>
This sample App contains an example class "BackgroundServiceManager" on how this can be achieved. <br>
It implements a state machine to keep track of states (Foreground - background Qeo active - background Qeo inactive - background Keep Alive) and transitions (going to/from background, keep alive activated and BGNS notification received). <br>
The goal of this class is to manage the Qeo suspend/resume state together with the Qeo Background notifications.

#### Handling Qeo Foregorund/Background Switchover: The Background Service Manager
This is done in the BackgroundServiceManager.m. <br>
Here is the implemented state machine: <br>


                                        +--------------+ (didLaunchInBackground)
                                        |  Qeo active  | <------------ * (auto restart App)
                         ---------------| (background) |
                        |               +--------------+
                        |                   ^      |
                        |     (BGNS notif.) |      | (Timeout)
                        |                   |      |
      User              V                   |      V
    starts App    +------------+        +--------------+
        *-------> | Foreground | -----> | Qeo inactive |  
                  |            | <----- | (background) |  
                  +------------+        + -------------+        
                        ^                   ^      |
                        |                   |      | (OS trigger keep alive)
                        |         (Timeout) |      |
                        |                   |      V
                        |               +--------------+
                        |               |  Keep Alive  |
                         ---------------|    running   |
                                        | (background) |
                                        +--------------+



#### Initializing the Background Service Manager
Initialization is done in the AppDelegate.m. Below are code snippets showing this:

    - (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
    {
        //---------------------------------------------------------
        // callback handler in case BGNS notifies the App
        //---------------------------------------------------------
        typedef void (^onQeoDataHandler)(void);
        onQeoDataHandler onQeoData = ^{
            // Your App can do some specific tasks like:
            //  - create local notification
            //  - start ringing tone
            //  - ...
        };

        // Create BGNS Manager
        self.qeoBackgroundServiceManager =
            [[QeoBackgroundServiceManager alloc]initWithKeepAlivePollingTime:600         // Apple minimum = 600 sec
                                                                qeoAliveTime:590         // Max. 600 sec, take a small margin
                                                            onQeoDataHandler:onQeoData];

        return YES;
    }

    - (void)applicationDidEnterBackground:(UIApplication *)application
    {
        // Notify the BGNS Manager that the App is in background mode
        [self.qeoBackgroundServiceManager applicationDidEnterBackground:application];
    }

    - (void)applicationWillEnterForeground:(UIApplication *)application
    {
        // Notify the BGNS Manager that the App is coming to the foreground
        [self.qeoBackgroundServiceManager applicationWillEnterForeground:application];
    }

#### Returning to Foreground
When the App returns to foreground, it is advised to check whether the user has indicated he wants to reset Qeo credentials:

    // Called when resuming from background mode
    -(void)willContinue
    {
        // Check the user defaults, the user may have set the reset the Qeo identities in
        // the General settings of the App.
        NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
        [standardUserDefaults synchronize];

        if (YES == [standardUserDefaults boolForKey:@"reset_Qeo"]) {
            // reset
            [QEOIdentity clearQeoIdentities];

            // reset the flag in the user defaults
            [standardUserDefaults setObject:@"NO" forKey:@"reset_Qeo"];
            [standardUserDefaults synchronize];

            //Close readers/writers and factory
            _factory = nil;
            _eventReader = nil;
            _eventWriter = nil;
        }

        if ((nil == _factory) && (NO == self.qeoRequestStarted)) {
            // re-init Qeo
            [self setupQeoCommunication];
        }
    }


#### Removing Qeo Credentials
A check is also performed here. The *QEOIdentity* object has a class called *clearQeoIdentities*. In the above code snippet, a value is read from the general settings of the App (included as a settings bundle). It checks if a user has requested to remove the Qeo credentials (by checking the value of the corresponding flag). If so, Qeo credentials are first remmoved, and the flag value is reset.

<br>
<br>
<br>
<br>

## User Instructions

#### Procedure
Run the App. When running, a chat window opens. Type your message and click **Send**. 

#### Qeo Credentials

If your device does not yet have Qeo credentials, you need to provide an OTC. Refer to the [Qeo Security Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+Security+User+Guide) for more information

<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>



<a name=QGaugeReader></a>
[Back to Top](#Top)
# QGaugeReader Sample Application Documentation

## About QGaugeReader

#### What is QGaugeReader

The QGauge App monitors the throughput of the network interfaces of different devices in your network in real time. It actually consists of two separate applications:

- QGauge Writer: which publishes network interface information
- QGauge Reader: which reads the information and shows it in a graph.



**Note:** The SDK contains only the Reader part for iOS. Writers are available for other platforms (Android, Linux, JavaScript). If no Writers are running, QGauge Reader will not register any throughput.


####Important Note

“Sample-qgaugeReader is supported currently to run on :

- All 32 bit ios physical devices(iphone/ipod) and simulators.
- All 64 bit ios physical devices(iphone/ipod)  in 32-bit mode.
- And not on 64 bit simulators.

In order to run it on 64-bit simulators, you have to recompile the CorePlot library in 64-bit activation mode and add this library again in sample-gaugeReader. At this point, this sample application does not run on iPad. Support for this device is under development. 


#### Location Of the Files ###

The files related to this application are located in the *QeoSDKHome/ios/samples/sample-qsimplechat* folder of the SDK.

#### More Information ###
For more information, refer to the Qeo iOS Developer Guide.

<br>
<br>
<br>
<br>


## How to Install and Run the Sample ##

#### Installation Procedure ###
Proceed as follows to install the sample:

1. Start Xcode
2. Open the QgaugeReader project
3. Note the *Qeo bundle* and *Qeo framework* references. These are necessary for a Qeo-enabled project. The Qeo Framework provides the ability to create and intialize a  Qeo Factory, which allows you to Readers and Writers. The Qeo Bundle provides the UI for authentication.

You can now build and run the sample on a connected device or the simulator.

<br>
<br>
<br>
<br>

## Application Details ##

#### General Information ###

For general information about Qeo entities, the Qeo Data Model (QDM) and the Code Generator, go here:


- [Qeo System Description](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+System+Description).
- [Introduction to Qeo Security](http://wiki.qeo-app-development.com/display/QeoCusDoc/Introduction+to+Qeo+Security).
- [QDM Developer Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+Data+Model+%28QDM%29+Developer+Guide).
- [Code Generator User Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Code+Generator+User+Guide).

Also, refer to the Qeo iOS Developer Guide.

#### About the QDM and the Code Generator ###

The QGauge Reader uses two data types:

- **Qgauge_Netstatmessage:** the topic containing information about a network interface. This is a State Topic
- **qeo_DeviceInfo:** the Topic containing the details about a Qeo device.

These topics are formalised in the Qeo Data Models (QDMs) for these topics, an XML-based format. These XML files are then translated into .h and .m files using the Code Generator, and included in the project.

<br>
<br>
<br>
<br>

## About QGRQeoHandler ##

#### General Process ###

The Qeo part of this sample application is handled in the QGRQeoHandler.What happens is this:

1. The class initializes the Qeo Factory. The Factory allows you to create a Reader  which will listen to the *netStatMessage* topics. This is done on a background thread in order not to block the App.
2. The Application initializes a State Reader and starts reading netStatMessage Topics.
3. The Application extracts the data from the netStatMessages and performs some calculations to prepare the data for th UI.
4. This data is passed to the UI and shown on the device screen.
5. All Qeo setup is done in the *setupQeoCommunication* class. Refer to the source files for the full code. Some code snippets are included below.

### Initializing the Factory ###
A factory needs to be created in a background thread. This is shown in the code snippet below:

    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        _factory = [[QEOFactory alloc] initWithError:nil];
        
        if (_factory == nil){
            NSLog(@"Could not make factory");
            return;
        }

#### Creating the State Reader ###
The code snippet below shows how to create a State Reader for the netStatMessage topic:

    _stateReader=[[QEOStateReader alloc]initWithType:[org_qeo_sample_gauge_NetStatMessage class]
                                             factory:_factory 
                                            delegate:self 
                                      entityDelegate:nil 
                                               error:nil];
        
    if(_stateReader==nil){
        NSLog(@"Could not make state Reader");
        return;
    } 

<br>
<br>
<br>
<br>

## Actions for Foreground/Background Switchover

#### Handling Qeo Foregorund/Background Switchover
This is done in the QGRAppDelegate.m. Below is are code snippets showing this:

    - (void)applicationDidEnterBackground:(UIApplication *)application
    {
        // Background mode: Put Qeo to deep sleep
        [QEOFactory suspendQeoCommunication];
    }

    - (void)applicationWillEnterForeground:(UIApplication *)application
    {
        // Foreground mode: Wakeup Qeo
        [QEOFactory resumeQeoCommunication];
    }

#### Registering to Foregorund/Background Notifications
This is done in the QGRAppDelegate.m. Below is a code snippet showing this:

    (void)applicationDidBecomeActive:(UIApplication *)application
    {
        // Restart any tasks that were paused (or not yet started) 
        // while the application was inactive. If the application was
        // previously in the background, optionally refresh the user 
        // interface.
        [[NSNotificationCenter defaultCenter] postNotificationName:@"resumeApp" 
                                                            object:nil 
                                                          userInfo:nil];
    }

#### Handling the Notification

The notification handling is done in QGRQEoHandler.m. Below is a code snippet showing this:

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(willResign:)
                                                 name:@"pauseApp" 
                                               object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(willContinue:)
                                                 name:@"resumeApp" 
                                               object:nil];



#### Returning to Foreground
When the App returns to foreground, there is a check to see if Qeo credentials are to be reset.

    // Resumes QEO communication if application is coming to foreground.
    -(void)willContinue:(NSNotification *)notification
    {
        NSLog(@"%s",__FUNCTION__);
    
        // Check the user defaults, the user may have set the reset the Qeo identities in
        // the General settings of the App.
        NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
        [standardUserDefaults synchronize];
    
        if (YES == [standardUserDefaults boolForKey:@"reset_Qeo"]) {
            // reset
            [QEOIdentity clearQeoIdentities];
        
            // reset the flag in the user defaults
            [standardUserDefaults setObject:@"NO" forKey:@"reset_Qeo"];
            [standardUserDefaults synchronize];
        
            //Close readers/writers  and factory
            _factory = nil;
            _stateReader = nil;
            _deviceInfoReader = nil;
        }
        if ((nil == _factory)&& (NO == self.qeoRequestStarted)) {
            // re-init Qeo
            [self setupQeoCommunication];
        } else if (nil != _factory){
            // Qeo was still running */
            dispatch_async(dispatch_get_main_queue(), ^{
            
                timer = [NSTimer scheduledTimerWithTimeInterval: 1.0
                                                        ctarget: self
                                                       selector:@selector(getQEOUpdate:)
                                                       userInfo: nil repeats:YES];
            
                NSRunLoop *runloop = [NSRunLoop currentRunLoop];
                [runloop addTimer:timer forMode:NSDefaultRunLoopMode];
            });
        }
    }

<br>
<br>
<br>
<br>

## User Instructions

#### Instructions
Run the App. When running, you will see a list of Qeo devices on your Realm. Tap one to see more details.

#### Qeo Authentication
If your device does not yet have Qeo credentials, you need to provide an OTC. Refer to the [Qeo Security Guide](http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+Security+User+Guide) for more information.

[Back to Top](#Top)
