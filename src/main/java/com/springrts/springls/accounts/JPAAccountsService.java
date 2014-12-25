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
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA implementation of an accounts service.
 * Uses abstracted DB access to store data,
 * see persistence.xml for DB settings.
 *
 * @author hoijui
 */
public class JPAAccountsService extends AbstractAccountsService {

	private static final Logger LOG
			= LoggerFactory.getLogger(JPAAccountsService.class);
	private static final long DAY = 1000L * 60L * 60L * 24L;

	private final EntityManagerFactory entManFactory;

	public JPAAccountsService() {

		EntityManagerFactory tmpEntManFactory = null;
		try {
			tmpEntManFactory = Persistence.createEntityManagerFactory("springls");
		} catch (final PersistenceException ex) {
			LOG.error("Failed to initialize database storage", ex);
		}
		entManFactory = tmpEntManFactory;
	}

	private EntityManager open() {

		final EntityManager entMan = entManFactory.createEntityManager();
		return entMan;
	}
	private void begin(final EntityManager entMan) {
		entMan.getTransaction().begin();
	}
	private void commit(final EntityManager entMan) {
		entMan.getTransaction().commit();
	}
	private void rollback(final EntityManager entMan) {

		if (entMan == null) {
			LOG.error("Failed to create an entity manager");
		} else {
			try {
				if (entMan.isOpen() && entMan.getTransaction().isActive()) {
					entMan.getTransaction().rollback();
				} else {
					LOG.error("Failed to rollback a transaction: no active"
							+ " connection or transaction");
				}
			} catch (final PersistenceException ex) {
				LOG.error("Failed to rollback a transaction", ex);
			}
		}
	}
	private void close(final EntityManager entMan) {

		if (entMan == null) {
			LOG.error("Failed to create an entity manager");
		} else {
			try {
				if (entMan.isOpen()) {
					entMan.close();
				}
			} catch (final IllegalStateException ex) {
				LOG.error("Failed to close an entity manager", ex);
			}
		}
	}


	@Override
	public boolean isReadyToOperate() {
		return (entManFactory != null);
	}

	@Override
	public int getAccountsSize() {

		int accounts = -1;

		EntityManager entMan = null;
		try {
			entMan = open();
			final long numAccounts = (Long) (entMan.createNamedQuery("acc_size")
					.getSingleResult());
			accounts = (int) numAccounts;
		} catch (final Exception ex) {
			LOG.error("Failed fetching number of accounts", ex);
		} finally {
			close(entMan);
		}

		return accounts;
	}

	@Override
	public int getActiveAccountsSize() {

		int activeAccounts = -1;

		EntityManager entMan = null;
		try {
			entMan = open();
			final Query activeSizeQuery = entMan.createNamedQuery("acc_size_active");
			final long oneWeekAgo = System.currentTimeMillis() - (DAY * 7);
			activeSizeQuery.setParameter("oneWeekAgo", oneWeekAgo);
			activeAccounts = (int) (long) (Long)
					activeSizeQuery.getSingleResult();
		} catch (final Exception ex) {
			LOG.error("Failed fetching active accounts", ex);
			activeAccounts = -1;
		} finally {
			close(entMan);
		}

		return activeAccounts;
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

		EntityManager entMan = null;
		try {
			entMan = open();
			begin(entMan);
			entMan.persist(acc);
			commit(entMan);
		} catch (final Exception ex) {
			LOG.error("Failed adding an account", ex);
			rollback(entMan);
		} finally {
			close(entMan);
		}
	}

	@Override
	public void addAccounts(final Iterable<Account> accs) {

		EntityManager entMan = null;
		try {
			entMan = open();
			begin(entMan);

			for (final Account acc : accs) {
				entMan.merge(acc);
			}

			commit(entMan);
		} catch (final Exception ex) {
			LOG.error("Failed adding an account", ex);
			rollback(entMan);
		} finally {
			close(entMan);
		}
	}

	@Override
	public boolean removeAccount(final Account acc) {

		boolean removed = false;

		EntityManager entMan = null;
		try {
			entMan = open();
			begin(entMan);
			entMan.remove(acc);
			commit(entMan);
			removed = true;
		} catch (final Exception ex) {
			LOG.error("Failed removing an account", ex);
			rollback(entMan);
		} finally {
			close(entMan);
		}

		return removed;
	}

	@Override
	public Account getAccount(final String username) {

		Account act = null;

		EntityManager entMan = null;
		try {
			entMan = open();
			final Query fetchByNameQuery = entMan.createNamedQuery("acc_fetchByName");
			fetchByNameQuery.setParameter("name", username);
			act = (Account) fetchByNameQuery.getSingleResult();
		} catch (final NoResultException ex) {
			LOG.trace("Failed fetching an account by name: " + username
					+ " (user not found)", ex);
		} catch (final Exception ex) {
			LOG.trace("Failed fetching an account by name: " + username, ex);
		} finally {
			close(entMan);
		}

		return act;
	}

	@Override
	public Account findAccountNoCase(final String username) {

		Account act = null;

		EntityManager entMan = null;
		try {
			entMan = open();
			final Query fetchByLowerNameQuery = entMan.createNamedQuery(
					"acc_fetchByLowerName");
			fetchByLowerNameQuery.setParameter("lowerName",
					username.toLowerCase());
			act = (Account) fetchByLowerNameQuery.getSingleResult();
		} catch (final Exception ex) {
			LOG.trace("Failed fetching an account by name (case-insensitive)",
					ex);
		} finally {
			close(entMan);
		}

		return act;
	}

	@Override
	public Account findAccountByLastIP(final InetAddress ipAddress) {

		Account act = null;

		EntityManager entMan = null;
		try {
			entMan = open();
			final Query fetchByLastIpQuery = entMan.createNamedQuery("acc_fetchByLastIP");
			fetchByLastIpQuery.setParameter("ip", ipAddress.getHostAddress());
			act = (Account) fetchByLastIpQuery.getSingleResult();
		} catch (final Exception ex) {
			LOG.trace("Failed fetching an account by last IP", ex);
		} finally {
			close(entMan);
		}

		return act;
	}

	@Override
	public List<Account> findAccountsByEmail(final String email) {

		List<Account> fittingAccounts = Collections.EMPTY_LIST;

		EntityManager entMan = null;
		try {
			entMan = open();
			final Query fetchByEmailQuery = entMan.createNamedQuery("acc_fetchByEmail");
			fetchByEmailQuery.setParameter("email", email.toLowerCase());
			fittingAccounts = fetchByEmailQuery.getResultList();
		} catch (final Exception ex) {
			LOG.trace("Failed fetching accounts by email", ex);
		} finally {
			close(entMan);
		}

		return fittingAccounts;
	}

	@Override
	public boolean mergeAccountChanges(final Account account, final String oldName) {

		boolean replaced = false;

		EntityManager entMan = null;
		try {
			entMan = open();
			begin(entMan);
			entMan.merge(account);
			commit(entMan);
			replaced = true;
		} catch (final Exception ex) {
			LOG.error("Failed replacing an account", ex);
			rollback(entMan);
		} finally {
			close(entMan);
		}

		return replaced;
	}

	@Override
	public List<Account> fetchAllAccounts() {

		List<Account> acts = Collections.EMPTY_LIST;

		EntityManager entMan = null;
		try {
			entMan = open();
			acts = entMan.createNamedQuery("acc_list").getResultList();
		} catch (final Exception ex) {
			LOG.error("Failed fetching all accounts", ex);
		} finally {
			close(entMan);
		}

		return acts;
	}
}
