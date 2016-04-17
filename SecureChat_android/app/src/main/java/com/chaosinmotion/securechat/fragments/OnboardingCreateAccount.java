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
import android.widget.EditText;

import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.activities.WizardFragment;
import com.chaosinmotion.securechat.activities.WizardInterface;
import com.chaosinmotion.securechat.messages.SCMessageQueue;
import com.chaosinmotion.securechat.network.SCNetwork;
import com.chaosinmotion.securechat.network.SCNetworkCredentials;
import com.chaosinmotion.securechat.rsa.SCRSAManager;
import com.chaosinmotion.securechat.utils.PasswordComplexity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Set the passcode fragment
 */
public class OnboardingCreateAccount extends Fragment implements WizardFragment
{
	private WizardInterface wizardInterface;
	private EditText username;
	private EditText password;

	public OnboardingCreateAccount()
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
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_onboarding_create_account, container, false);
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
	}


	@Override
	public void doNext()
	{
		String uname = username.getText().toString();
		String pwd = password.getText().toString();
		if (!PasswordComplexity.complexityTest(pwd)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.weak_password_message);
			builder.setTitle(R.string.weak_password_title);
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					// Ignore
				}
			});
			builder.show();
		}

		final SCNetworkCredentials creds = new SCNetworkCredentials(uname);
		creds.setPasswordFromClearText(pwd);

		try {
			JSONObject json = new JSONObject();
			json.put("username", creds.getUsername());
			json.put("password", creds.getPassword());
			json.put("deviceid", SCRSAManager.shared().getDeviceUUID());
			json.put("pubkey", SCRSAManager.shared().getPublicKey());

			SCNetwork.get().request("login/createaccount", json, this, new SCNetwork.ResponseInterface()
			{
				@Override
				public void responseResult(SCNetwork.Response response)
				{
					if (response.isSuccess()) {
						/*
						 *  Success. Save the username and password,
						 *  and save the whole thing to the back end.
						 */
						SCRSAManager.shared().setCredentials(creds.getUsername(),creds.getPassword());
						SCRSAManager.shared().encodeSecureData(getActivity());

						/*
						 *  We have what we need to start the queue
						 */
						SCMessageQueue.get().startQueue(getActivity());

						/*
						 *  Done. Go to the next page
						 */

						wizardInterface.transitionToFragment(new OnboardingFinished());
					}
				}
			});
		}
		catch (JSONException ex) {
		}
	}

	@Override
	public int getTitleResourceID()
	{
		return R.string.onboarding_title_create;
	}

	@Override
	public boolean showNext()
	{
		return true;
	}
}
