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
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.chatusers.ChatAdapter;
import com.chaosinmotion.securechat.messages.SCMessageDatabase;
import com.chaosinmotion.securechat.messages.SCMessageQueue;

/**
 * Created by woody on 4/22/16.
 */
public class ChatActivity extends AppCompatActivity
{
	private String username;
	private int userID;
	private ChatAdapter chatActivity;
	private ListView listView;
	private Button sendButton;
	private EditText textView;

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

	    sendButton = (Button)findViewById(R.id.send);
	    textView = (EditText)findViewById(R.id.editText);
	    sendButton.setOnClickListener(new View.OnClickListener()
	    {
		    @Override
		    public void onClick(View v)
		    {
			    doSubmit();
		    }
	    });

	    // TODO: Scroll to end, maintain scroll to end.
    }

	private void doSubmit()
	{
		String clearText = textView.getText().toString();
		if (clearText.length() == 0) return;    // no message.

		textView.setText("");

		String sending = getResources().getString(R.string.sending);
		final ProgressDialog progressDialog = ProgressDialog.show(this,sending,null,true);
		SCMessageQueue.get().sendMessage(clearText, username, new SCMessageQueue.SenderCompletion()
		{
			@Override
			public void senderCallback(boolean success)
			{
				if (progressDialog != null) {
					progressDialog.hide();
				}

				if (success) {
					// TODO: send success?
				} else {
					// TODO: send failure?
				}
			}
		});
	}
}
