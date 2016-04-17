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
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.chaosinmotion.securechat.MainApplication;
import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.activities.OnboardingActivity;
import com.chaosinmotion.securechat.activities.WizardFragment;
import com.chaosinmotion.securechat.activities.WizardInterface;
import com.chaosinmotion.securechat.messages.SCMessageQueue;
import com.chaosinmotion.securechat.rsa.SCRSAManager;

/**
 * A simple {@link Fragment} subclass.
 */
public class LoginNewAccount extends Fragment implements WizardFragment
{
	private WizardInterface wizardInterface;
	private Button newAccount;

	public LoginNewAccount()
	{
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (getArguments() != null) {
			// TODO: Load arguments
		}
	}
	@Override
	public void onActivityCreated(Bundle bundle)
	{
		super.onActivityCreated(bundle);

		newAccount = (Button)getView().findViewById(R.id.newAccount);
		newAccount.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				doNewAccount();
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_login_new_account, container, false);
	}

	@Override
	public void doNext()
	{
	}

	@Override
	public int getTitleResourceID()
	{
		return R.string.new_account_title;
	}

	@Override
	public boolean showNext()
	{
		return false;
	}

	private void runNewAccount()
	{
		/*
		 *  Stop message queue, dismiss this and run the dialog for
		 *  onboarding in my place.
		 */

		SCMessageQueue.get().stopQueue();
		MainApplication.loginResult(false);

		SCMessageQueue.get().clearQueue(getActivity());
		SCRSAManager.shared().clear(getActivity());

		/*
		 *  Launch onboarding sequence
		 */
		Intent intent = new Intent(getActivity(), OnboardingActivity.class);
		getActivity().startActivity(intent);
		getActivity().finish();
	}

	private void doNewAccount()
	{
		/*
		 *  Alert user and verify
		 */

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.new_account_verify_message);
		builder.setTitle(R.string.new_account_verify_title);
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Ignore
			}
		});
		builder.setPositiveButton(R.string.new_account_verify_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				runNewAccount();
			}
		});
		builder.show();
	}

	/*
	 *  Dear Google: WTF?
	 */

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
	}
}
