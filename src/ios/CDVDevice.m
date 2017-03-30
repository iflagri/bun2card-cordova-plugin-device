/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

#include <sys/types.h>
#include <sys/sysctl.h>
#include "TargetConditionals.h"

#import <Cordova/CDV.h>
#import "CDVDevice.h"

#define kCDVAppIdentifier @"CDVUUID"

@implementation UIDevice (ModelVersion)

- (NSString*)modelVersion
{
    size_t size;

    sysctlbyname("hw.machine", NULL, &size, NULL, 0);
    char* machine = malloc(size);
    sysctlbyname("hw.machine", machine, &size, NULL, 0);
    NSString* platform = [NSString stringWithUTF8String:machine];
    free(machine);

    return platform;
}

@end

@interface CDVDevice () {}
@end

@implementation CDVDevice

- (NSString*)uniqueAppInstanceIdentifier:(UIDevice*)device
{
    NSUserDefaults* userDefaults = [NSUserDefaults standardUserDefaults];
    
    // Check user defaults first to maintain backwards compaitibility with previous versions
    // which didn't user identifierForVendor
    NSString* app_uuid = [userDefaults stringForKey:kCDVAppIdentifier];
    if (app_uuid == nil) {
        if ([device respondsToSelector:@selector(identifierForVendor)]) {
            app_uuid = [[device identifierForVendor] UUIDString];
        } else {
            CFUUIDRef uuid = CFUUIDCreate(NULL);
            app_uuid = (__bridge_transfer NSString *)CFUUIDCreateString(NULL, uuid);
            CFRelease(uuid);
        }

        [userDefaults setObject:app_uuid forKey:kCDVAppIdentifier];
        [userDefaults synchronize];
    }
    
    return app_uuid;
}

- (void)getDeviceInfo:(CDVInvokedUrlCommand*)command
{
    NSString *newUuid = [command argumentAtIndex:0];
    NSString *uuid = [self uniqueAppInstanceIdentifier:[UIDevice currentDevice]];
    
    if (newUuid.length && ![newUuid isEqualToString:uuid]) {
        [self setUuidWithString:newUuid];
    }
    
    NSDictionary* deviceProperties = [self deviceProperties];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:deviceProperties];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setUuid:(CDVInvokedUrlCommand*)command
{
    NSString *uuid = [command argumentAtIndex:0];
    if (![self setUuidWithString:uuid]) {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No argument uuid"];
        [self.commandDelegate sendPluginResult:result  callbackId:command.callbackId];
        return;
    }
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:uuid];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (BOOL) setUuidWithString :(NSString *) newUuid {
    if (!newUuid.length || newUuid.length < 10) {
        return NO;
    }
    NSUserDefaults* userDefaults = [NSUserDefaults standardUserDefaults];
    [userDefaults setObject:newUuid forKey:kCDVAppIdentifier];
    [userDefaults synchronize];
    return YES;
}

- (NSDictionary*)deviceProperties
{
    UIDevice* device = [UIDevice currentDevice];

    return @{
             @"manufacturer": @"Apple",
             @"model": [device modelVersion],
             @"platform": @"iOS",
             @"version": [device systemVersion],
             @"uuid": [self uniqueAppInstanceIdentifier:device],
             @"cordova": [[self class] cordovaVersion],
             @"isVirtual": @([self isVirtual])
             };
}


+ (NSString*)cordovaVersion
{
    return CDV_VERSION;
}

- (BOOL)isVirtual
{
    #if TARGET_OS_SIMULATOR
        return true;
    #elif TARGET_IPHONE_SIMULATOR
        return true;
    #else
        return false;
    #endif
}

@end
