//
//  SCOutputStream.m
//  SecureChat
//
//  Created by William Woody on 3/10/16.
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

#import "SCOutputStream.h"
#import "SCChecksum.h"

// https://developer.apple.com/library/ios/documentation/Cocoa/Conceptual/Streams/Articles/WritingOutputStreams.html#//apple_ref/doc/uid/20002274-BAJCABBC

#define MAXBUFFER	1024

@interface SCOutputStream ()
@property (strong) NSMutableArray<NSData *> *buffers;
@property (assign) NSUInteger pos;
@property (strong) NSOutputStream *outStream;
@property (assign) BOOL readyToWrite;
@end

/**
 *	Wraps NSOutputStream to provide checksum and encapsulation services
 *	for our protocol. Note we use the runloop version of the APIs in order
 *	to verify if we're trying to write data faster than we can pump data
 *	out the pipe, we don't block.
 */

@implementation SCOutputStream

- (id)initWithOutputStream:(NSOutputStream *)outputStream
{
	if (nil != (self = [super init])) {
		self.outStream = outputStream;
		self.buffers = [[NSMutableArray alloc] init];

		[self.outStream setDelegate:self];
	}
	return self;
}

- (void)open
{
	[self.outStream scheduleInRunLoop:[NSRunLoop mainRunLoop] forMode:NSDefaultRunLoopMode];
	[self.outStream open];
}

- (void)stream:(NSStream *)aStream handleEvent:(NSStreamEvent)eventCode
{
	NSError *err;

	switch (eventCode) {
		case NSStreamEventHasSpaceAvailable:
			if ([self.buffers count] == 0) {
				self.readyToWrite = YES;
			} else {
				[self internalWrite];
			}
			break;

		case NSStreamEventErrorOccurred:
			err = [aStream streamError];
			NSLog(@"Error %d: %@",(int)err.code,err.localizedDescription);

			[self.outStream close];
			// Falls through to

		default:
			if (self.eventCallback) {
				self.eventCallback(eventCode);
			}
			break;
	}
}

/*
 *	Internal method for writing data. Called when the stream is empty;
 *	writes as much as we can.
 */

- (void)internalWrite
{
	NSData *data = self.buffers[0];
	size_t dsize = data.length;
	const uint8_t *buffer = (const uint8_t *)data.bytes;
	size_t wrote = [self.outStream write:buffer+self.pos maxLength:dsize-self.pos];
	self.pos = self.pos + wrote;
	if (self.pos >= dsize) {
		// Finished writing buffer. Remove from queue
		self.pos = 0;
		[self.buffers removeObjectAtIndex:0];
	}
}

/*
 *	Write buffer. This calculates a checksum and writes the data with
 *	padding rules so that byte 0 is the end of the packet.
 */

- (void)writeData:(NSData *)data
{
	size_t size = data.length;
	const uint8_t *buf = (uint8_t *)data.bytes;

	uint8_t checksum = SCCalcCRC8(0, buf, size);

	/*
	 *	Calculate the size of the data buffer this will translate to
	 *
	 *	This uses the byte sequence 0x01 0x01 to represent 0, and 0x01
	 *	0x02 to represent 1. 0x00 represents the end of a packet.
	 */

	size_t nsize = 1;		// eom
	nsize += (checksum < 2) ? 2 : 1;		// deal with checksum == 0 or 1
	for (size_t i = 0; i < size; ++i) {
		if (buf[i] < 2) nsize += 2;
		else nsize++;
	}

	NSMutableData *transData = [[NSMutableData alloc] initWithLength:nsize];
	uint8_t *dst = (uint8_t *)transData.mutableBytes;

	size_t ptr = 0;
	for (size_t i = 0; i < size; ++i) {
		if (buf[i] < 2) {
			dst[ptr++] = 1;
			dst[ptr++] = buf[i]+1;
		} else {
			dst[ptr++] = buf[i];
		}
	}
	if (checksum < 2) {
		dst[ptr++] = 1;
		dst[ptr++] = checksum + 1;
	} else {
		dst[ptr++] = checksum;
	}
	dst[ptr++] = 0;

	/*
	 *	Append data to the end of the data objects to write
	 */

	[self.buffers addObject:[transData copy]];

	/*
	 *	Write the data
	 */

	if (self.readyToWrite) {
		[self internalWrite];
	}
}

- (void)close
{
	[self.outStream close];
}

@end
