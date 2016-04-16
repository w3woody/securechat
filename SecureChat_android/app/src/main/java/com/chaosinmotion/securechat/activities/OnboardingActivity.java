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
import android.app.FragmentManager;
import android.content.Context;
import android.net.Uri;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.fragments.OnboardingSetPasscode;
import com.chaosinmotion.securechat.fragments.OnboardingSetupFragment;

import java.util.LinkedList;
import java.util.Stack;

public class OnboardingActivity extends AbstractWizardActivity
{

	/**
	 * Returns the first fragment in a wizard sequence
	 * @return
	 */
	protected Fragment firstFragment()
	{
		return new OnboardingSetupFragment();
	}
}
