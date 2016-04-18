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

package com.chaosinmotion.securechat.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.messages.SCMessageQueue;
import com.chaosinmotion.securechat.network.SCNetwork;
import com.chaosinmotion.securechat.rsa.SCRSAManager;

/**
 * The passcode activity is brought up to help the user log in.
 * Created by woody on 4/17/16.
 */
public class PasscodeActivity extends AppCompatActivity
{
	private EditText passcode;
	private Button login;
	private Button setupNew;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		login = (Button)findViewById(R.id.loginAccount);
		login.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				doLogin();
			}
		});

		setupNew = (Button)findViewById(R.id.createAccount);
		setupNew.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v)
			{
				doCreateAccount();
			}
		});

		passcode = (EditText)findViewById(R.id.passcode);
	}

	private void doLogin()
	{
		String code = passcode.getText().toString();
		if (SCRSAManager.shared().setPasscode(code,this)) {
			/*
			 *	At this point we have a valid passcode, which means we've just
			 *	loaded the contents. Set up networking for network requests
			 */

			String server = SCRSAManager.shared().getServer();
			SCNetwork.get().setServerPrefix(server);

			SCMessageQueue.get().startQueue(this);

			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
			finish();
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.incorrect_passcode_message);
			builder.setTitle(R.string.incorrect_passcode_title);
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
				}
			});
			builder.show();
		}
	}

	private void doCreateAccount()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.new_device_message);
		builder.setTitle(R.string.new_device_title);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				/*
				 *  Clear the secure store and drop into the onboarding
				 *  pathway.
				 */

				SCMessageQueue.get().stopQueue();
				SCMessageQueue.get().clearQueue(PasscodeActivity.this);
				SCRSAManager.shared().clear(PasscodeActivity.this);

				Intent intent = new Intent(PasscodeActivity.this, OnboardingActivity.class);
				startActivity(intent);
				finish();
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
			}
		});
		builder.show();
	}
}
