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

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.fragments.OnboardingSetupFragment;

public class OnboardingActivity extends AppCompatActivity implements OnboardingSetupFragment.OnFragmentInteractionListener
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_onboarding);

		if (savedInstanceState == null) {
			OnboardingSetupFragment first = new OnboardingSetupFragment();
			getSupportFragmentManager().beginTransaction().
					add(R.id.root,first).
					commit();
		}

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		toolbar.setTitle("SecureChat Setup");
	}

	@Override
	public void onFragmentInteraction(Uri uri)
	{
		// TODO (and rewrite as needed; this is silly)
	}
}
