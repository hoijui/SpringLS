/*
	Copyright (c) 2006 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.springls.accounts;


import com.springrts.springls.Account;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This will be used in LAN mode only, which means that you can login with any
 * account credentials.
 * @author hoijui
 */
public class LanAccountsService extends AbstractAccountsService {

	private final List<Account> accounts;
	private int biggestAccountId;

	/**
	 * Used to speed up searching for accounts by username.
	 * (TreeMap class implements efficient Red-Black trees)
	 * @see mapNoCase
	 */
	private final Map<String, Account> map;

	/**
	 * Same as 'map', only that this ignores case.
	 * @see map
	 */
	private final Map<String, Account> mapNoCase;


	public LanAccountsService() {

		// NOTE ArrayList is not synchronized!
		// Use Collections.synchronizedList(...) instead,
		// if multiple threads are going to access it.
		accounts = new ArrayList<Account>();
		biggestAccountId = 1000;
		map = new TreeMap<String, Account>(COMPARATOR_STR);
		mapNoCase = new TreeMap<String, Account>(COMPARATOR_STR_IGNORE_CASE);
	}


	@Override
	public boolean isReadyToOperate() {
		return true;
	}

	@Override
	public int getAccountsSize() {
		return getActiveAccountsSize();
	}

	@Override
	public int getActiveAccountsSize() {
		return accounts.size();
	}

	@Override
	public boolean loadAccounts() {
		return true;
	}

	@Override
	public void saveAccounts(final boolean block) {}

	@Override
	public void saveAccountsIfNeeded() {}

	@Override
	public void addAccount(final Account acc) {

		if (acc.getId() == Account.NEW_ACCOUNT_ID) {
			acc.setId(++biggestAccountId);
		} else if (acc.getId() > biggestAccountId) {
			biggestAccountId = acc.getId();
		}
		accounts.add(acc);
		map.put(acc.getName(), acc);
		mapNoCase.put(acc.getName(), acc);
	}

	@Override
	public void addAccounts(final Iterable<Account> accs) {

		for (final Account acc : accs) {
			addAccount(acc);
		}
	}

	@Override
	public boolean removeAccount(final Account acc) {

		final boolean result = accounts.remove(acc);

		map.remove(acc.getName());
		mapNoCase.remove(acc.getName());

		return result;
	}

	/** Returns null if account is not found */
	@Override
	public Account getAccount(final String username) {
		return map.get(username);
	}

	/** Returns 'null' if index is out of bounds */
	public Account getAccount(final int index) {

		try {
			return accounts.get(index);
		} catch (final IndexOutOfBoundsException ex) {
			return null;
		}
	}

	@Override
	public Account findAccountNoCase(final String username) {
		return mapNoCase.get(username);
	}

	@Override
	public Account findAccountByLastIP(final InetAddress ipAddress) {
		return null;
	}

	@Override
	public List<Account> findAccountsByEmail(final String email) {
		return Collections.EMPTY_LIST;
	}

	@Override
	public boolean mergeAccountChanges(
			final Account account,
			final String oldName)
	{
		// persistent here only means, that someone is currently logged in with
		// this name
		final boolean isPersistentAccount = map.containsKey(oldName);
		if (!isPersistentAccount) {
			return false;
		}

		final String newName = account.getName();
		if (!newName.equals(oldName)) {
			// the account was renamed
			map.remove(oldName);
			mapNoCase.remove(oldName);
			map.put(newName, account);
			mapNoCase.put(newName, account);
		}

		return true;
	}

	@Override
	public List<Account> fetchAllAccounts() {
		return accounts;
	}
}
