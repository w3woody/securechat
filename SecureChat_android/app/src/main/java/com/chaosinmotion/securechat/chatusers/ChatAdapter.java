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

package com.chaosinmotion.securechat.chatusers;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.chaosinmotion.securechat.R;
import com.chaosinmotion.securechat.encapsulation.SCMessageObject;
import com.chaosinmotion.securechat.messages.SCDecryptCache;
import com.chaosinmotion.securechat.messages.SCMessageDatabase;
import com.chaosinmotion.securechat.messages.SCMessageQueue;
import com.chaosinmotion.securechat.utils.DateUtils;
import com.chaosinmotion.securechat.utils.NotificationCenter;
import com.chaosinmotion.securechat.utils.ThreadPool;
import com.chaosinmotion.securechat.views.SCChatSummaryView;
import com.chaosinmotion.securechat.views.SCChatView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by woody on 4/24/16.
 */
public class ChatAdapter implements ListAdapter, NotificationCenter.Observer
{
	private ArrayList<DataSetObserver> observers;
	private LayoutInflater inflater;
	private Context context;
	private int senderID;

	private List<SCMessageDatabase.Message> list;
	private int location;
	private int length;

	public ChatAdapter(Context ctx, int sender)
	{
		list = null;
		senderID = sender;
		context = ctx;
		observers = new ArrayList<DataSetObserver>();
		NotificationCenter.defaultCenter().addObserver(this, SCMessageQueue.NOTIFICATION_NEWMESSAGE);
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return true;
	}

	@Override
	public boolean isEnabled(int position)
	{
		return true;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer)
	{
		observers.add(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer)
	{
		observers.remove(observer);
	}

	@Override
	public int getCount()
	{
		return SCMessageQueue.get().getMessagesForSender(senderID);
	}

	public SCMessageDatabase.Message getMessageAtIndex(int index)
	{
		int loc = (index & ~15);
		int len = 16;

		if ((list != null) && (loc == location) && (len == length)) {
			return list.get(index - loc);
		} else {
			location = loc;
			length = len;
			list = SCMessageQueue.get().getMessagesInRange(senderID,loc,len);
			return list.get(index - loc);
		}
	}

	@Override
	public Object getItem(int position)
	{
		return getMessageAtIndex(position);
	}

	@Override
	public long getItemId(int position)
	{
		SCMessageDatabase.Message msg = getMessageAtIndex(position);
		if (msg == null) return 0;
		return msg.getMessageID();
	}

	@Override
	public boolean hasStableIds()
	{
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		SCChatView chat;
		if (convertView instanceof SCChatView) {
			chat = (SCChatView) convertView;
		} else {
			chat = new SCChatView(context);
		}

		SCMessageDatabase.Message msg = getMessageAtIndex(position);
		String dateString = DateUtils.formatDisplayTime(msg.getTimestamp());

		SCMessageObject message = SCDecryptCache.get().decrypt(msg.getMessage(), msg.getMessageID(), new SCDecryptCache.DecryptCallback()
		{
			@Override
			public void decryptedMessage(int messageID, SCMessageObject msg)
			{
				// If we had to decrypt a message, we now need to reload the
				// view associated with that chat.

				// TODO: Is this the right answer here? Couldn't we simply
				// reload the individual message?
				for (DataSetObserver obs: observers) {
					obs.onChanged();
				}
			}
		});
		if (message == null) {
			String tmp = context.getResources().getString(R.string.decrypt_label);
			message = new SCMessageObject(tmp);
		}
		chat.setMessage(msg.isReceiveFlag(),message,dateString);
		return chat;
	}

	@Override
	public int getItemViewType(int position)
	{
		return 0;
	}

	@Override
	public int getViewTypeCount()
	{
		return 1;
	}

	@Override
	public boolean isEmpty()
	{
		return getCount() == 0;
	}

	@Override
	public void notification(final NotificationCenter.Notification n)
	{
		ThreadPool.get().enqueueMain(new Runnable()
		{
			@Override
			public void run()
			{
				Integer val = (Integer)(n.getUserData().get("userid"));
				if (val.intValue() == senderID) {
					// Invalidate cache and resend
					notifyDataSetChanged();
				}
			}
		});
	}

	public void notifyDataSetChanged()
	{
		list = null;
		for (DataSetObserver obs: observers) {
			obs.onChanged();
		}
	}
}
