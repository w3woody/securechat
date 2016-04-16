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
import com.chaosinmotion.securechat.network.SCNetwork;
import com.chaosinmotion.securechat.rsa.SCRSAManager;

/**
 * Set the passcode fragment
 */
public class OnboardingSetServer extends Fragment implements WizardFragment
{
	private WizardInterface wizardInterface;
	private EditText server;

	public OnboardingSetServer()
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

		server = (EditText)getView().findViewById(R.id.server);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_onboarding_set_server, container, false);
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
		server = null;
	}

	private void enterServerError()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.enter_server_message);
		builder.setTitle(R.string.enter_server_title);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Ignore
			}
		});
		builder.show();
	}

	private void displayServerError()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.error_server_message);
		builder.setTitle(R.string.error_server_title);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Ignore
			}
		});
		builder.show();
	}

	private void gotoNextScreen()
	{
		/*
		 *  Transition to next screen
		 */

		wizardInterface.transitionToFragment(new OnboardingAccount());
	}

	@Override
	public void doNext()
	{
		final String serverURL = server.getText().toString();
		if (server.length() < 3) {
			enterServerError();
		} else {
			/*
			 *  Set the server prefix and test
			 */

			SCNetwork.get().setServerPrefix(serverURL);
			SCNetwork.get().request("login/status", null, false, true, this, new SCNetwork.ResponseInterface()
			{
				@Override
				public void responseResult(SCNetwork.Response response)
				{
					if (response.isSuccess()) {
						SCRSAManager.shared().setServerURL(serverURL);
						gotoNextScreen();
					} else {
						displayServerError();
					}
				}
			});
		}
	}
}
