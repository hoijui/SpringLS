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
public class JPABanService implements BanService {

	private static final Logger LOG
			= LoggerFactory.getLogger(JPABanService.class);

	private final EntityManagerFactory emf;

	public JPABanService() {
		emf = Persistence.createEntityManagerFactory("springls");
	}

	private EntityManager open() {

		final EntityManager em = emf.createEntityManager();
		return em;
	}
	private void begin(final EntityManager em) {
		em.getTransaction().begin();
	}
	private void commit(final EntityManager em) {
		em.getTransaction().commit();
	}
	private void rollback(final EntityManager em) {

		if (em == null) {
			LOG.error("Failed to create an entity manager");
		} else {
			try {
				if (em.isOpen() && em.getTransaction().isActive()) {
					em.getTransaction().rollback();
				} else {
					LOG.error("Failed to rollback a transaction: no active"
							+ " connection or transaction");
				}
			} catch (final PersistenceException ex) {
				LOG.error("Failed to rollback a transaction", ex);
			}
		}
	}
	private void close(final EntityManager em) {

		if (em == null) {
			LOG.error("Failed to create an entity manager");
		} else {
			try {
				if (em.isOpen()) {
					em.close();
				}
			} catch (final IllegalStateException ex) {
				LOG.error("Failed to close an entity manager", ex);
			}
		}
	}

	@Override
	public int getBansSize() {

		EntityManager em = null;
		try {
			em = open();
			final long numBans = (Long) (em.createNamedQuery("ban_size")
					.getSingleResult());
			return (int)numBans;
		} catch (final Exception ex) {
			LOG.error("Failed fetching number of bans", ex);
		} finally {
			close(em);
		}

		return -1;
	}

	@Override
	public int getActiveBansSize() {

		int activeBans = -1;

		EntityManager em = null;
		try {
			em = open();
			final long numBans = (Long) (em.createNamedQuery("ban_size_active")
					.getSingleResult());
			activeBans = (int) numBans;
		} catch (final Exception ex) {
			LOG.error("Failed fetching number of bans", ex);
		} finally {
			close(em);
		}

		return activeBans;
	}

	@Override
	public void addBanEntry(final BanEntry ban) {

		EntityManager em = null;
		try {
			em = open();
			begin(em);
			em.persist(ban);
			commit(em);
		} catch (final Exception ex) {
			LOG.error("Failed adding a ban", ex);
			rollback(em);
		} finally {
			close(em);
		}
	}

	@Override
	public boolean removeBanEntry(final BanEntry ban) {

		boolean removed = false;

		EntityManager em = null;
		try {
			em = open();
			begin(em);
			em.remove(ban);
			commit(em);
			removed = true;
		} catch (final Exception ex) {
			LOG.error("Failed removing a ban", ex);
			rollback(em);
		} finally {
			close(em);
		}

		return removed;
	}

	@Override
	public BanEntry getBanEntry(
			final String username,
			final InetAddress ip,
			final int userId)
	{
		BanEntry ban = null;

		EntityManager em = null;
		try {
			em = open();
			Query fetchQuery = em.createNamedQuery("ban_fetch");
			fetchQuery.setParameter("username", username);
			fetchQuery.setParameter("ip", ProtocolUtil.ip2Long(ip));
			fetchQuery.setParameter("userId", userId);
			ban = (BanEntry) fetchQuery.getSingleResult();
		} catch (final Exception ex) {
			LOG.trace("Failed fetching a ban", ex);
			ban = null;
		} finally {
			close(em);
		}

		return ban;
	}

	@Override
	public boolean mergeBanEntryChanges(final BanEntry ban) {

		boolean replaced = false;

		EntityManager em = null;
		try {
			em = open();
			begin(em);
			em.merge(ban);
			commit(em);
			replaced = true;
		} catch (final Exception ex) {
			LOG.error("Failed replacing a ban", ex);
			rollback(em);
		} finally {
			close(em);
		}

		return replaced;
	}

	@Override
	public List<BanEntry> fetchAllBanEntries() {

		List<BanEntry> bans = null;

		EntityManager em = null;
		try {
			em = open();
			bans = (List<BanEntry>) (em.createNamedQuery("ban_list")
					.getResultList());
		} catch (final Exception ex) {
			LOG.error("Failed fetching all bans", ex);
			bans = null;
		} finally {
			close(em);
		}

		return bans;
	}

	@Override
	public List<BanEntry> fetchActiveBanEntries() {

		List<BanEntry> bans = null;

		EntityManager em = null;
		try {
			em = open();
			bans = (List<BanEntry>) (em.createNamedQuery("ban_list_active")
					.getResultList());
		} catch (final Exception ex) {
			LOG.error("Failed fetching all bans", ex);
			bans = null;
		} finally {
			close(em);
		}

		return bans;
	}
}
