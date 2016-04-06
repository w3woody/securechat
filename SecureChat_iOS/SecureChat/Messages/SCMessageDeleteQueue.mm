//
//  SCMessageDeleteQueue.m
//  SecureChat
//
//  Created by William Woody on 3/21/16.
//  Copyright © 2016 by William Edward Woody.
//

/*	SecureChat: A secure chat system which permits secure communications 
 *  between iOS devices and a back-end server.
 *
 *	Copyright © 2016 by William Edward Woody
 *
 *	This program is free software: you can redistribute it and/or modify it 
 *	under the terms of the GNU General Public License as published by the 
 *	Free Software Foundation, either version 3 of the License, or (at your 
 *	option) any later version.
 *
 *	This program is distributed in the hope that it will be useful, but 
 *	WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *	or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *	for more details.
 *
 *	You should have received a copy of the GNU General Public License along 
 *	with this program. If not, see <http://www.gnu.org/licenses/>
 */

#import "SCMessageDeleteQueue.h"
#import "SCMessageDeleteQueueItem.h"
#import "SCRSAManager.h"
#import "SCNetwork.h"
#import "SCNetworkResponse.h"

#include "SCSecureHash.h"
#include <pthread.h>

@interface SCMessageDeleteQueue ()
{
	pthread_mutex_t decodeQueueMutex;
	pthread_cond_t decodeQueueCond;
	pthread_t decodeQueueThread;
	NSMutableArray<SCMessageDeleteQueueItem *> *decodeQueue;

	BOOL deleteRunning;
	NSMutableArray<SCMessageDeleteQueueItem *> *deleteQueue;
}

- (void)decodeProc;
@end

static void *DecodeThread(void *ref)
{
	SCMessageDeleteQueue *pSelf = (__bridge SCMessageDeleteQueue *)ref;
	[pSelf decodeProc];
	return nil;
}

@implementation SCMessageDeleteQueue

+ (SCMessageDeleteQueue *)shared
{
	static SCMessageDeleteQueue *singleton;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		singleton = [[SCMessageDeleteQueue alloc] init];
	});
	return singleton;
}

- (id)init
{
	if (nil != (self = [super init])) {
		pthread_mutex_init(&decodeQueueMutex,NULL);
		pthread_cond_init(&decodeQueueCond,NULL);
		deleteQueue = [[NSMutableArray alloc] init];
		decodeQueue = [[NSMutableArray alloc] init];

		// Start threads
		pthread_attr_t attr;
		pthread_attr_init(&attr);
		pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
		pthread_create(&decodeQueueThread, &attr, DecodeThread, (__bridge void *)(self));
		pthread_attr_destroy(&attr);
	}
	return self;
}

- (void)deleteMessage:(NSInteger)messageIndex withData:(NSData *)data
{
	SCMessageDeleteQueueItem *item = [[SCMessageDeleteQueueItem alloc] init];
	item.messageID = messageIndex;
	item.message = data;

	// Enqueue onto our list and send notification
	pthread_mutex_lock(&decodeQueueMutex);

	[decodeQueue addObject:item];

	pthread_cond_signal(&decodeQueueCond);
	pthread_mutex_unlock(&decodeQueueMutex);
}

/*
 *	Should never be called given our singleton semantics
 */

- (void)dealloc
{
	pthread_cancel(decodeQueueThread);

	pthread_cond_destroy(&decodeQueueCond);
	pthread_mutex_destroy(&decodeQueueMutex);
}

/*
 *	decode: the method to handle decoding. Runs in a separate thread. This
 *	basically runs looking for messages added to our queue, then
 *	decrypts the message in the thread and adds to the delete queue
 */

- (void)decodeProc
{
	BOOL done = NO;
	SCMessageDeleteQueueItem *item;

	while (!done) {
		/*
		 *	Lock mutex and get status
		 */

		pthread_mutex_lock(&decodeQueueMutex);
		while (decodeQueue && ([decodeQueue count] == 0)) {
			int err = pthread_cond_wait(&decodeQueueCond, &decodeQueueMutex);
			NSLog(@"%d",err);
		}

		item = nil;
		if (decodeQueue == nil) {
			done = YES;
		} else if ([decodeQueue count] > 0) {
			item = [decodeQueue firstObject];
			[decodeQueue removeObjectAtIndex:0];
		}

		pthread_mutex_unlock(&decodeQueueMutex);

		/*
		 *	If we don't have an item (because we're done?) then continue
		 */

		if (item == nil) continue;

		/*
		 *	We have an item which needs a checksum calculated.
		 */

		NSData *cdata = [[SCRSAManager shared] decodeData:item.message];

		SCSHA256Context hasher;
		hasher.Start();
		hasher.Update(cdata.length, (const uint8_t *)cdata.bytes);
		uint8_t output[32];
		hasher.Finish(output);

		char buffer[80];
		for (int i = 0; i < 32; ++i) {
			sprintf(buffer + i*2,"%02x",output[i]);
		}
		item.checksum = [NSString stringWithUTF8String:buffer];
		item.message = nil;

		/*
		 *	Now enqueue onto delete queue
		 */

		dispatch_async(dispatch_get_main_queue(), ^{
			[self addDelete:item];
		});
	}
}

/*
 *	Delete item. Note addDelete and runDeleteCall all happen on the main
 *	thread.
 */

- (void)addDelete:(SCMessageDeleteQueueItem *)item
{
	[deleteQueue addObject:item];
	if (!deleteRunning) {
		[self runDeleteCall];
	}
}

- (void)runDeleteCall
{
	NSArray *del = [deleteQueue copy];
	[deleteQueue removeAllObjects];

	NSMutableArray *a = [[NSMutableArray alloc] init];
	for (SCMessageDeleteQueueItem *item in del) {
		NSDictionary *d = @{ @"messageid": @( item.messageID ),
							 @"checksum": item.checksum };
		[a addObject:d];
	}

	NSDictionary *params = @{ @"messages": a };
	deleteRunning = YES;

	[[SCNetwork shared] request:@"messages/dropmessages" withParameters:params backgroundRequest:YES caller:self response:^(SCNetworkResponse *response) {

		/*
		 *	I don't care if this succeeds or fails. Clear running state and
		 *	see if the list is empty or not. If it's not empty, something new
		 *	came along while the network request was in progress, so we
		 *	repeat until the list is empty.
		 */

		deleteRunning = NO;

		if ([deleteQueue count] > 0) {
			[self runDeleteCall];
		}
	}];
}

@end
