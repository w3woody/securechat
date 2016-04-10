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

import android.test.AndroidTestCase;

import com.chaosinmotion.securechat.network.SCNetwork;
import com.chaosinmotion.securechat.network.SCNetworkCredentials;

import java.util.concurrent.CountDownLatch;

/**
 * Provides testing for the SCNetwork class. Note this requires a running
 * server. You can set that up by setting up Eclipse and running the server
 * there (with a local copy of Postgres, 'natch.)
 *
 * For some reason--that I'm sure some investigation will resolve--this
 * doesn't work on 127.0.0.1
 *
 * Created by woody on 4/10/16.
 */
public class NetworkUnitTest extends AndroidTestCase
{
	/**
	 * The most simple test we can do; this simply requests a login token
	 * from the back end.
	 */
	public void testToken() throws InterruptedException
	{
		final CountDownLatch expectation = new CountDownLatch(1);

		SCNetwork.get().setServerPrefix("http://192.168.1.214:8080/securechat");
		SCNetwork.get().setNetworkDelegate(new SCNetwork.Delegate() {
			@Override
			public void startWaitSpinner()
			{
			}

			@Override
			public void stopWaitSpinner()
			{
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
				response.didLogin(false);
			}
		});

		SCNetwork.get().request("login/token", null, this, new SCNetwork.ResponseInterface()
		{
			@Override
			public void responseResult(SCNetwork.Response response)
			{
				try {
					assertTrue(response.isSuccess());
					assertTrue(null != response.getData().optString("token"));
				}
				finally {
					expectation.countDown();
				}
			}
		});

		expectation.await();
	}
}
