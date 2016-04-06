#	schema1.sql
#
#		Create the database schema for SecureServer. Note that we don't need
#	to bootstrap an admin password because we don't have an admin function.
#	This is a deliberate design decision; our goal is to remove all admin
#	functionality; users manage their own accounts.

#-------------------------------------------------------------------------------#
#																				#
#	User Database																#
#																				#
#-------------------------------------------------------------------------------#

#	Users
#
#		User database. This is simply a table of users (by username), the
#	password (which is hashed using SHA256) and IDs. No other identifying
#	information is tracked with users.

CREATE TABLE Users (
	userid serial not null primary key,
	username text unique not null,
	password varchar(128) not null
);

CREATE INDEX UsersIX1 on Users ( username );

#-------------------------------------------------------------------------------#
#																				#
#	Forgot password	tracking													#
#																				#
#-------------------------------------------------------------------------------#

CREATE TABLE ForgotPassword (
	recordid serial not null primary key,
	userid int not null,
	token varchar(256) not null unique,
	expires timestamp without time zone not null
);

CREATE INDEX ForgotPasswordIX1 on ForgotPassword ( token );


#-------------------------------------------------------------------------------#
#																				#
#	Device Database																#
#																				#
#-------------------------------------------------------------------------------#

#	Devices
#
#		Each user has one or more devices. This tracks the UUID of each
#	announced device, the public key each device advertised, and which user
#	the device belongs to

CREATE TABLE Devices (
	deviceid serial not null primary key,
	userid int not null,
	deviceuuid text not null,
	publickey text not null
);

CREATE INDEX DevicesIX1 on Devices ( userid );
CREATE INDEX DevicesIX2 on Devices ( deviceuuid );


#-------------------------------------------------------------------------------#
#																				#
#	Message Queue																#
#																				#
#-------------------------------------------------------------------------------#

#	Messages
#
#		Represents each message stored in the system. Note that each message
#	is associated with a specific device, and we flush the messages for a
#	device once the device calls and receives its messages. Each message in
#	the queue is encrypted using an RSA public key; we have no way to 
#	decrypt these messages.
#
#		Each message receives a timestamp when we receive the message, which
#	is sent with the encrypted message
#
#		Note that our protocol encrypts the binary message as a text string,
#	so we store a text string here.
#
#		We also note (in the toflag field) if this message was sent from
#	the senderid, or sent *TO* the senderid. This allows us to reconstruct
#	the complete message thread on every device associated with a user: if
#	A sends a message to B, A sends the message to every device registered
#	to B as a sender, and to every device registered to A as the receiver.
#	Thus, A can poll for messages and see the one he sent, even devices that
#	did not participate in the conversation
#
#		Note that for messages sent from A to B, the message stored in the
#	queue for the device owned by A shows 'B' in the senderid, and 'toflag'
#	set to 'true', indicating the message was sent 'to' the senderid from
#	the owner of this device.

CREATE TABLE Messages (
	messageid serial not null primary key,
	deviceid int not null,
	senderid int not null,
	toflag boolean not null,
	received timestamp without time zone not null,
	checksum text not null,
	message bytea not null
);

CREATE INDEX MessagesIX1 on Messages ( deviceid );
CREATE INDEX MessagesIX2 on Messages ( senderid );

