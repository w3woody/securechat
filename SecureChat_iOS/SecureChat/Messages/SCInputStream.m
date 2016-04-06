//
//  SCInputStream.m
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

// https://www.bing.com/search?q=CFStreamCreatePairWithSocketToCFHost&form=APMCS1
// CFStreamCreatePairWithSocketToCFHost
// https://developer.apple.com/library/ios/documentation/Cocoa/Conceptual/Streams/Articles/NetworkStreams.html

#import "SCInputStream.h"
#import "SCChecksum.h"

#define MAXSIZE		4096


@interface SCInputStream ()
{
	uint8_t buffer[MAXSIZE];
	int bufpos;
	int buflen;
}

@property (strong) NSInputStream *inStream;

@end

/**
 *	Wraps the NSINputStream to provide parsing, and provides a standard
 *	callback for handling incoming events. Note that this code is written
 *	in a blocking fashion; it is assumed this input stream will be
 *	run in a background thread rather than run on the main thread, as
 *	the read code will block
 */

@implementation SCInputStream

- (id)initWithInputStream:(NSInputStream *)inputStream
{
	if (nil != (self = [super init])) {
		self.inStream = inputStream;

		bufpos = 0;
		buflen = 0;
	}
	return self;
}

- (int)nextByte
{
	if (bufpos >= buflen) {
		bufpos = 0;
		buflen = (int)[self.inStream read:buffer maxLength:MAXSIZE];
		if (buflen == -1) return -1;			// end of stream
	}
	
	return buffer[bufpos++];
}

- (void)processStream
{
	uint8_t lastByte = 0;
	BOOL startFlag = NO;
	BOOL nextFlag = NO;
	NSMutableData *data = [[NSMutableData alloc] initWithCapacity:MAXSIZE];

	/*
	 *	Run the stream, identifying 0x01 and 0x00. 
	 */
	for (;;) {
		int b = [self nextByte];
		if (b == -1) return;		// end of stream.
		
		if (b == 0) {
			/*
			 * Dump the processed buffer. We never escape 0.
			 */

			[self processBuffer:[data copy] withChecksum:lastByte];
			[data replaceBytesInRange:NSMakeRange(0, data.length) withBytes:nil length:0];
			nextFlag = NO;
			startFlag = NO;
			lastByte = 0;
			
		} else if (nextFlag) {
			if (startFlag) {
				[data appendBytes:&lastByte length:sizeof(lastByte)];
			} else {
				startFlag = YES;
			}
			lastByte = (uint8_t)(b - 1);
			nextFlag = NO;
			
		} else if (b == 1) {
			nextFlag = YES;
			
		} else {
			if (startFlag) {
				[data appendBytes:&lastByte length:sizeof(lastByte)];
			} else {
				startFlag = YES;
			}
			lastByte = (uint8_t)b;
		}
	}
}

- (void)processBuffer:(NSData *)data withChecksum:(uint8_t)checksum
{
	if (checksum == SCCalcCRC8(0, data.bytes, data.length)) {
		if (self.processPacket) {
			dispatch_async(dispatch_get_main_queue(), ^{
				self.processPacket(data);
			});
		}
	}
}

/**
 *	Close underlying stream
 */

- (void)close
{
	[self.inStream close];
}

@end
