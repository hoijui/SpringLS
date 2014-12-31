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


import com.springrts.springls.util.ProtocolUtil;
import java.net.InetAddress;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA implementation of a ban entries service.
 * Uses abstracted DB access to store data,
 * see persistence.xml for DB settings.
 *
 * @author hoijui
 */
public class JPAIgnoreListService implements IgnoreListService {

	private static final Logger LOG
			= LoggerFactory.getLogger(JPAIgnoreListService.class);

	private final EntityManagerFactory entManFactory;

	public JPAIgnoreListService() {
		entManFactory = Persistence.createEntityManagerFactory("springls");
	}

	private EntityManager open() {
		return entManFactory.createEntityManager();
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
	public int getBansSize() {

		EntityManager entMan = null;
		try {
			entMan = open();
			final long numBans = (Long) (entMan.createNamedQuery("ban_size")
					.getSingleResult());
			return (int)numBans;
		} catch (final Exception ex) {
			LOG.error("Failed fetching number of bans", ex);
		} finally {
			close(entMan);
		}

		return -1;
	}

	@Override
	public int getActiveBansSize() {

		int activeBans = -1;

		EntityManager entMan = null;
		try {
			entMan = open();
			final long numBans = (Long) (entMan.createNamedQuery("ban_size_active")
					.getSingleResult());
			activeBans = (int) numBans;
		} catch (final Exception ex) {
			LOG.error("Failed fetching number of bans", ex);
		} finally {
			close(entMan);
		}

		return activeBans;
	}

	@Override
	public void addBanEntry(final IgnoreListEntry ban) {

		EntityManager entMan = null;
		try {
			entMan = open();
			begin(entMan);
			entMan.persist(ban);
			commit(entMan);
		} catch (final Exception ex) {
			LOG.error("Failed adding a ban", ex);
			rollback(entMan);
		} finally {
			close(entMan);
		}
	}

	@Override
	public boolean removeBanEntry(final IgnoreListEntry ban) {

		boolean removed = false;

		EntityManager entMan = null;
		try {
			entMan = open();
			begin(entMan);
			entMan.remove(ban);
			commit(entMan);
			removed = true;
		} catch (final Exception ex) {
			LOG.error("Failed removing a ban", ex);
			rollback(entMan);
		} finally {
			close(entMan);
		}

		return removed;
	}

	@Override
	public IgnoreListEntry getBanEntry(
			final String username,
			final InetAddress ip,
			final int userId)
	{
		IgnoreListEntry ban = null;

		EntityManager entMan = null;
		try {
			entMan = open();
			final Query fetchQuery = entMan.createNamedQuery("ban_fetch");
			fetchQuery.setParameter("username", username);
			fetchQuery.setParameter("ip", ProtocolUtil.ip2Long(ip));
			fetchQuery.setParameter("userId", userId);
			ban = (IgnoreListEntry) fetchQuery.getSingleResult();
		} catch (final Exception ex) {
			LOG.trace("Failed fetching a ban", ex);
			ban = null;
		} finally {
			close(entMan);
		}

		return ban;
	}

	@Override
	public boolean mergeBanEntryChanges(final IgnoreListEntry ban) {

		boolean replaced = false;

		EntityManager entMan = null;
		try {
			entMan = open();
			begin(entMan);
			entMan.merge(ban);
			commit(entMan);
			replaced = true;
		} catch (final Exception ex) {
			LOG.error("Failed replacing a ban", ex);
			rollback(entMan);
		} finally {
			close(entMan);
		}

		return replaced;
	}

	@Override
	public List<IgnoreListEntry> fetchAllBanEntries() {

		List<IgnoreListEntry> bans = null;

		EntityManager entMan = null;
		try {
			entMan = open();
			bans = (List<IgnoreListEntry>) (entMan.createNamedQuery("ban_list")
					.getResultList());
		} catch (final Exception ex) {
			LOG.error("Failed fetching all bans", ex);
			bans = null;
		} finally {
			close(entMan);
		}

		return bans;
	}

	@Override
	public List<IgnoreListEntry> fetchActiveBanEntries() {

		List<IgnoreListEntry> bans = null;

		EntityManager entMan = null;
		try {
			entMan = open();
			bans = (List<IgnoreListEntry>) (entMan.createNamedQuery("ban_list_active")
					.getResultList());
		} catch (final Exception ex) {
			LOG.error("Failed fetching all bans", ex);
			bans = null;
		} finally {
			close(entMan);
		}

		return bans;
	}
}
