/*
	Copyright (c) 2007 Robin Vobruba <hoijui.quaero@gmail.com>

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.springrts.springls;


/**
 * @author Betalord
 * @author hoijui
 */
public class FailedLoginAttempt {

	private final String userName;
	private int failedAttempts;
	private long timeOfLastFailedAttempt;
	private boolean logged;

	public FailedLoginAttempt(
			final String username,
			final int numOfFailedAttempts,
			final long timeOfLastFailedAttempt)
	{
		this.userName = username;
		this.failedAttempts = numOfFailedAttempts;
		this.timeOfLastFailedAttempt = timeOfLastFailedAttempt;
		this.logged = false;
	}

	public String getUserName() {
		return userName;
	}

	public int getFailedAttempts() {
		return failedAttempts;
	}

	public long getTimeOfLastFailedAttempt() {
		return timeOfLastFailedAttempt;
	}

	/**
	 * Did we log the repeatedly failed login attempt already?
	 */
	public boolean isLogged() {
		return logged;
	}

	public void setFailedAttempts(final int failedAttempts) {
		this.failedAttempts = failedAttempts;
	}

	public void addFailedAttempt() {

		this.timeOfLastFailedAttempt = System.currentTimeMillis();
		this.failedAttempts++;
	}

	public void setLogged(final boolean logged) {
		this.logged = logged;
	}
}
