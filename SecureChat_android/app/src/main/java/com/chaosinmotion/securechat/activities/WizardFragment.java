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

/**
 * This is the interface that should be implemented by the wizard fragment
 * to handle a sequence of fragments as part of a wizard presentation.
 * Created by woody on 4/16/16.
 */
public interface WizardFragment
{
	/**
	 * Get the title that should be shown at this stage
	 */

	int getTitleResourceID();

	/**
	 * Determine if we get the next item
	 */
	boolean showNext();

	/**
	 * Event sent by activity to indicate the next button was pressed
	 */
	void doNext();
}
