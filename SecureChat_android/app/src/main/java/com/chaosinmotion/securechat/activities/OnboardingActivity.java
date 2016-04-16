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

public class OnboardingActivity extends AppCompatActivity implements WizardInterface
{
	private Stack<WizardFragment> wizardStack;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_onboarding);

		wizardStack = new Stack<WizardFragment>();

		if (savedInstanceState == null) {
			Fragment first = firstFragment();
			getFragmentManager().beginTransaction().
					add(R.id.root,first).
					commit();
			if (!(first instanceof WizardFragment)) {
				throw new RuntimeException("Wizard fragment must implement interface");
			}
			wizardStack.push((WizardFragment)first);
		}

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle("SecureChat Setup");
		setSupportActionBar(toolbar);
	}

	/**
	 * Returns the first fragment in a wizard sequence
	 * @return
	 */
	private Fragment firstFragment()
	{
		return new OnboardingSetupFragment();
	}

	/**
	 * Runs the transition to the next fragment
	 * @param fragment
	 */
	@Override
	public void transitionToFragment(Fragment fragment)
	{
		if (!(fragment instanceof WizardFragment)) {
			throw new RuntimeException("Wizard fragment must implement interface");
		}

		// Hide soft keyboard
		View view = getCurrentFocus();
		if (view != null) {
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(),0);
		}

		// Transition
		FragmentTransaction t = getFragmentManager().beginTransaction();
		t.replace(R.id.root,fragment);
		t.addToBackStack(null);
		t.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		t.commit();

		wizardStack.push((WizardFragment)fragment);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_onboarding, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.action_next) {
			// Send event to fragment so it can determine who is next
			wizardStack.peek().doNext();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed()
	{
		if(getFragmentManager().getBackStackEntryCount() != 0) {
			getFragmentManager().popBackStack();
			wizardStack.pop();
		} else {
			super.onBackPressed();
		}
	}
}
