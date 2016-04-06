#	coreschema.sql
#
#		Generate the various core database entries needed on startup

CREATE TABLE DBVersion (
	version int not null unique
);
