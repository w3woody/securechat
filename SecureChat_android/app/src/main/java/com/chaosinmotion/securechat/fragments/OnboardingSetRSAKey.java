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
import android.widget.RadioGroup;

import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.activities.WizardFragment;
import com.chaosinmotion.securechat.activities.WizardInterface;
import com.chaosinmotion.securechat.rsa.SCRSAManager;
import com.chaosinmotion.securechat.utils.ThreadPool;

/**
 * Onboarding RSA key
 */
public class OnboardingSetRSAKey extends Fragment implements WizardFragment
{
	private WizardInterface wizardInterface;
	private RadioGroup radioGroup;
	private Button generateKey;
	private boolean isGeneratingKey;

	private interface GenerateKeyCallback
	{
		void complete();
	}

	public OnboardingSetRSAKey()
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

		radioGroup = (RadioGroup)getView().findViewById(R.id.rsaPicker);
		radioGroup.check(R.id.rsa1024);
		generateKey = (Button)getView().findViewById(R.id.generateKey);
		generateKey.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				doGenerateKey();
			}
		});
//		passcode = (EditText)getView().findViewById(R.id.passcode);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_onboarding_set_rsa_key, container, false);
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
//		passcode = null;
	}


	private boolean isRSAKeyGenerated()
	{
		return SCRSAManager.shared().getPublicKey() != null;
	}

	private void doGenerateKey()
	{
		generateKeyWithCallback(new GenerateKeyCallback()
		{
			@Override
			public void complete()
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(R.string.generate_key_message);
				builder.setTitle(R.string.generate_key_title);
				builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						// Ignore
					}
				});
				builder.show();
			}
		});
	}

	/**
	 * Actually generates the RSA key.
	 * @param size
	 * @param callback
	 */
	private void generateRSAKey(final int size, final GenerateKeyCallback callback)
	{
		isGeneratingKey = true;

		generateKey.setEnabled(false);
		int color = getResources().getColor(R.color.colorDisabledButton);
		generateKey.setTextColor(color);
		ThreadPool.get().enqueueAsync(new Runnable()
		{
			@Override
			public void run()
			{
				SCRSAManager.shared().generateRSAKeyWithSize(size);

				ThreadPool.get().enqueueMain(new Runnable()
				{
					@Override
					public void run()
					{
						isGeneratingKey = false;
						generateKey.setEnabled(true);
						int color = getResources().getColor(R.color.colorButton);
						generateKey.setTextColor(color);
						generateKey.setText(R.string.onboarding_regenerate_key);
						callback.complete();
					}
				});
			}
		});
		// TODO
	}

	private void generateKeyWithCallback(final GenerateKeyCallback callback)
	{
		if (isGeneratingKey) return;

		final int size;
		int index = radioGroup.getCheckedRadioButtonId();
		switch (index) {
			default:
			case R.id.rsa1024:  size = 1024;    break;
			case R.id.rsa2048:  size = 2048;    break;
			case R.id.rsa4096:  size = 4096;    break;
		}

		if (isRSAKeyGenerated()) {
			/*
			 *  Regenerate warning
			 */
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.regenerate_key_message);
			builder.setTitle(R.string.regenerate_key_title);
			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					// Ignore
				}
			});
			builder.setPositiveButton(R.string.regenerate_key_prompt, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					generateRSAKey(size,callback);
				}
			});
			builder.show();
		} else if (size > 1024) {
			/*
			 *  Large key warning
			 */
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.long_key_message);
			builder.setTitle(R.string.long_key_title);
			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					// Ignore
				}
			});
			builder.setPositiveButton(R.string.long_key_prompt, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					generateRSAKey(size,callback);
				}
			});
			builder.show();
		} else {
			/*
			 *  It's just fine.
			 */
			generateRSAKey(size,callback);
		}
	}

	private void gotoNextPage()
	{
		wizardInterface.transitionToFragment(new OnboardingSetServer());
	}

	@Override
	public void doNext()
	{
		if (isGeneratingKey) return;

		if (isRSAKeyGenerated()) {
			gotoNextPage();
		} else {
			generateKeyWithCallback(new GenerateKeyCallback()
			{
				@Override
				public void complete()
				{
					gotoNextPage();
				}
			});
		}
	}
}
