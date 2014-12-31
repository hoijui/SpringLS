/*
	Copyright (c) 2014 Robin Vobruba <hoijui.quaero@gmail.com>

	This program is free software; you can redistribute it and/or gameify
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

package com.springrts.springls.statistics;


import java.util.Comparator;

/**
 * @author hoijui
 */
class GameBattles implements Comparable<GameBattles> {

	public static final Comparator<GameBattles> BATTLES_COMPARATOR
			= new Comparator<GameBattles>() {
		@Override
		public int compare(final GameBattles gameBattles1, final GameBattles gameBattles2) {
			return gameBattles1.getBattles() - gameBattles2.getBattles();
		}
	};

	private final String name;
	private int battles;

	GameBattles(final String name, final int battles) {

		this.name = name;
		this.battles = battles;
	}

	public String getName() {
		return name;
	}

	public int getBattles() {
		return battles;
	}

	public void addBattles(final int additionalBattles) {
		this.battles += additionalBattles;
	}

	@Override
	public int compareTo(final GameBattles other) {
		return getName().compareTo(other.getName());
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof String) {
			return getName().equals((String) other);
		} else if (other instanceof GameBattles) {
			return getName().equals(((GameBattles) other).getName());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 23 * hash + (this.name != null ? this.name.hashCode() : 0);
		return hash;
	}
}
