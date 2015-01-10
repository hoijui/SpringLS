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

package com.springrts.springls;


import com.springrts.springls.util.Processor;
import com.springrts.springls.util.ProtocolUtil;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Betalord
 * @author hoijui
 */
public class Battles implements ContextReceiver {

	private static final Logger LOG = LoggerFactory.getLogger(Battle.class);

	private final List<Battle> battles;
	private Context context;

	public Battles() {

		this.battles = new ArrayList<Battle>();
		this.context = null;
	}

	@Override
	public void setContext(final Context context) {

		this.context = context;
		for (final Battle battle : battles) {
			battle.setContext(context);
		}
	}

	public void verify(final Battle battle) {

		if (battle == null) {
			LOG.error("Invalid battle ID. Server will now exit!");
			context.getServerThread().closeServerAndExit();
		}
	}

	public int getBattlesSize() {
		return battles.size();
	}

	/**
	 * If battle with id 'battleID' exist, it is returned,
	 * or else null is returned.
	 */
	public Battle getBattleByID(final int battleId) {

		Battle battle = null;

		if (battleId != Battle.NO_BATTLE_ID) { // only used for speedup
			for (final Battle battle1 : battles) {
				if (battle1.getId() == battleId) {
					battle = battle1;
					break;
				}
			}
		}

		return battle;
	}

	/** Returns null if index is out of bounds */
	public Battle getBattleByIndex(final int index) {

		try {
			return battles.get(index);
		} catch (final IndexOutOfBoundsException ex) {
			return null;
		}
	}

	private static class BattleCloser implements Processor<Client> {
		@Override
		public void process(final Client curClient) {
			curClient.setBattleID(Battle.NO_BATTLE_ID);
		}
	}

	/** Will close given battle and notify all clients about it */
	public void closeBattleAndNotifyAll(final Battle battle) {

		battle.applyToClientsAndFounder(new BattleCloser());

		context.getClients().sendToAllRegisteredUsers("BATTLECLOSED "
				+ battle.getId());
		battles.remove(battle);
	}

	/**
	 * Removes the client from a battle and notifies everyone.
	 * This also checks if the client is the founder and closes the battle in
	 * that case. All client's bots in this battle are removed as well.
	 */
	public boolean leaveBattle(final Client client, final Battle battle) {

		if (battle.getFounder() == client) {
			closeBattleAndNotifyAll(battle);
		} else {
			if ((client.getBattleID() != battle.getId())
					|| !battle.removeClient(client))
			{
				return false;
			}
			client.setBattleID(Battle.NO_BATTLE_ID);
			battle.removeClientBots(client);
			context.getClients().sendToAllRegisteredUsers(String.format(
					"LEFTBATTLE %d %s", battle.getId(),
					client.getAccount().getName()));
		}

		return true;
	}

	private static class BattleJoiner implements Processor<Client> {

		private final int battleId;

		BattleJoiner(final int battleId) {
			this.battleId = battleId;
		}

		@Override
		public void process(final Client curClient) {
			curClient.sendLine(String.format("JOINEDBATTLE %d %s", battleId,
					curClient.getAccount().getName()));
		}
	}

	/**
	 * Will send a list of all active battles and users participating in it to
	 * the given client
	 */
	public void sendInfoOnBattlesToClient(final Client client) {

		client.beginFastWrite();
		for (final Battle battle : battles) {
			// make sure that clients behind NAT get local IPs and not external
			// ones
			final boolean local = battle.getFounder().getIp().equals(client.getIp());
			client.sendLine(battle.createBattleOpenedCommandEx(local));
			// We have to send UPDATEBATTLEINFO command too,
			// in order to tell the user how many spectators are in the battle,
			// for example.
			client.sendLine(String.format("UPDATEBATTLEINFO %d %d %d %d %s",
					battle.getId(),
					battle.spectatorCount(),
					ProtocolUtil.boolToNumber(battle.isLocked()),
					battle.getMapHash(),
					battle.getMapName()));
			battle.applyToClients(new BattleJoiner(battle.getId()));
		}
		client.endFastWrite();
	}

	/**
	 * Will add this battle object to battle list
	 */
	public void addBattle(final Battle battle) {

		battles.add(battle);
		battle.setContext(context);
	}
}
