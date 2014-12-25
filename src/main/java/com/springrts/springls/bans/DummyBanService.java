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

package com.springrts.springls.bans;


import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

/**
 * Dummy implementation of a BanService.
 * This will always contain 0 bans.
 * @author hoijui
 */
public class DummyBanService implements BanService{

	@Override
	public int getBansSize() {
		return 0;
	}

	@Override
	public int getActiveBansSize() {
		return 0;
	}

	@Override
	public void addBanEntry(final BanEntry ban) {}

	@Override
	public boolean removeBanEntry(final BanEntry ban) {
		return true;
	}

	@Override
	public BanEntry getBanEntry(
			final String username,
			final InetAddress ipAddress,
			final int userId)
	{
		return null;
	}

	@Override
	public boolean mergeBanEntryChanges(final BanEntry ban) {
		return true;
	}

	@Override
	public List<BanEntry> fetchAllBanEntries() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public List<BanEntry> fetchActiveBanEntries() {
		return Collections.EMPTY_LIST;
	}
}
