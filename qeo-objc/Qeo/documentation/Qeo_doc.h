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

#import <Foundation/Foundation.h>

#pragma mark - QEOType
/** Qeo type, base type for all the generated QDM code */
@interface QEOType : NSObject
// All generated code form the QDM's will inherit from this interface

/** Creates a numerical identification of the Qeo type. <br>
 The hash value is calculated based on the contnets of the key fields only. <br>
 If the Qeo type does not have any key fields defined, then all tags will be used in the hashing calculation.
 
 @return hash value
 */
- (NSUInteger)hash;

/** Verifies if the Qeo type is equal to a given one. <br>
 Equality is based on the contents of the key fields only. <br>
 If the Qeo type does not have any key fields defined, then all tags will be used in the comparison
 
 @param other Qeo type to compare with
 @return true if equal, false otherwise
 */
- (BOOL)isEqual:(id)other;

/** Create a readable desciption about the content of the Qeo type.
 
 @return Returns a string describing the content of the Qeo type
 */
- (NSString *)description;
@end

#pragma mark - QEOFactory
/** Qeo Identity, identifying the Qeo Realm */
@interface QEOIdentity : NSObject

/** Unique identifier of a Realm */
@property (readonly, copy, nonatomic) NSNumber *realmId;
/** Unique identifier of a Device */
@property (readonly, copy, nonatomic) NSNumber *deviceId;
/** Unique identifier of a User */
@property (readonly, copy, nonatomic) NSNumber *userId;
/** Registration URL */
@property (readonly, strong, nonatomic)  NSURL *url;

///-----------------------------
/// @name Initializers
///-----------------------------
/** Creates an empty QeoIdentity that can be used for open domain registration */
- (instancetype) init;

///-----------------------------
/// @name Identity manipulators
///-----------------------------
/** Retrieves all available Qeo Identities
 
 @return Returns an array of usable QEO Identities. The first one is the default Realm
 */
+ (NSArray *) retrieveQeoIdentities;


/** Removes current Qeo Identities */
+ (void)clearQeoIdentities;
@end


/** Factory Context Delegate protocol
 
 The QEOFactoryContextDelegate protocol declares action methods that a class can perform during the registration process. The context object implementing this protocol is provided in each interface of the QEOFactoryDelegate registration callback methods.
 
 */
@protocol QEOFactoryContextDelegate <NSObject>

/** Perform an actual registration with the provided Qeo credentials
 
 @param otc One time activation code with a short expiration time
 @param url Url of the SMS server to which the otc is validated
 @param error Specifies the error when something went wrong
 @return BOOL YES:success, NO:error occured
 */
- (BOOL)performRegisterWithOtc:(NSString *)otc
                           url:(NSURL *)url
                         error:(NSError **)error;

/** Indication for performing a registration remotely
 
 @param name Public name under which the App is visible in the public domain
 @param timeout Maximum time in seconds before a remote registration is cancelled
 @return BOOL YES:success, NO: could not start request
 */
- (BOOL)requestRemoteRegistrationAs:(NSString *)name
                            timeout:(NSNumber *)timeout;

/** Confirm or reject a remote registrations
 
 When a remote registration is performed, the App is still required to confirm or reject the actual registration.
 
 @param accept YES:accept, NO:reject
 */
- (void)remoteRegistrationConfirmation:(BOOL)accept;

/** Cancels a registration, even for a remote one */
- (void)cancelRegistration;

@end


/** Factory Delegate protocol
 
 The QEOFactoryDelegate protocol declares methods, by which a class can be notified to perform an action during the registration process.
 
 */
@protocol QEOFactoryDelegate <NSObject>

/** A notification in the registration process to express the need for Qeo credentials.
 
 This notification mechanism enables the support for custom registration dialogs and remote registrations.<br> 
 The provided factory context must be used to stear the registration process in the desired direction.
 
 @param context Factory context to be used to stear the ongoing registration
 */
- (void)registrationParametersNeeded:(id<QEOFactoryContextDelegate>)context;

@optional
/** A notification in the remote registration process to express the need for Qeo credential verification.

 This notification mechanism enables the support for Qeo credential verification of a remote registration process.
 The App can accept or deny the ongoning registration via the provided factory context.
 
 @param context Factory context to be used to stear the ongoing registration
 @param realmName Qeo Realm name to which the App will be registered
 @param url Url of the SMS server to which the otc will be validated
 */
- (void)remoteRegistrationConfirmationNeeded:(id<QEOFactoryContextDelegate>)context
                                   realmName:(NSString *)realmName
                                         url:(NSURL *)url;

@end

/** Background Notification Service Delegate protocol
 
 The QEOBackgroundNotificationServiceDelegate protocol declares a method, by which a class can be notified about pending Qeo data on the Background Notification Server when Qeo is in suspended mode. The registered class can take necessary actions like fetching Qeo data and/or generating a local notification to inform the user.
 
 */
@protocol QEOBackgroundNotificationServiceDelegate <NSObject>

/** A trigger from the Background Notification Server to indicate that some Qeo data is pending.
 
  On receival of a notification, the App will be woken from suspended state by the OS and it can fetch the pending data from the Background Notification Server (BGNS) if needed. Fetching of pending data is done by resuming Qeo from suspended state. <br>
 
    [QEOFactory resumeQeoCommunication];
 
  When the App is in background mode, Qeo needs to be suspend again before the App is moved to suspended mode by the OS. <br>

    [QEOFactory suspendQeoCommunication];
 
 @param readers All Qeo reader entities that support the Qeo type for which the Notification was send by the Background Notification Server
 */
- (void)qeoDataAvailableNotificationReceivedForEntities:(NSArray *)readers;
@end

/** Qeo Factory encapsulates your application's connection to a given Qeo Realm.
 
 The Qeo Factory sets up the connection to your Qeo Realm during its allocation. <br>
 The first time a Factory is allocated, you will be prompted to provide Qeo credentials. <br>
 This request is dependant on the initializer being used. The default behaviour is a modal view delivered with the framework to enter your Qeo credentials (OTC/URL).<br>
 For the initializer with a delegate, the registration process can be controlled in an interactive way. The App can provide its own set of registration dialogs or can ask for a remote registration. <br>
 Once the provided/remote credentials are verified, the associated certificates are stored in the App sandbox. <br>
 When the Qeo Factory is re-created at a later point in time, it will reuse the existing credentials.<br>
 This object also provides the ability to clean up previously stored credentials.<br>
 
 **Note**: This object is required for the allocation of Qeo readers/writers on the desired topics.<br>
 
 #Object Management <br>
 At this moment only foreground applications are supported. There is no communication while in the background. 
 
 #State Preservation and Restoration <br>
 States are preserved automatically when you move from foreground to background or vice versa. There is no need to close the factory or readers/writers when you go to the background.
 */
@interface QEOFactory : NSObject
/** Qeo Identity (of the Realm) that this factory is using */
@property (readonly, copy, nonatomic) QEOIdentity *identity;
/** Delegate which handles the Background service notification callbacks */
@property (nonatomic, weak) id<QEOBackgroundNotificationServiceDelegate> bgnsCallbackDelegate;

///-----------------------------
/// @name Initializers
///-----------------------------
/** Initialise a factory for the default realm. <br>
 When no default credentials are found it presents a modal view to enter credential information (OTC/URL).<br>
 
 This method is synchronous and blocks the main UI thread. To avoid deadlock, you need to allocate the Qeo Factory on a background thread (e.g. using the GCD).<br>
 
    QEFactory *factory;
    dispatch_async(dispatch_get_global_queue( DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
        factory = [[QEFactory alloc] init];
        // ...
    });
 
 @return QEOFactor object or nil if an error occured
 */
- (instancetype)init;

/** Initialise a factory for the default Realm. <br>
 When no default credentials are found it will present a modal view to enter credential information (OTC/URL).<br>
 
 This method is synchronous and blocks the main UI thread. You need to allocate the Qeo Factory on a background thread (e.g. using the GCD).<br>
 
    QEFactory *factory;
    dispatch_async(dispatch_get_global_queue( DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
        NSError* error = nil;
        factory = [[QEFactory alloc] initWithError:&error];
        // ...
    });
 
 @param error Specifies the error when something went wrong
 @return QEOFactor object or nil if an error occured
 */
- (instancetype)initWithError:(NSError **)error;

/** Initialise a factory for a given Realm Identity. <br>
 
 This method is synchronous and blocks the main UI thread. You need to allocate the Qeo Factory on a background thread (e.g. using the GCD).<br>
 
    QEFactory *factory;
    dispatch_async(dispatch_get_global_queue( DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
        // Open domain
        QEOIdentity* myRealmIdentity = nil;
        NSError* error = nil;
        factory = [[QEFactory alloc] initWithQeoIdentity:myRealmIdentity
        error:&error];
        // ...
    });
 
 @param identity Qeo Identity that the factory will be using. An empty QEOIdentity means using the open domain. 
 object forces a new registration process for a Realm.
 @param error Specifies the error when something went wrong
 @return QEOFactory object or nil if an error occured
 */
- (instancetype)initWithQeoIdentity:(QEOIdentity *)identity
                    error:(NSError **)error;

/** Initialise a factory for the default Realm. <br>
 When no default credentials are found: <br>
 - it will call on the delegate to provide credentials or to start remote registration <br>
 - if the delegate is nil, it will present a modal view to enter credential information (OTC/URL).<br>
 
 This method is synchronous and blocks the main UI thread. You need to allocate the Qeo Factory on a background thread (e.g. using the GCD).<br>
 
    QEFactory *factory;
    __weak myClass *me = self;
    dispatch_async(dispatch_get_global_queue( DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
        NSError* error = nil;
        factory = [[QEFactory alloc] initWithFactoryDelegate:me error:&error];
        // ...
    });
 
 @param delegate Delegate which handles the registration callbacks
 @param error Specifies the error when something went wrong
 @return QEOFactor object or nil if an error occured
 */
- (instancetype)initWithFactoryDelegate:(id <QEOFactoryDelegate>) delegate
                                  error:(NSError **)error;

/** Suspend Qeo communication
 
 Suspending Qeo means that all communication and socket connections are stopped.
 However in a special case one TCP socket can be kept alive to handle background notifications. <br>
 There are 3 mandatory requirements to make this possible: <br>
 - The App must have the VoIP feature as background mode set in its plist file. <br>
 - There must be at least one QEOStateReader or one QEOStateChangeReader enabled for background notifications. <br>
 - There must be a Background Notification Server available to connect to <br>
 
 */
+ (void)suspendQeoCommunication;

/** Resume all Qeo communication */
+ (void)resumeQeoCommunication;
@end


#pragma mark - QEOEntity
/** Qeo Policy Identity
 
 Unique identification of a user within Qeo.
 */
@interface QEOPolicyIdentity : NSObject
/** User ID of the user that you want to allow/disallow access. */
@property (readonly, copy, nonatomic) NSNumber *userId;
@end

@class QEOEntity;

/** Entity Delegate protocol
 
 The QEOEntityDelegate protocol declares a method that a class must implement to set access rights for policy identities (Readers and or Writers).
 */
@protocol QEOEntityDelegate <NSObject>

@optional
/** Delegate method for allowing access to a policy identity. The entity is a Reader/Writer. <br>
 
 The method is called for every participant identity that was granted access to the entity at the server side. <br>
 This delegate method can give or deny further access on a one-by-one basis to the entity for a subset of those participant identities. The list is ended with a nil identity.<br>
 
 **Note**: This feature is only relevant for fine grained policy access. In case of coarse grained policy access it will be called only for the nil identity.
 
 @param entity Qeo Reader or Writer to be allowed
 @param identity Qeo policy identity that uniquelly identifies the user
 @return Returns if the given entity is allowed or not for the given identity
 */
- (BOOL)allowAccessForEntity:(QEOEntity *)entity
                    identity:(QEOPolicyIdentity *)identity;
@end

/** Qeo Entity
 
 Base class for Qeo Readers and Writers */
@interface QEOEntity : NSObject
/** The QEOType this entity is using */
@property (readonly, strong, nonatomic) Class qeoType;
/** The Factory this entity it is attached to */
@property (readonly, strong, nonatomic) QEOFactory *factory;
/** Delegate for access right management */
@property (nonatomic, weak) id <QEOEntityDelegate> entityDelegate;
/** Method to trigger an access update of the policy 
 
 @param error Specifies the error when something went wrong
 @return Indicates if update was successful or not
 */
- (BOOL)updatePolicyWithError:(NSError **)error;
@end

#pragma mark - QEOEventReader
@class QEOEventReader;

/** Event Reader Delegate protocol
 
 The QEOEventReaderDelegate protocol declares two methods that a class must implement to handle event notifications.
 */
@protocol QEOEventReaderDelegate <NSObject>
@optional

///-----------------------------
/// @name Event handlers
///-----------------------------
/** Method will be called when a new event value has been published on the Topic.
 
 @param event published value on the Topic.
 @param eventReader Reference to the Event Reader, which received the notification.
 */
- (void)didReceiveEvent:(QEOType *)event
              forReader:(QEOEventReader *)eventReader;

/** Method will be called when there are no more immediate pending event notifications
 
 This notification is sent after a series of event notifications. This allows an App to accumulate incoming notifications and then trigger the UI to update the changes when this notification  is received.
 
 **Note**: This notification is also send in case there was only a single event notification.
 
 @param reader Reference to the Event Reader, which received the notification.
 */
- (void)didFinishBurstForEventReader:(QEOEventReader *)reader;
@end


/** Event Reader
 
 An Event Reader is created for a specific Qeo Topic and is used for consuming event information from a Qeo Topic.<br>
 Event Data does not have any key fields. This means that they are not instances on the Topic. All events are considered as one flat stream.
 */
@interface QEOEventReader : QEOEntity
/** Delegate that will handel event notifications */
@property (nonatomic, weak) id <QEOEventReaderDelegate> delegate;

///-----------------------------
/// @name Initializer
///-----------------------------
/** Default initialiser that will create an Event Reader
 
 The Event Reader is allocated for a specific Realm identified by a Factory instance and for a specific Topic identified by the Topic's Data type.
 
 @param qeoType Topic's Data type (QDM generated by the code generator)
 @param factory The Qeo factory (identifies the Realm)
 @param delegate Delegate which handles the event notifications
 @param entityDelegate (optional) Delegate to handle policy change events
 @param error Specifies (optional) Specifies the error when something went wrong
 @return QEOEventReader object or nil if an error occured
 */
- (instancetype)initWithType:(Class)qeoType
                     factory:(QEOFactory *)factory
                    delegate:(id <QEOEventReaderDelegate>)delegate
              entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                       error:(NSError **)error;
@end

#pragma mark - QEOEventWriter

/** Event Writer
 
 An Event Writer is created for a specific Qeo Topic and is used for publishing event information on a Qeo Topic.<br>
 Event Data does not have any key fields, this means that they are not instances on the Topic. All publications are considered as one flat stream of events.
 */
@interface QEOEventWriter : QEOEntity

///-----------------------------
/// @name Initializer
///-----------------------------
/** Default initialiser that will create an Event Writer
 
 The Event Writer is allocated for a specific Realm identified by a factory instance and for a specific Topic identified by the Topic's Data type.
 
 @param qeoType Topic's Data type (QDM generated by the code generator)
 @param factory The Qeo factory (identifies the Realm)
 @param entityDelegate (optional) Delegate to handle policy change events
 @param error Specifies (optional) Specifies the error when something went wrong
 @return QEOEventWriter object or nil if an error occured
 */
- (instancetype)initWithType:(Class)qeoType
                     factory:(QEOFactory *)factory
              entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                       error:(NSError **)error;

///-----------------------------
/// @name Event manipulators
///-----------------------------
/** Writes an event sample on the Topic.
 
 @param object Object containing the state information of a Qeo Topic to be created or updated
 @param error Specifies the error when something went wrong
 @return Returns if the write action was successful or not
 */
- (BOOL)write:(QEOType *)object
    withError:(NSError **)error;
@end

#pragma mark - QEOStateReader
@class QEOStateReader;

/** Qeo State Reader Delegate Protocol
 
 The QEOStateReaderDelegate protocol declares one method that a class must implement to handle state notifications.
 */
@protocol QEOStateReaderDelegate <NSObject>
@optional

///-----------------------------
/// @name State handlers
///-----------------------------
/** Method that will receive notifications when a state has updated for this state reader
 
 **Note**: This notification offers no details on the exact nature of the change.
 The App must use one of the enumeration methods of the State Reader to retrieve the individual state notifications.
 
 @param stateReader Reference to the State Change Reader, which received the notification.
 */
- (void)didReceiveUpdateForStateReader:(QEOStateReader *)stateReader;
@end

/** Qeo State Change Reader
 
 A State Reader is created for a specific Qeo Topic and is used for consuming state information on its Topic.<br>
 It builds a local cache that holds the latest state for all instances on the Topic. <br>
 The local cache can be queried by the App whenever it needs information on the topic. <br>
 Querying is done in the form of iteration on the State Reader. Two types of iterations are supported: <br>
 - Fast enumeration <br>
 - Enumeration using blocks
 
 */
@interface QEOStateReader : QEOEntity <NSFastEnumeration>
/** Delegate will handle state update notifications */
@property (nonatomic, weak) id <QEOStateReaderDelegate> delegate;

///-----------------------------
/// @name Initializer
///-----------------------------
/** Default initialiser that will create a State Reader
 
 The State Reader is allocated for a specific Realm identified by a factory instance and for a specific Topic identified by the Topic's Data type.
 
 @param qeoType Topic's Data type (QDM generated by the code generator)
 @param factory The Qeo factory (identifies the Realm)
 @param delegate Delegate which handles the state update notifications
 @param entityDelegate (optional) Delegate to handle policy change events
 @param error Specifies (optional) Specifies the error when something went wrong
 @return QEOStateReader object or nil if an error occured
 */
- (instancetype)initWithType:(Class)qeoType
                     factory:(QEOFactory *)factory
                    delegate:(id <QEOStateReaderDelegate>)delegate
              entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                       error:(NSError **)error;

/** Enables or disables the Reader object for background notifications.
 
 VoIP enabled Apps can make use of the Qeo Background Notification Server (BGNS).<br>
 This method enables/disables the ability of getting BGNS notifications in suspended state.<br>
 A notification indicates that the BGNS has received data that this reader is interested in.<br>
 
 **Note**: <br>
 - If the App is not a Voip App, the OS will not keep the connection open to the BGNS when the App goes to the background. The TCP connection will be force killed by the OS and Qeo will be in an undetermined state. <br>
 - If there is no Background Notification Server available, then there will be no TCP connection and the App will not be woken up in background. In such a case this method will have no effect.
 
 @param enable Enables/disables the background notification feature for the reader
 @param error Specifies (optional) the error when something went wrong
 */
- (BOOL)backgroundServiceNotification:(BOOL)enable
                                error:(NSError **)error;

///------------------------------
/// @name enumeration with blocks
///------------------------------
/** Enumeration using blocks
 
 This method will iterate over all instances present in the State Reader and will invoke the provided function Block per found instance.<br>
 Iteration will continue till the end of the available instances or untill you set the BOOL output parameter to NO.
 
    [stateReader enumerateInstancesUsingBlock:^void (const QEOType *qeoType, BOOL *cont) {
        const Wall *w = (const Wall *)qeoType;
        NSLog(@"reader: %@", w.description);
    }];
 
 @param iterationBlock The provided block will be called on a per sample basis
 */
- (void)enumerateInstancesUsingBlock:(void (^)(const QEOType *, BOOL *))iterationBlock;
@end

#pragma mark - QEOStateChangeReader
@class QEOStateChangeReader;

/** Qeo State Change Reader Delegate Protocol
 
 The QEOStateChangeReaderDelegate protocol declares the three methods that a class must implement to handle state transitions.
 */
@protocol QEOStateChangeReaderDelegate <NSObject>
@optional

///----------------------------------
/// @name State change event handlers
///----------------------------------
/** Method will be called when a new value has been published on the Topic.
 
 The trigger can be the creation of a new instance or an update of an existing instance.
 
 @param state published value on the Topic.
 @param stateChangeReader Reference to the State Change Reader, which received the notification.
 */
- (void)didReceiveStateChange:(QEOType *)state
                    forReader:(QEOStateChangeReader *)stateChangeReader;

/** Method will be called when there are no more immediate pending state change or removal notifications
 
 This notification is sent after a series of state change or removal notifications. <br>
 This allows an App to accumulate incoming notifications and then trigger the UI to update the changes when this notification  is received.
 
 **Note**: This notification is also send in case there was only one state change or remove notification.
 
 @param stateChangeReader Reference to the State Change Reader, which received the notification.
 */
- (void)didFinishBurstForStateChangeReader:(QEOStateChangeReader *)stateChangeReader;

/** Method will be called when a state is removed from a Topic
 
 **Note**: Only the key fields of the state object have valid values. All other fields should be ignored.
 
 @param state Removed value of the Topic.
 @param stateChangeReader Reference to the State Change Reader, which received the notification.
 */
- (void)didReceiveStateRemoval:(QEOType *)state
                     forReader:(QEOStateChangeReader *)stateChangeReader;
@end

/** Qeo State Change Reader
 
 A State Change Reader is created for a specific Qeo Topic and is used for consuming state information on its Topic. It offers a notications-only interface, which allows Apps to treat state transitions as events. <br>
 
 **NOTE**: There is no guarentee that the App will be notified on every single state transition.
 If state transitions occur rapidly, Qeo may aggregate them and notify the App only once.
 
 */
@interface QEOStateChangeReader : QEOEntity
/** Delegate that will handel state change updates notifications */
@property (nonatomic, weak) id <QEOStateChangeReaderDelegate> delegate;

///-----------------------------
/// @name Initializer
///-----------------------------
/** Default initialiser that will create a State Change Reader
 
 The State Change Reader is allocated for a specific Realm identified by a Factory instance and for a specific Topic identified by the Topic's Data type.
 
 @param qeoType Topic's Data type (QDM generated by the Code Generator)
 @param factory The Qeo Factory (identifies the Realm)
 @param delegate Delegate to  handle state change notifications
 @param entityDelegate (optional) Delegate to handle policy change events
 @param error Specifies (optional) Specifies the error when something went wrong
 @return QEOStateChangeReader object or nil if an error occured
 */
- (instancetype)initWithType:(Class)qeoType
                     factory:(QEOFactory *)factory
                     delegate:(id <QEOStateChangeReaderDelegate>) delegate
               entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                        error:(NSError **)error;

/** Enables or disables the Reader object for background notifications.
 
 VoIP enabled Apps can make use of the Qeo Background Notification Server (BGNS).<br>
 This method enables/disables the ability of getting BGNS notifications in suspended state.<br>
 A notification indicates that the BGNS has received data that this reader is interested in.<br>
 
 **Note**: <br>
 - If the App is not a Voip App, the OS will not keep the connection open to the BGNS when the App goes to the background. The TCP connection will be force killed by the OS and Qeo will be in an undetermined state. <br>
 - If there is no Background Notification Server available, then there will be no TCP connection and the App will not be woken up in background. In such a case this method will have no effect.
 
 @param enable Enables/disables the background notification feature for the reader
 @param error Specifies (optional) the error when something went wrong
 */
- (BOOL)backgroundServiceNotification:(BOOL)enable
                                error:(NSError **)error;
@end

#pragma mark - QEOStateWriter
/** Qeo State Writer
 
 A State Writer is created for a specific Qeo Topic and is used for publishing state information on a Qeo Topic.
 
 */
@interface QEOStateWriter : QEOEntity

///-----------------------------
/// @name Initializer
///-----------------------------
/** Default initialiser for a State Writer
 
 The State Writer is allocated for a specific Realm identified by a Factory instance and for a specific Topic identified by the Topic's Data type.
 
 @param qeoType Topic's Data type (QDM generated by the code generator)
 @param factory The Qeo factory (identifies the Realm)
 @param entityDelegate (optional) Delegate to handle policy change events
 @param error Specifies (optional) Specifies the error when something went wrong
 @return QEOStateWriter object or nil if an error occured
 */
- (instancetype)initWithType:(Class)qeoType
              factory:(QEOFactory *)factory
               entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                        error:(NSError **)error;

///-----------------------------
/// @name State manipulators
///-----------------------------
/** Writes an instance to a Qeo Topic
 
 This method writes data values to a Topic. It makes no distinction between creating and updating samples.<br>
 Samples are identified by its Key fields, if for the given set of keys no value was written in the past it will be treated as a create operation otherwise it will be handled as an update.
 
 @param object Object containing the state information of a Qeo Topic to be created or updated
 @param error Specifies the error when something went wrong
 @return Returns if the write action was successful or not
 */
- (BOOL)write:(QEOType *)object
    withError:(NSError **)error;

/** Removes an instance from a Qeo Topic.
 
 This method removes samples from a Qeo Topic that were written in the past.<br>
 The remove operation takes the Key fields of the sample into account as identification.<br>
 All other field values of the sample are ignored.<br>
 
 @param object Object containing the state information of a Qeo Topic to be remove
 @param error (optional) Specifies the error when something went wrong
 @return Returns if the remove action was successful or not
 */
- (BOOL)remove:(QEOType *)object
     withError:(NSError **)error;
@end
