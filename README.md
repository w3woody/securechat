# SecureChat

A small scale demonstration of a secure chat system using a public key architecture that prevents eavesdropping.

SecureChat is a secure chat application which is designed to minimize the opportunity for third party agencies (such as hackers or governmental agencies) from being able to obtain chat history or chat messages from a compromised server.

This is done through a design which does the following:

* Encrypts all messages sent to the server using a public key/private key architecture.
* Removes all messages from the sever as they are read by a client.
* Captures no personally identifying information, either with account setup or with device registration.

This also attempts to secure the iOS chat client via the following techniques:

* Uses a weak checksum to verify the correct device code is entered, giving a 1 in 256 chance the wrong password will destructively load the internal private key.
* Stores all messages on the device encrypted; thus, simply copying the data from the device without the private key is useless.
* Assumes the RSA algorithms on the device have been compromised by providing a custom implementation of RSA key encryption.
* Assumes the keystore on the device has been compromised by requiring a separate passcode to decode internal data.

**Please note:** I have absolutely no reason to believe that the Apple keychain or encryption services have been compromised, and I am of the very strong opinion that Apple has best security practices in mind.

The purpose of this exercise is to assume _what if?_, and to provide a demonstration that it is possible for an individual, only equipped with a couple of books and some Wikipedia articles, to implement a secure chat system, and that this level of security is not the province of large corporations or nation-states.

Also note that I am not providing an implementation of this system for public use. That is, I am not running a publicly available SecureChat server, nor am I providing the iOS app on the App Store. Instead, this is intended to be a demonstration of how an individual can provide a secure chat system by providing a sample implementation.

## License

    SecureChat: A secure chat system which permits secure communications 
    between iOS devices and a back-end server.

    Copyright © 2016 by William Edward Woody

    This program is free software: you can redistribute it and/or modify 
    it under the terms of the GNU General Public License as published by 
    the Free Software Foundation, either version 3 of the License, or 
    (at your option) any later version.

    This program is distributed in the hope that it will be useful, but 
    WITHOUT ANY WARRANTY; without even the implied warranty of 
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
    General Public License for more details.

    You should have received a copy of the GNU General Public License 
    along with this program. If not, see http://www.gnu.org/licenses/

    Contact information:

    William Edward Woody
    12605 Raven Ridge Rd
    Raleigh, NC 27614
    United States of America
    woody@alumni.caltech.edu
    
## Full Documentation

Full documentation for this project is contained in the subfolder SecureChat_docs, which provides more detailed information in HTML. Download and open the index.html file in a browser to review the documentation set.

Beyond publishing this software as open source, the documentation serves as an overview of how the software works. If you wish, you can compare the documentation here with the implementation of the client and server components to verify that the components act as they should. You are also welcome to examine the design in order to poke holes; I'm aware of a couple (which are described in their sections), and you may be able to find more on your own.

## Why?

For me, this boils down to the [Apple v FBI](http://www.wired.com/2016/02/apples-fbi-battle-is-complicated-heres-whats-really-going-on/) clash, and more importantly, to the public rhetoric which surrounds that case.

Over the past few weeks I've seen pundits and Presidential candidates opine that Apple was being traitorous, that Apple was being un-American, that Apple was out of line and needs to cooperate with the FBI regardless of the national or international ramifications.

Worse, I've seen pundits completely mis-represent how encryption works (confusing it with a related concept of [Access Control](https://en.wikipedia.org/wiki/Access_control_list)), or stating that while they personally believed that their own information should not be exposed to hackers, the FBI must have access to every last byte on a suspect's device, regardless of how Apple has to engineer the operating system to provide that access. And if that means our personal private data is exposed to hackers or to other nation-states, then we'll "deal with that" when we get there.

And, more to the point, I've seen pundits theorize that only large corporations and nation-states are capable of putting together secure end-to-end communications systems--and so regulating them and forcing them to comply with law enforcement would have few implications.

Sorry, no.

First, encryption is no longer the province of wealthy corporations and nation-states. Books such as [Applied Cryptography](https://www.schneier.com/books/applied_cryptography/) and [Handbook of Applied Cryptography](http://cacr.uwaterloo.ca/hac/) (the two reference books I used to implement the public key algorithm used here), along with articles on Wikipedia make it easy for any reasonably competent software developer with a little experience to assemble a system such as this.

Second, encryption is not a pixie dust which is sprinkled over an application to make it more secure. It is a way of thinking about the design of a system from end to end which makes it more secure. Encryption, in other words, is a way to think about the design of an application.

Think of your system as the plumbing of a house: pipes running throughout carrying water to a series of sinks and toilets and showers and household appliances. Encryption is not like tape which can be wrapped around a leak to keep it from leaking. Security is a way to think about the entire plumbing, to make sure that all of the joints are tight and well installed, that certain pipes slant downhill (so sewer lines don't clog), that the right piping materials are used so you don't create future problems (such as tying in copper with steel without an insulating buffer). In that light, encryption is a tool, like plumber's tape, that helps with the overall design.

And what the FBI wants is for Apple to poke holes in the piping in order to gain unprecedented access to communications that, just 30 years ago, was ephemeral and temporary in nature--and thus unavailable to agencies like the FBI.

Now this wouldn't be so bad, if it weren't for the fact that for every terrorist there are hundreds of oppressed peoples around the world (such as LGBT activists and political activists seeking greater freedom for themselves) who rely on secure communications in order to coordinate their activities or simply to live a more fulfilling life with less fear of oppression by authorities [who make execution of homosexuals a public spectacle.](http://www.theguardian.com/world/2011/sep/07/iran-executes-men-homosexuality-charges)

So the idea that we should weaken this system of security, exposing millions of people to greater oppression around the world by setting [a dangerous precedent,](http://www.apple.com/customer-letter/) in order to slightly simplify the law enforcement efforts of the FBI at home--what the f...?

And it's not even that many terrorists care about secure systems. The attackers in Paris, for example, [used unsecure SMS messages](https://www.techdirt.com/articles/20151118/08474732854/after-endless-demonization-encryption-police-find-paris-attackers-coordinated-via-unencrypted-sms.shtml%3E) to coordinate their attacks. The very lightening nature of coordinated terrorists attacks combined with the cell-like structure of terrorist organizations means that encryption is unnecessary for them to achieve their goals.

### Code is Speech.

That's why I did this. Because code is speech, and nothing works better to demonstrate the strengths and weaknesses of encryption and secure chat systems than to build a system as a demonstration that such a system can be built by a motivated individual using nothing but publicly available sources.

There are no public secure chat systems. I am running a private server for test purposes, but it has no traffic outside of my verifying the system works, and is located on a local network without exposure to the outside world.

I am certainly willing to work with any agency or corporation as a private consultant; if you wish to contact me shoot me an e-mail at woody@alumni.caltech.edu. And this is only a demonstration project, as it exists it is not capable of being extended to a large system, though it certainly could with some effort.