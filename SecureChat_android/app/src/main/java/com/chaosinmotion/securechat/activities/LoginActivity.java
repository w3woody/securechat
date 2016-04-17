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

package com.chaosinmotion.securechat.activities;

import android.app.Fragment;

import com.chaosinmotion.securechat.fragments.LoginAccount;

public class LoginActivity extends AbstractWizardActivity
{

	/**
	 * Returns the first fragment in a wizard sequence
	 * @return
	 */
	protected Fragment firstFragment()
	{
		return new LoginAccount();
	}

	/**
	 * Returns false; this is because if we need to log in, we have a
	 * problem that needs to be resolved.
	 * @return
	 */
	@Override
	protected boolean canGoBack()
	{
		return false;
	}
}
