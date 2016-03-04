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
@interface QEOType : NSObject
// All generated code form the QDM's will inherit from this interface
- (NSUInteger)hash;
- (BOOL)isEqual:(id)other;
- (NSString *)description;
@end

#pragma mark - QEOFactory
@interface QEOIdentity : NSObject
@property (readonly, copy, nonatomic) NSNumber *realmId;
@property (readonly, copy, nonatomic) NSNumber *deviceId;
@property (readonly, copy, nonatomic) NSNumber *userId;
@property (readonly, strong, nonatomic)  NSURL *url;

// Creates an empty QeoIdentity that can be used for open domain registration
- (instancetype) init;
// Retuns an array of usable QEOIdentities. The first one is the default realm
+ (NSArray *)retrieveQeoIdentities;
+ (void)clearQeoIdentities;
@end

@protocol QEOFactoryContextDelegate <NSObject>
// Perform an actual registration with provided credentials
- (BOOL)performRegisterWithOtc:(NSString *)otc
                           url:(NSURL *)url
                         error:(NSError **)error;
// Indication for performing a registration remotely
- (BOOL)requestRemoteRegistrationAs:(NSString *)name
                            timeout:(NSNumber *)timeout;
// Accept/reject feature in case registration is done remotely
- (void)remoteRegistrationConfirmation:(BOOL)accept;
// Cancels an ongoing registration (including remote registration)
- (void)cancelRegistration;
@end

@protocol QEOFactoryDelegate <NSObject>
// A trigger that enables the support for custom registration dialogs or
// to enable remote registration through the factory context
- (void)registrationParametersNeeded:(id<QEOFactoryContextDelegate>)context;

@optional
// A trigger in case registration is performed remotely, it enables an accept/reject feature for it
- (void)remoteRegistrationConfirmationNeeded:(id<QEOFactoryContextDelegate>)context
                                   realmName:(NSString *)realmName
                                         url:(NSURL *)url;
@end

@protocol QEOBackgroundNotificationServiceDelegate <NSObject>
// A trigger from the Background Notification Server to indicate that some QEO data is pending.
// On receival of this trigger you can resume Qeo communication to fetch the pending data.
//   => [QEOFactory resumeQeoCommunication];
// Since we are still in the background, you will need to suspend the Qeo notification before the
// window of background processing gets closed
//   => [QEOFactory suspendQeoCommunication];
- (void)qeoDataAvailableNotificationReceivedForEntities:(NSArray *)readers;
@end

@interface QEOFactory : NSObject
// Identity that this factory is using
@property (readonly, copy, nonatomic) QEOIdentity *identity;
// Delegate which handles the Background service notification callbacks */
@property (nonatomic, weak) id<QEOBackgroundNotificationServiceDelegate> bgnsCallbackDelegate;

// Initialise a factory for the default realm. If there is none, register for one
- (instancetype)init;
// Same behavior as default initialiser, but returns a specific error when something goes wrong
- (instancetype)initWithError:(NSError **)error;
// An empty QEOIdentity will create a factory for the open domain
- (instancetype)initWithQeoIdentity:(QEOIdentity *)identity
                              error:(NSError **)error;
// Same behavior as the initialiser with error, but with extra callbacks to the delegate
// to allow a customized presentation of the registration process
- (instancetype)initWithFactoryDelegate:(id <QEOFactoryDelegate>) delegate
                                  error:(NSError **)error;

+ (void)suspendQeoCommunication;
+ (void)resumeQeoCommunication;
@end

#pragma mark - QEOEntity
@interface QEOPolicyIdentity : NSObject
// User ID of the user that you want to allow/disallow access to
@property (readonly, copy, nonatomic) NSNumber *userId;
@end

@class QEOEntity;
@protocol QEOEntityDelegate <NSObject>
@optional
// Delegate method for allowing access to a policy identity, the entity is a reader/writer
- (BOOL)allowAccessForEntity:(QEOEntity *)entity
                    identity:(QEOPolicyIdentity *)identity;
@end

// Abstract class that all readers and writers inherit from (was combined in nested protocols in the past)
@interface QEOEntity : NSObject
// The QEOType this entity is using
@property (readonly, strong, nonatomic) Class qeoType;
// The factory this entity is attached to
@property (readonly, strong, nonatomic) QEOFactory *factory;
// Delegate for access right management
@property (nonatomic, weak) id <QEOEntityDelegate> entityDelegate;
// Method to trigger a re-evaluation of the entity's policy
- (BOOL)updatePolicyWithError:(NSError **)error;
@end

#pragma mark - QEOEventReader
@class QEOEventReader;
@protocol QEOEventReaderDelegate <NSObject>
@optional
// Delegate method that will be called for every event received on an event reader
- (void)didReceiveEvent:(QEOType *)event
              forReader:(QEOEventReader *)eventReader;
// Could be used as trigger for the UI to refresh the event info when there is a burst of events received
- (void)didFinishBurstForEventReader:(QEOEventReader *)reader;
@end

@interface QEOEventReader : QEOEntity
// Delegate that will handle event updates
@property (nonatomic, weak) id <QEOEventReaderDelegate> delegate;
// Default initialiser that will create an event reader, entityDelegate and error are optional, rest is mandatory
- (instancetype)initWithType:(Class)qeoType
                     factory:(QEOFactory *)factory
                    delegate:(id <QEOEventReaderDelegate>)delegate
              entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                       error:(NSError **)error;
@end

#pragma mark - QEOEventWriter
@interface QEOEventWriter : QEOEntity
// Default initialiser that will create an event writer, entityDelegate and error are optional, rest is mandatory
- (instancetype)initWithType:(Class)qeoType
                     factory:(QEOFactory *)factory
              entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                       error:(NSError **)error;
// Writes an event sample on the event writer
- (BOOL)write:(QEOType *)object
    withError:(NSError **)error;
@end

#pragma mark - QEOStateReader
@class QEOStateReader;
@protocol QEOStateReaderDelegate <NSObject>
@optional
// Delegate method that will receive notifications when a state has updated for this state reader
- (void)didReceiveUpdateForStateReader:(QEOStateReader *)stateReader;
@end

@interface QEOStateReader : QEOEntity <NSFastEnumeration>
// Delegate that will handle state update events
@property (nonatomic, weak) id <QEOStateReaderDelegate> delegate;
// This property en/disables the ability of getting BGNS notifications in suspended state.
@property (nonatomic, readwrite) BOOL backgroundServiceNotification;
// Default initialiser that will create a state reader, entityDelegate and error are optional, rest is mandatory
- (instancetype)initWithType:(Class)qeoType
                     factory:(QEOFactory *)factory
                    delegate:(id <QEOStateReaderDelegate>)delegate
              entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                       error:(NSError **)error;

// Block function will iterate over all instances in the state reader as long as he returns YES
// Use BOOL as pointed argument
- (void)enumerateInstancesUsingBlock:(void (^)(const QEOType *, BOOL *))iterationBlock;
@end

#pragma mark - QEOStateChangeReader
@class QEOStateChangeReader;
@protocol QEOStateChangeReaderDelegate <NSObject>
@optional
// Delegate method that will be called when a state changes for a state change reader
- (void)didReceiveStateChange:(QEOType *)state
                    forReader:(QEOStateChangeReader *)stateChangeReader;
// Could be used to trigger the UI to update the changes when a burst of state updates is received
- (void)didFinishBurstForStateChangeReader:(QEOStateChangeReader *)stateChangeReader;
// Delegate method that will be called when a state is removed
- (void)didReceiveStateRemoval:(QEOType *)state
                     forReader:(QEOStateChangeReader *)stateChangeReader;
@end

@interface QEOStateChangeReader : QEOEntity
// Delegate that will handel state change updates
@property (nonatomic, weak) id <QEOStateChangeReaderDelegate> delegate;
// This property en/disables the ability of getting BGNS notifications in suspended state.
@property (nonatomic, readwrite) BOOL backgroundServiceNotification;
// Default initialiser for a state change reader, entityDelegate and error are optional, rest is mandatory
- (instancetype)initWithType:(Class)qeoType
                     factory:(QEOFactory *)factory
                    delegate:(id <QEOStateChangeReaderDelegate>) delegate
              entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                       error:(NSError **)error;
@end

#pragma mark - QEOStateWriter
@interface QEOStateWriter : QEOEntity
// Default initialiser for a state writer, entityDelegate and error are optional, rest is mandatory
- (instancetype)initWithType:(Class)qeoType
                     factory:(QEOFactory *)factory
              entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                       error:(NSError **)error;
// Method that will write an instance of a state on the state writer
- (BOOL)write:(QEOType *)object
    withError:(NSError **)error;
// Method that will remove a sample of a state that was written in the past
- (BOOL)remove:(QEOType *)object
     withError:(NSError **)error;
@end
