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

package com.chaosinmotion.securechat;

import android.app.Application;
import android.app.ProgressDialog;

import com.chaosinmotion.securechat.network.SCNetwork;
import com.chaosinmotion.securechat.network.SCNetworkCredentials;

/**
 * Created by woody on 4/16/16.
 */
public class MainApplication extends Application implements SCNetwork.Delegate
{
	private int waitCount;
	private ProgressDialog progressDialog;

	@Override
	public void onCreate()
	{
		super.onCreate();

		SCNetwork.get().setNetworkDelegate(this);
	}

	@Override
	public void showServerError(SCNetwork.Response response)
	{

	}

	@Override
	public SCNetworkCredentials credentials()
	{
		return null;
	}

	@Override
	public void requestLoginDialog(SCNetwork.LoginResponse response)
	{

	}
}
