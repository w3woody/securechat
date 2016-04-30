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
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.chatusers.ChatAdapter;
import com.chaosinmotion.securechat.encapsulation.SCMessageObject;
import com.chaosinmotion.securechat.messages.SCMessageDatabase;
import com.chaosinmotion.securechat.messages.SCMessageQueue;
import com.chaosinmotion.securechat.utils.JPEGImage;

import java.io.File;
import java.util.UUID;

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
	private ImageButton photoButton;

	private Uri imageUri;
	private static final int TAKE_PICTURE = 1;
	private static final int MAXSIZE = 640; // maximum image size

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
	    registerForContextMenu(listView);

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

	    photoButton = (ImageButton)findViewById(R.id.photo);
	    photoButton.setOnClickListener(new View.OnClickListener()
	    {
		    @Override
		    public void onClick(View v)
		    {
			    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			    File photo = new File(Environment.getExternalStorageDirectory(),"pic" + UUID.randomUUID().toString() + ".jpg");
				imageUri = Uri.fromFile(photo);
			    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
			    startActivityForResult(intent,TAKE_PICTURE);
		    }
	    });

	    // TODO: Scroll to end, maintain scroll to end.
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode,resultCode,data);
		if ((requestCode == TAKE_PICTURE) && (resultCode == Activity.RESULT_OK)) {
			getContentResolver().notifyChange(imageUri,null);
			ContentResolver cr = getContentResolver();
			Bitmap bmap = null;

			try {
				bmap = MediaStore.Images.Media.getBitmap(cr,imageUri);

				doSubmitPhoto(bmap);
			}
			catch (Exception ex) {
				Toast.makeText(this,"Failed to load photograph",Toast.LENGTH_LONG).show();
				Log.e("SecureChat","Camera failure",ex);
			}
			finally {
				if (bmap != null) {
					bmap.recycle();
				}
			}
		}
	}

	private void doSubmitPhoto(final Bitmap bmap)
	{
		// TODO
		JPEGImage image = new JPEGImage(bmap,MAXSIZE,MAXSIZE);
		SCMessageObject msg = new SCMessageObject(image);
		bmap.recycle();

		String sending = getResources().getString(R.string.sending);
		// TODO: wait spinner

		SCMessageQueue.get().sendMessage(msg, username, new SCMessageQueue.SenderCompletion()
		{
			@Override
			public void senderCallback(boolean success)
			{
				if (success) {
					Toast.makeText(ChatActivity.this,"Image uploaded",Toast.LENGTH_LONG).show();
					// TODO: send success?
				} else {
					// TODO: send failure?
				}
			}
		});
	}

	private void doSubmit()
	{
		String clearText = textView.getText().toString();
		if (clearText.length() == 0) return;    // no message.

		textView.setText("");

		SCMessageObject msg = new SCMessageObject(clearText);

		String sending = getResources().getString(R.string.sending);
		SCMessageQueue.get().sendMessage(msg, username, new SCMessageQueue.SenderCompletion()
		{
			@Override
			public void senderCallback(boolean success)
			{
				if (success) {
					// TODO: send success?
				} else {
					// TODO: send failure?
				}
			}
		});
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		if (v.getId() == R.id.main_list) {
			String[] menuItems = getResources().getStringArray(R.array.context_menu);
			for (int i = 0; i < menuItems.length; ++i) {
				menu.add(Menu.NONE,i,i,menuItems[i]);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		SCMessageDatabase.Message msg = chatActivity.getMessageAtIndex(info.position);

		if (item.getItemId() == 0) {
			/*
			 *  Delete the sender that was clicked on
			 */

			SCMessageQueue.get().deleteMessage(msg.getMessageID());
			chatActivity.notifyDataSetChanged();
		}

		return true;
	}

}
