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

import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.activities.WizardFragment;
import com.chaosinmotion.securechat.activities.WizardInterface;
import com.chaosinmotion.securechat.network.SCNetwork;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Set the passcode fragment
 */
public class OnboardingForgotPassword extends Fragment implements WizardFragment
{
	private WizardInterface wizardInterface;
	private EditText username;
	private Button resetButton;

	public OnboardingForgotPassword()
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
		resetButton = (Button)getView().findViewById(R.id.resetPassword);
		resetButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				doResetPassword();
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_onboarding_forgot_password, container, false);
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
	}

	private void doResetPassword()
	{
		try {
			JSONObject d = new JSONObject();
			d.put("username",username.getText().toString());
			SCNetwork.get().request("login/forgotpassword", d, this, new SCNetwork.ResponseInterface()
			{
				@Override
				public void responseResult(SCNetwork.Response response)
				{
					if (response.isSuccess()) {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder.setMessage(R.string.reset_password_message);
						builder.setTitle(R.string.reset_password_title);
						builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								wizardInterface.goBack();
							}
						});
						builder.show();
					}
				}
			});
		}
		catch (JSONException ex) {
		}
	}

	@Override
	public void doNext()
	{
	}

	@Override
	public int getTitleResourceID()
	{
		return R.string.onboarding_title_forgot_password;
	}

	public boolean showNext()
	{
		return false;
	}
}
