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

package com.chaosinmotion.securechat.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.chaosinmotion.securechat.MainApplication;
import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.activities.WizardFragment;
import com.chaosinmotion.securechat.activities.WizardInterface;
import com.chaosinmotion.securechat.messages.SCMessageQueue;
import com.chaosinmotion.securechat.network.SCNetwork;
import com.chaosinmotion.securechat.network.SCNetworkCredentials;
import com.chaosinmotion.securechat.rsa.SCRSAManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Set the passcode fragment
 */
public class LoginAccount extends Fragment implements WizardFragment
{
	private WizardInterface wizardInterface;
	private EditText username;
	private EditText password;
	private Button login;
	private Button forgotPassword;
	private Button newAccount;

	public LoginAccount()
	{
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (getArguments() != null) {
			// TODO: load arguments
		}
	}

	@Override
	public void onActivityCreated(Bundle bundle)
	{
		super.onActivityCreated(bundle);

		username = (EditText)getView().findViewById(R.id.username);
		password = (EditText)getView().findViewById(R.id.password);

		login = (Button)getView().findViewById(R.id.login);
		login.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				doLogin();
			}
		});

		newAccount = (Button)getView().findViewById(R.id.newAccount);
		newAccount.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				doNewAccount();
			}
		});

		forgotPassword = (Button)getView().findViewById(R.id.forgotPassword);
		forgotPassword.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				doForgotPassword();
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_login, container, false);
	}

	@TargetApi(23)
	public void onActivity(Context context)
	{
		super.onAttach(context);
		if (!(context instanceof WizardInterface)) {
			throw new RuntimeException("Wizard activity must implement interface");
		}
		wizardInterface = (WizardInterface)context;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		if (!(activity instanceof WizardInterface)) {
			throw new RuntimeException("Wizard activity must implement interface");
		}
		wizardInterface = (WizardInterface)activity;
	}

	@Override
	public void onDetach()
	{
		super.onDetach();
		wizardInterface = null;
		username = null;
		password = null;
		forgotPassword = null;
	}

	private void doForgotPassword()
	{
		// Reuse the fragment from the onboarding sequence
		wizardInterface.transitionToFragment(new OnboardingForgotPassword());
	}

	private void doNewAccount()
	{
		wizardInterface.transitionToFragment(new LoginNewAccount());
	}

	@Override
	public void doNext()
	{
	}

	private void displayError()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.login_error_message);
		builder.setTitle(R.string.login_error_title);
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Ignore
			}
		});
		builder.show();
		return;
	}

	public void doLogin()
	{
		String uname = username.getText().toString();
		String pwd = password.getText().toString();
		final SCNetworkCredentials creds = new SCNetworkCredentials(uname);
		creds.setPasswordFromClearText(pwd);

		/*
		 *	Here's the trick: we do not allow the user to log in with a
		 *	separate account. We don't have the mechanism in place to switch
		 *	devices to a new account, since our model has a device per
		 *	account. So instead, we locally match to make sure our login
		 *	username matches what we have stored; if it doesn't we bail
		 *	after a short delay. The delay is so as not to give away the
		 *	fact that the user picked the wrong username.
		 */

		if (!creds.getUsername().equals(SCRSAManager.shared().getUsername())) {
			displayError();
			return;
		}

		/*
		 *  Now validate the credentials
		 */
		SCNetwork.get().doLogin(creds, new SCNetwork.LoginCallback()
		{
			@Override
			public void loginResult(int reason)
			{
				if (reason == SCNetwork.LOGIN_SUCCESS) {
					/*
					 *  Login success. Register device
					 */

					SCRSAManager.shared().setCredentials(creds.getUsername(), creds.getPassword());
					SCRSAManager.shared().encodeSecureData(getActivity());

					/*
					 *  Now dismiss
					 */

					getActivity().finish();
					MainApplication.loginResult(true);
				} else {
					/*
					 *  Problem.
					 */

					displayError();
				}
			}
		});
	}

	@Override
	public int getTitleResourceID()
	{
		return R.string.login_title;
	}

	@Override
	public boolean showNext()
	{
		return false;
	}
}
