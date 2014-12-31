/*
	Copyright (c) 2010 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.springls.ignorelist;


import java.net.InetAddress;
import java.util.List;

/**
 * Data Access Object (DAO) interface for IgnoreListEntry's.
 * Used for retrieving and storing bans to permanent storage,
 * for example in files or a DB.
 *
 * @author hoijui
 */
public interface IgnoreListService {

	/**
	 * Returns the number of all enlisted bans.
	 */
	int getBansSize();

	/**
	 * Returns the number of all bans currently active.
	 */
	int getActiveBansSize();

	// TODO Add a CommandProcessor to be able to register bans.
	// This was done through ChanServ or a web interface in the past.
	/**
	 * Add a ban-entry to be persisted.
	 * Note: caller must check if username/password is valid etc. himself!
	 */
	void addBanEntry(IgnoreListEntry ban);

	boolean removeBanEntry(IgnoreListEntry ban);

	/** Returns null if no matching ban is found */
	IgnoreListEntry getBanEntry(String username, InetAddress ipAddress, int userId);

	/**
	 * Save changes to a ban entry to permanent storage.
	 * @param ban the ban entry which got changed
	 * @return 'true' if changes were saved successfully
	 */
	boolean mergeBanEntryChanges(IgnoreListEntry ban);

	/**
	 * Loads all ban entries from the persistent storage into memory.
	 * This should only be used for maintenance task, and not during general
	 * server up-time.
	 */
	List<IgnoreListEntry> fetchAllBanEntries();

	/**
	 * Loads all active ban entries from the persistent storage into memory.
	 * This should only be used for maintenance task, and not during general
	 * server up-time.
	 */
	List<IgnoreListEntry> fetchActiveBanEntries();
}
