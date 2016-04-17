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
import com.chaosinmotion.securechat.network.SCNetworkCredentials;
import com.chaosinmotion.securechat.rsa.SCRSAManager;
import com.chaosinmotion.securechat.utils.PasswordComplexity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChangePasswordStart extends Fragment implements WizardFragment
{
	private WizardInterface wizardInterface;
	private EditText oldPassword;
	private EditText newPassword;
	private EditText copyPassword;

	public ChangePasswordStart()
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

		oldPassword = (EditText)getView().findViewById(R.id.oldpassword);
		newPassword = (EditText)getView().findViewById(R.id.newpassword);
		copyPassword = (EditText)getView().findViewById(R.id.copypassword);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_change_password_start, container, false);
	}

	@Override
	public void doNext()
	{
		String password = newPassword.getText().toString();
		if (!PasswordComplexity.complexityTest(password)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.weak_password_message);
			builder.setTitle(R.string.weak_password_title);
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

		String retypedPassword = copyPassword.getText().toString();
		if (!retypedPassword.equals(password)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.change_password_not_matched_message);
			builder.setTitle(R.string.change_password_not_matched_title);
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

		changePassword(oldPassword.getText().toString(),password);
	}

	private void changePassword(final String oldPwd, final String newPwd)
	{
		SCNetwork.get().request("login/token", null, this, new SCNetwork.ResponseInterface()
		{
			@Override
			public void responseResult(SCNetwork.Response response)
			{
				if (response.isSuccess()) {
					String token = response.getData().optString("token");
					SCNetworkCredentials creds = new SCNetworkCredentials("");
					creds.setPasswordFromClearText(oldPwd);
					String oldPwdHash = creds.hashPasswordWithToken(token);
					creds.setPasswordFromClearText(newPwd);
					final String newPwdHash = creds.getPassword();

					try {
						JSONObject req = new JSONObject();
						req.put("oldpassword",oldPwdHash);
						req.put("newpassword",newPwdHash);

						SCNetwork.get().request("login/changepassword", req, this, new SCNetwork.ResponseInterface()
						{
							@Override
							public void responseResult(SCNetwork.Response response)
							{
								if (response.isSuccess()) {
									// Change password worked; update local store
									String username = SCRSAManager.shared().getUsername();
									SCRSAManager.shared().setCredentials(username,newPwdHash);
									SCRSAManager.shared().encodeSecureData(getActivity());

									// Done; go to next page
									wizardInterface.transitionToFragment(new ChangePasswordEnd());
								}
							}
						});
					}
					catch (JSONException ex) {
						// Should never happen
					}
				}
			}
		});
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
