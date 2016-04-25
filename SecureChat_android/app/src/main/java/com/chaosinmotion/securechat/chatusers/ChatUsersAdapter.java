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


import com.chaosinmotion.securechat.messages.SCMessageDatabase;
import com.chaosinmotion.securechat.messages.SCMessageQueue;
import com.chaosinmotion.securechat.utils.NotificationCenter;

import java.util.ArrayList;
import java.util.List;

/**
 * The users adapter which pulls the list of users from our database.
 * This also contains code for handling changes in the database
 * Created by woody on 4/20/16.
 */
public class ChatUsersAdapter implements ListAdapter, NotificationCenter.Observer
{
	private ArrayList<DataSetObserver> observers;
	private List<SCMessageDatabase.Sender> data;
	private LayoutInflater inflater;
	private Context context;

	public ChatUsersAdapter(Context ctx)
	{
		context = ctx;
		observers = new ArrayList<DataSetObserver>();
		data = new ArrayList<SCMessageDatabase.Sender>();   // empty list
		NotificationCenter.defaultCenter().addObserver(this, SCMessageQueue.NOTIFICATION_NEWMESSAGE);
	}

	public void release()
	{
		NotificationCenter.defaultCenter().removeObserver(this);
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
		return data.size();
	}

	@Override
	public Object getItem(int position)
	{
		return data.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return data.get(position).getSenderID();
	}

	@Override
	public boolean hasStableIds()
	{
		return false;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		// TODO: Create message display view
		ChatUsersView view;
		if (convertView instanceof ChatUsersView) {
			view = (ChatUsersView)convertView;
		} else {
			view = new ChatUsersView(context);
		}

		view.setSender(data.get(position));

		return view;
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
		return data.size() == 0;
	}

	public void refreshData()
	{
		data = SCMessageQueue.get().getSenders();
		for (DataSetObserver obs: observers) {
			obs.onChanged();
		}
	}

	@Override
	public void notification(NotificationCenter.Notification n)
	{
		notifyDataSetChanged();
	}


	public void notifyDataSetChanged()
	{
		data = SCMessageQueue.get().getSenders();
		for (DataSetObserver obs: observers) {
			obs.onChanged();
		}
	}
}
