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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ListView;

import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.chatusers.ChatAdapter;

/**
 * Created by woody on 4/22/16.
 */
public class ChatActivity extends AppCompatActivity
{
	private String username;
	private int userID;
	private ChatAdapter chatActivity;
	private ListView listView;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        username = intent.getStringExtra("username");
	    userID = intent.getIntExtra("userid",0);

        setTitle(username);   // TODO: Test

	    listView = (ListView)findViewById(R.id.main_list);
	    chatActivity = new ChatAdapter(this,userID);
	    listView.setAdapter(chatActivity);

	    // TODO: Scroll to end, maintain scroll to end.
    }
}
