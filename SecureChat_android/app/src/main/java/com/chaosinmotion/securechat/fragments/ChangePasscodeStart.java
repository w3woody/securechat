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
import android.widget.EditText;

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
public class ChangePasscodeStart extends Fragment implements WizardFragment
{
	private WizardInterface wizardInterface;
	private EditText oldPasscode;
	private EditText newPasscode;
	private EditText copyPasscode;

	public ChangePasscodeStart()
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

		oldPasscode = (EditText)getView().findViewById(R.id.oldpasscode);
		newPasscode = (EditText)getView().findViewById(R.id.newpasscode);
		copyPasscode = (EditText)getView().findViewById(R.id.copypasscode);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_change_passcode_start, container, false);
	}

	@Override
	public void doNext()
	{
		String passcode = newPasscode.getText().toString();
		if (passcode.length() < 4) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.passcode_short_message);
			builder.setTitle(R.string.passcode_short_title);
			builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					// Ignore
				}
			});
			builder.show();
			return;
		}

		String retypedPasscode = copyPasscode.getText().toString();
		if (!retypedPasscode.equals(passcode)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.change_passcode_not_matched_message);
			builder.setTitle(R.string.change_passcode_not_matched_title);
			builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					// Ignore
				}
			});
			builder.show();
			return;
		}

		if (!SCRSAManager.shared().updatePasscode(passcode, oldPasscode.getText().toString(), getActivity())) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.change_passcode_wrong_message);
			builder.setTitle(R.string.change_passcode_wrong_title);
			builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					// Ignore
				}
			});
			builder.show();
			return;
		}

		wizardInterface.transitionToFragment(new ChangePasscodeEnd());
	}

	@Override
	public int getTitleResourceID()
	{
		return R.string.change_passcode_start_title;
	}

	@Override
	public boolean showNext()
	{
		return true;
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
