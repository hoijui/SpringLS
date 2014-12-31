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

package com.springrts.springls.accounts;


import com.springrts.springls.Account;
import com.springrts.springls.ContextReceiver;
import com.springrts.springls.LiveStateListener;
import com.springrts.springls.Updateable;
import java.net.InetAddress;
import java.util.Comparator;
import java.util.List;

/**
 * Data Access Object (DAO) interface for <tt>Account</tt>s.
 * Used for retrieving and storing accounts to permanent storage,
 * for example in files or a DB.
 *
 * @author hoijui
 */
public interface AccountsService extends ContextReceiver, LiveStateListener,
		Updateable
{
	/**
	 * Used to compare accounts by username.
	 * @see #COMPARATOR_STR_IGNORE_CASE
	 */
	Comparator<String> COMPARATOR_STR = new Comparator<String>() {
		@Override
		public int compare(final String str1, final String str2) {
			return str1.compareTo(str2);
		}
	};

	/**
	 * Same as {@link #COMPARATOR_STR}, only that this ignores case.
	 * @see #COMPARATOR_STR
	 */
	Comparator<String> COMPARATOR_STR_IGNORE_CASE = new Comparator<String>() {
		@Override
		public int compare(final String str1, final String str2) {
			return str1.compareToIgnoreCase(str2);
		}
	};

	/**
	 * Checks resources required for the service to operate.
	 * This may return false if the environment is missing resources, like a
	 * storage file not available, or a DB system not running.
	 * @return true if this service is ready to be initialize, false otherwise.
	 */
	boolean isReadyToOperate();

	/**
	 * Returns the number of all accounts.
	 */
	int getAccountsSize();

	/**
	 * Returns the number of all active accounts.
	 * An account has to full fill two criteria to be considered active:
	 * - last login is not more then 1 week ago
	 * - rank is higher then Newbie/Rank 1
	 */
	int getActiveAccountsSize();

	/**
	 * (Re-)Loads accounts from disk.
	 * @return false if loading failed, true otherwise
	 */
	boolean loadAccounts();

	/**
	 * Saves accounts to permanent storage.
	 * @param block if false, this method will spawn a new thread, so this
	 *   method can return immediately (non-blocking mode). If true, it will not
	 *   return until the accounts have been saved.
	 */
	void saveAccounts(boolean block);

	/**
	 * Saves accounts only if they have not been saved for some time.
	 * This method should be called periodically!
	 * @see #saveAccounts(boolean block)
	 */
	void saveAccountsIfNeeded();

	/**
	 * Add an account to be persisted.
	 * Note: The caller has to check if username/password is valid etc.!
	 */
	void addAccount(Account acc);

	/**
	 * Add accounts to be persisted.
	 * Note: The caller has to check if usernames/passwords are valid etc.!
	 */
	void addAccounts(Iterable<Account> accs);

	boolean addAccountWithCheck(Account acc);

	boolean removeAccount(Account acc);

	boolean removeAccount(String userName);

	/** Returns null if account is not found */
	Account getAccount(String userName);

	Account findAccountNoCase(String userName);

	/** Returns 'null' if no account ever connected from this IP */
	Account findAccountByLastIP(InetAddress ipAddress);

	/**
	 * Returns an empty set if no account has the given email set.
	 * @params email email to look for, case is ignored
	 */
	List<Account> findAccountsByEmail(String email);

	boolean doesAccountExist(String userName);

	/**
	 * Save changes to an account to permanent storage.
	 * @param account the account which got changed
	 * @param oldName the old value of the name attribute of the account
	 *   is only used by the 'FSAccountsService'
	 * @return 'true' if changes were saved successfully
	 */
	boolean mergeAccountChanges(Account account, String oldName);

	/**
	 * Loads all accounts from the persistent storage into memory.
	 * This should only be used for maintenance task, and not during general
	 * server up-time.
	 */
	List<Account> fetchAllAccounts();

	/**
	 * Indicates whether or not it is possible to register new accounts.
	 */
	boolean isRegistrationEnabled();

	/**
	 * Sets whether or not it is possible to register new accounts.
	 * @return true if new value was successfully set
	 */
	boolean setRegistrationEnabled(boolean registrationEnabled);

	/**
	 * Checks if a pair of login credentials are valid.
	 * This also obfuscates (by not logging) the reason why the login is
	 * invalid.
	 * @param username
	 * @param password
	 * @return the account that fits to the login credentials,
	 *   or <code>null</code>, in case they are not valid
	 */
	Account verifyLogin(String username, String password);
}
