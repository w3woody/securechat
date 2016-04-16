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

/**
 * Provides a common interface to our activities which present a sequence
 * of fragments as part of a "wizard" flow. This should be implemented by
 * the activity which presents a sequence of fragments.
 * Created by woody on 4/16/16.
 */
public interface WizardInterface
{
	/**
	 * Transition to the next fragment in the sequence.
	 * @param fragment
	 */
	void transitionToFragment(Fragment fragment);
}
