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


import com.springrts.springls.util.Misc;
import com.springrts.springls.util.Processor;
import com.springrts.springls.util.ProtocolUtil;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.configuration.Configuration;

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
	 * Creates new Battle object from a command that client sent to server.
	 * This method parses the command 's', and tries to read
	 * battle attributes from it.
	 * @return the created battle or 'null' on failure
	 */
	public Battle createBattleFromString(final List<String> args, final Client founder) {

		if (args.size() < 9) {
			return null;
		}

		final String pass = args.get(2);
		if (!pass.equals("*") && !pass.matches("^[A-Za-z0-9_]+$")) {
			// invalid characters in the password
			return null;
		}

		int type;
		int natType;
		int port;
		int maxPlayers;
		int hash;
		int rank;
		int maphash;

		try {
			type = Integer.parseInt(args.get(0));
			natType = Integer.parseInt(args.get(1));
			// args.get(2) is password
			port = Integer.parseInt(args.get(3));
			maxPlayers = Integer.parseInt(args.get(4));
			hash = Integer.parseInt(args.get(5));
			rank = Integer.decode(args.get(6));
			maphash = Integer.decode(args.get(7));
		} catch (NumberFormatException e) {
			return null;
		}

		final String[] sentenceArguments = Misc.makeSentence(args, 8).split("\t");
		if (sentenceArguments.length != 5) { // FIXME without cl flag it has to be 3!
			return null;
		}
		int sentenceIndex = 0;
		final String engineName;
		final String engineVersion;
		if (founder.getCompatFlags().contains("cl")) { // NOTE lobby protocol "0.36+ cl"
			engineName = sentenceArguments[sentenceIndex++]; // For example: 'my spring'
			engineVersion = sentenceArguments[sentenceIndex++]; // For example: '94.1.1-1062-g9d16c2d develop'
		} else {
			engineName = "spring"; // default; the same value uberserver uses
			final Configuration conf = context.getService(Configuration.class);
			engineVersion = conf.getString(ServerConfiguration.ENGINE_VERSION);
		}
		final String mapName = sentenceArguments[sentenceIndex++];
		final String title = sentenceArguments[sentenceIndex++];
		final String gameName = sentenceArguments[sentenceIndex++];

		if ((type < 0) || (type > 1)) {
			return null;
		}
		if ((natType < 0) || (natType > 2)) {
			return null;
		}

		return new Battle(type, natType, founder, pass, port,
				maxPlayers, hash, rank, maphash, mapName, title, gameName,
				engineName, engineVersion);
	}

	/**
	 * Will add this battle object to battle list
	 */
	public void addBattle(final Battle battle) {

		battles.add(battle);
		battle.setContext(context);
	}
}
