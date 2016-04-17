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
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.activities.WizardFragment;
import com.chaosinmotion.securechat.activities.WizardInterface;

/**
 * A simple {@link Fragment} subclass.
 */
public class OnboardingAccount extends Fragment implements WizardFragment
{
	private WizardInterface wizardInterface;
	private Button login;
	private Button create;

	public OnboardingAccount()
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

		create = (Button)getView().findViewById(R.id.createAccount);
		create.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				doNext();
			}
		});

		login = (Button)getView().findViewById(R.id.loginAccount);
		login.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				doLogin();
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_onboarding_account, container, false);
	}

	@Override
	public void doNext()
	{
		doCreate();
//		wizardInterface.transitionToFragment(new OnboardingSetPasscode());
	}

	@Override
	public int getTitleResourceID()
	{
		return R.string.onboarding_title_account;
	}

	@Override
	public boolean showNext()
	{
		return false;
	}

	private void doCreate()
	{
		wizardInterface.transitionToFragment(new OnboardingCreateAccount());
	}

	private void doLogin()
	{
		wizardInterface.transitionToFragment(new OnboardingLoginAccount());
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
