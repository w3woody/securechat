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

package com.chaosinmotion.securechat.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rolling my own rather than using the built-in broadcastreceiver class
 * in order to match the semantics of the NSNotificationCenter class
 * Created by woody on 4/10/16.
 */
public class NotificationCenter
{
	/**
	 * Notification sent through this notification system.
	 */
	public static class Notification
	{
		private String name;
		private Object sender;
		private Map<String,Object> userData;

		public Notification(String name, Object sender, Map<String, Object> userData)
		{
			this.name = name;
			this.sender = sender;
			this.userData = userData;
		}

		public String getName()
		{
			return name;
		}

		public Object getSender()
		{
			return sender;
		}

		public Map<String, Object> getUserData()
		{
			return userData;
		}
	}

	/**
	 * The interface to adopt for receiving notifications
	 */
	public interface Observer
	{
		void notification(Notification n);
	}

	private static NotificationCenter shared;
	private HashMap<String,HashSet<Observer>> receivers;

	/**
	 * Get the default notification center
	 * @return
	 */
	public synchronized static NotificationCenter defaultCenter()
	{
		if (shared == null) shared = new NotificationCenter();
		return shared;
	}

	private NotificationCenter()
	{
		receivers = new HashMap<String,HashSet<Observer>>();
	}

	/**
	 * Post nofication with name from sender. No data provided
	 * @param name
	 * @param sender
	 */
	public void postNotification(String name, Object sender)
	{
		postNotification(name,sender,null);
	}

	/**
	 * Post notification with name from sender, with optional data
	 * @param name
	 * @param sender
	 * @param userData
	 */
	public void postNotification(String name, Object sender, Map<String,Object> userData)
	{
		Set<Observer> l;

		synchronized(receivers) {
			l = receivers.get(name);
		}

		if (l != null) {
			Notification n = new Notification(name,sender,userData);
			for (Observer obs: l) {
				obs.notification(n);
			}
		}
	}

	/**
	 * Add an observer to listen for messages named 'name'
	 * @param observer
	 * @param name
	 */
	public void addObserver(Observer observer, String name)
	{
		synchronized (receivers) {
			HashSet<Observer> list = receivers.get(name);
			if (list == null) {
				list = new HashSet<Observer>();
				receivers.put(name,list);
			}
			list.add(observer);
		}
	}

	/**
	 * Remove the observer. This removes all points where the observer
	 * is listening to messages
	 * @param observer
	 */
	public void removeObserver(Observer observer)
	{
		synchronized (receivers) {
			for (Map.Entry<String,HashSet<Observer>> e: receivers.entrySet()) {
				e.getValue().remove(observer);
			}
		}
	}
}
