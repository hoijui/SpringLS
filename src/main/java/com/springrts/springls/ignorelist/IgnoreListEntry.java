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

package com.springrts.springls.ignorelist;


import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author Betalord
 * @author hoijui
 */
@Entity
@Table(name = "BanEntries") TODO;
@NamedQueries({
	@NamedQuery(name = "ban_size",        query = "SELECT count(b.id) FROM BanEntry b"),
	@NamedQuery(name = "ban_size_active", query = "SELECT count(b.id) FROM BanEntry b WHERE ((b.enabled = TRUE) AND (b.expireDate IS NULL OR b.expireDate > CURRENT_TIMESTAMP))"),
	@NamedQuery(name = "ban_list",        query = "SELECT b FROM BanEntry b"),
	@NamedQuery(name = "ban_list_active", query = "SELECT b FROM BanEntry b WHERE ((b.enabled = TRUE) AND (b.expireDate IS NULL OR b.expireDate > CURRENT_TIMESTAMP))"),
	@NamedQuery(name = "ban_fetch",       query = "SELECT b FROM BanEntry b WHERE ((b.username = :username) OR ((b.ipStart <= :ip) AND (b.ipStart >= :ip)) OR (b.userId >= :userId))")
})
public class IgnoreListEntry implements Serializable {

	@Id
	@GeneratedValue
	private Long id;

	/**
	 * Account ID of the ignored user.
	 * If <code>0</code>, then we ignore it.
	 */
	@Column(
		name       = "userID",
		unique     = true,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
	private int userId;
	/** Username of the ignored account, which may be <code>null</code>. */
	@Column(
		name       = "Username",
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true,
		length     = 40
		)
	private String username;
	/** The reason for the ignore, which may be <code>null</code>. */
	@Column(
		name       = "Reason",
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
	private String reason;

	/** Used by JPA */
	public IgnoreListEntry() {}
	public IgnoreListEntry(
			final int userId,
			final String userName,
			final String reason)
	{
		this.userId       = userId;
		this.username     = userName;
		this.reason      = reason;
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Account ID of the ignored account.
	 * @return the userId
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * Username of the ignored account (may be <code>null</code> as well)
	 * @return the username
	 */
	public String getUserName() {
		return username;
	}

	/**
	 * Reason for the ignore.
	 * The person who was ignored will see this when trying to write
	 * to the ignoring user.
	 * @return the reason
	 */
	public String getReason() {
		return reason;
	}
}
