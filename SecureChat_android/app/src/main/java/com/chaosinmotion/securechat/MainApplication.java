/*
 * Copyright (c) 2016. William Edward Woody
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>
 *
 */

package com.chaosinmotion.securechat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.chaosinmotion.securechat.activities.LoginActivity;
import com.chaosinmotion.securechat.messages.SCMessageQueue;
import com.chaosinmotion.securechat.network.SCNetwork;
import com.chaosinmotion.securechat.network.SCNetworkCredentials;
import com.chaosinmotion.securechat.rsa.SCRSAManager;

import java.util.Stack;

/**
 * Notes: first, this is the central code for handling requests and
 * responses. We also tract activity lifestyle in order to find the
 * topmost of our activities to present errors. (We assume the topmost
 * is the last one which went active.) We also track the count of activities
 * in order to turn our messaging queue off.
 *
 * Created by woody on 4/16/16.
 */
public class MainApplication extends Application implements SCNetwork.Delegate,
		Application.ActivityLifecycleCallbacks
{
	private Stack<Activity> activities;
	private int activityCount;
	private static SCNetwork.LoginResponse loginCallback;

	@Override
	public void onCreate()
	{
		super.onCreate();

		activities = new Stack<Activity>();
		registerActivityLifecycleCallbacks(this);
		SCNetwork.get().setNetworkDelegate(this);
	}

	/*
	 *  From network delegate
	 */
	@Override
	public void showServerError(SCNetwork.Response response)
	{
		if (activities.empty()) return;     // don't show if noone is active
		Activity a = activities.peek();

		int title,message;
		switch (response.getError()) {
			case 1:
				title = R.string.server_exception_title;
				message = R.string.server_exception_message;
				break;
			case 2:
				title = R.string.login_error_title;
				message = R.string.login_error_message;
				break;
			case 3:
				title = R.string.internal_error_title;
				message = R.string.internal_error_message;
				break;
			case 4:
				title = R.string.not_authorized_title;
				message = R.string.not_authorized_message;
				break;
			case 5:
				title = R.string.username_already_exists_title;
				message = R.string.username_already_exists_message;
				break;
			case 6:
				title = R.string.unknown_device_title;
				message = R.string.unknown_device_message;
				break;
			case 7:
				// Don't alert user; just return.
				return;
			case 8:
				title = R.string.unknown_user_title;
				message = R.string.unknown_user_message;
				break;
			default:
				title = R.string.unknown_error_title;
				message = R.string.unknown_error_message;
				break;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setMessage(message);
		builder.setTitle(title);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
			}
		});
		builder.show();
	}

	@Override
	public SCNetworkCredentials credentials()
	{
		return new SCNetworkCredentials(SCRSAManager.shared().getUsername(),
				SCRSAManager.shared().getPasswordHash());
	}

	@Override
	public void requestLoginDialog(final SCNetwork.LoginResponse response)
	{
		if (activities.empty()) {
			response.didLogin(false);
			return;     // don't show if noone is active
		}
		Activity a = activities.peek();
		loginCallback = response;

		Intent intent = new Intent(a, LoginActivity.class);
		a.startActivity(intent);
	}

	public static void loginResult(boolean result)
	{
		loginCallback.didLogin(result);
	}

	/*
	 *  From activity lifestyle callbacks
	 */
	@Override
	public void onActivityCreated(Activity activity, Bundle savedInstanceState)
	{
		if (0 == activityCount++) {
			SCMessageQueue.get().startQueue(this);
		}
	}

	@Override
	public void onActivityStarted(Activity activity)
	{
	}

	@Override
	public void onActivityResumed(Activity activity)
	{
		activities.push(activity);
	}

	@Override
	public void onActivityPaused(Activity activity)
	{
		activities.remove(activity);
	}

	@Override
	public void onActivityStopped(Activity activity)
	{
	}

	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle outState)
	{
	}

	@Override
	public void onActivityDestroyed(Activity activity)
	{
		if (0 == --activityCount) {
			SCMessageQueue.get().stopQueue();
		}
	}
}
