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

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.chaosinmotion.securechat.MainApplication;
import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.activities.OnboardingActivity;
import com.chaosinmotion.securechat.network.SCNetwork;
import com.chaosinmotion.securechat.rsa.SCRSAManager;

import java.util.List;

public class MainActivity extends AppCompatActivity
{
	private ActionBarDrawerToggle drawerToggle;
	private ListView navDrawerList;
	private ArrayAdapter<String> navDrawerAdapter;
	private ListView senderList;
	private ArrayAdapter<String> senderListAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setHomeButtonEnabled(true);
		setupDrawer();

		navDrawerList = (ListView)findViewById(R.id.navList);
		initializeDrawer();

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
						.setAction("Action", null).show();
			}
		});

		/*
		 *  TODO: Finish me. For now, fillter.
		 */

		senderList = (ListView)findViewById(R.id.main_list);
		senderListAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, new String[]{ "Hi."} );
		senderList.setAdapter(senderListAdapter);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		/*
		 *  Verify if we need to onboard. If we do, dump myself and launch
		 *  the onboarding activity in my stead. It will then run me back
		 *  when the user is connected.
		 */

		if (!SCRSAManager.shared().hasSecureData(getApplication())) {
			// Load onboarding intent
			Intent intent = new Intent(this, OnboardingActivity.class);
			startActivity(intent);
			finish();
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration config)
	{
		super.onConfigurationChanged(config);
		drawerToggle.onConfigurationChanged(config);
	}

	private void initializeDrawer()
	{
		String[] list = getResources().getStringArray(R.array.drawer_commands);
		navDrawerAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, list);
		navDrawerList.setAdapter(navDrawerAdapter);
		navDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				doNavigationItem(position);
			}
		});
	}

	private void doNavigationItem(int position)
	{
		DrawerLayout layout = (DrawerLayout)findViewById(R.id.drawer_layout);
		layout.closeDrawers();

		/*
		 *  Disconnect item
		 */
		if (position == 2) {
			Intent intent = new Intent(this,DisconnectActivity.class);
			startActivity(intent);
			return;
		}
		if (position == 1) {
			Intent intent = new Intent(this,ChangePasscodeActivity.class);
			startActivity(intent);
			return;
		}
		if (position == 0) {
			Intent intent = new Intent(this,ChangePasswordActivity.class);
			startActivity(intent);
			return;
		}
	}

	private void setupDrawer()
	{
		DrawerLayout layout = (DrawerLayout)findViewById(R.id.drawer_layout);
		drawerToggle = new ActionBarDrawerToggle(this,layout,R.string.drawer_open,R.string.drawer_close);
		drawerToggle.setDrawerIndicatorEnabled(true);
		layout.addDrawerListener(drawerToggle);
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu)
//	{
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.menu_main, menu);
//		return true;
//	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle navigation drawer toggle
		if (drawerToggle.onOptionsItemSelected(item)) return true;

		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
