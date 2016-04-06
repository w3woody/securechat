//
//  AppDelegate.m
//  SecureChat
//
//  Created by William Woody on 2/17/16.
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

#import "AppDelegate.h"
#import "SCRSAManager.h"
#import "SCWait.h"
#import "SCNetworkCredentials.h"
#import "SCKeychain.h"
#import "SCNetworkResponse.h"
#import "SCLoginViewController.h"
#import "SCMessageQueue.h"

@interface AppDelegate ()
@property (strong) UIStoryboard *mainStoryboard;
@property (strong) UIViewController *viewController;
@property (assign) BOOL alertPresenting;
@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
	self.mainStoryboard = [UIStoryboard storyboardWithName:@"Main" bundle:nil];
	self.viewController = self.mainStoryboard.instantiateInitialViewController;
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    self.window.backgroundColor = [UIColor whiteColor];
	self.window.rootViewController = self.viewController;
    [self.window makeKeyAndVisible];

	/*
	 *	Attach this to the network
	 */

	[[SCNetwork shared] setDelegate:self];

	/*
	 *	Run startup tests
	 */

//	[[StartupTest shared] runTests];

	/*
	 *	Determine if we have an RSA key and account information, and if we
	 *	don't open the onboarding storyboard
	 */

	if ([self needsOnboarding]) {
		/*
		 *	We don't have secure data, so run onboarding sequence
		 */

		UIStoryboard *onboarding = [UIStoryboard storyboardWithName:@"Onboarding" bundle:nil];
		UIViewController *root = onboarding.instantiateInitialViewController;
		[self.viewController presentViewController:root animated:YES completion:nil];
	}

	return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application
{
	// Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
	// Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
	[[SCMessageQueue shared] stopQueue];
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
	// Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
	// If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
	// Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
	// Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
	[[SCMessageQueue shared] startQueue];
}

- (void)applicationWillTerminate:(UIApplication *)application
{
	// Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

/**
 *	Returns YES if we need onboarding. This happens when there is no data
 *	stored in our secure store.
 */

- (BOOL)needsOnboarding
{
	if (SCHasSecureData()) return NO;
	return YES;
}

#pragma mark - Network Delegate

- (void)startWaitSpinner
{
	[[SCWait shared] wait];
	[[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:YES];
}

- (void)stopWaitSpinner
{
	[[SCWait shared] stopWait];
	[[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
}

/*
 *	Standard error handler
 */

- (void)showServerError:(SCNetworkResponse *)response
{
	NSString *title;
	NSString *message;

	if (self.alertPresenting) return;
	self.alertPresenting = YES;

	if (response.serverCode != 200) {
		title = NSLocalizedString(@"Server Error", @"title");
		message = NSLocalizedString(@"There was a problem contacting the remote server", @"message");
	} else {
		switch (response.error) {
			case 1:
				title = @"Server Exception";
				message = NSLocalizedString(@"An unexpected problem was reported by the remote server", @"message");
				break;
			case 2:
				title = @"Login Error";
				message = NSLocalizedString(@"The user credentials provided are incorrect", @"message");
				break;
			case 3:
				title = @"Internal Error";
				message = NSLocalizedString(@"An unexpected problem was reported by the remote server", @"message");
				break;
			case 4:
				title = @"Not authorized";
				message = NSLocalizedString(@"You are currently not authorized to perform this operation", @"message");
				break;
			case 5:
				title = @"Username already exists";
				message = NSLocalizedString(@"The username you selected is already taken; please pick another", @"message");
				break;

			case 6:
				title = @"Unknown device";
				message = NSLocalizedString(@"Unable to find this device in the server", @"message");
				break;

			case 7:
				// Don't alert user; just run.
				return;
//				title = @"Notification service unknown";
//				message = NSLocalizedString(@"The notification service is not running", @"message");
//				break;

			case 8:
				title = @"Unknown user";
				message = NSLocalizedString(@"The user you provided is unknown. Please check and try again.", @"message");
				break;

			default:
				title = @"Unknown error";
				message = NSLocalizedString(@"An unknown server error was reported", @"message");
				break;
		}
	}

	UIAlertView *alert = [[UIAlertView alloc] initWithTitle:title message:message delegate:self cancelButtonTitle:NSLocalizedString(@"OK",@"OK") otherButtonTitles: nil];
	[alert show];
}

- (void)alertView:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex
{
	self.alertPresenting = NO;
}

- (SCNetworkCredentials *)credentials
{
	SCNetworkCredentials *creds = [[SCNetworkCredentials alloc] init];
	creds.username = [[SCRSAManager shared] username];
	creds.password = [[SCRSAManager shared] passwordHash];
	return creds;
}

- (void)requestLoginDialog:(void (^)(BOOL success))callback
{
	UIStoryboard *onboarding = [UIStoryboard storyboardWithName:@"LoginScreen" bundle:nil];
	UINavigationController *root = onboarding.instantiateInitialViewController;

	SCLoginViewController *lvc = (SCLoginViewController *)root.topViewController;
	lvc.callback = callback;

	[self.viewController presentViewController:root animated:YES completion:nil];
}


@end
