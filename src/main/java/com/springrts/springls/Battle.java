/*
	Copyright (c) 2005 Robin Vobruba <hoijui.quaero@gmail.com>

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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Betalord
 * @author hoijui
 */
public class Battle implements ContextReceiver {

	private static final Logger LOG = LoggerFactory.getLogger(Battle.class);

	/** the id that the next battle will have */
	private static int nextId; // FIXME BAD! make non-static and move to class BattleS

	/**
	 * This is used as a null kind of value,
	 * whenever we want to refer to no-battle.
	 */
	public static final int NO_BATTLE_ID = -1;
	/** unique id */
	private final int id;
	/** 0 = normal battle, 1 = battle replay */
	private final int type;
	/** NAT traversal technique used by the host. Use 0 for none. */
	private final int natType;
	/** description set by the founder of the battle */
	private final String title;
	/** founder (host) of the battle */
	private final Client founder;
	/** clients without the founder (host) */
	private final List<Client> clients;
	/** bots added by clients participating in this battle */
	private final List<Bot> bots;
	/** see protocol description for details */
	private final String gameName;
	/** name of the map currently selected for this battle */
	private String mapName;
	/** see protocol description for details */
	private int mapHash;
	/** maximum number of players (including bots) that can participate */
	private final int maxPlayers;
	/** use restricted() method to find out if battle is password-protected */
	private final String password;
	/** IP port number this battle will be hosted on (default is 8452). */
	private final int port;
	/**
	 * This is a checksum over all the content required to participate in
	 * the to be played game.
	 * See "Syncing" in the lobby protocol specs for more details.
	 */
	private final int hashCode;
	/**
	 * If 0, no rank limit is set.
	 * If 1 or higher, only players with this rank (or higher)
	 * can join the battle
	 * (Note: rank index 1 means seconds rank, not the first one,
	 * since you can not limit the game to players of the first rank,
	 * because that means the game is open to all players
	 * and you do not have to limit it in that case)
	 */
	private final int rank;
	/** list of unit-definition names which are not allowed to be built */
	private final List<String> disabledUnits;
	/** list of start rectangles */
	private List<StartRect> startRects;
	/**
	 * if the battle is locked, no-one can join it
	 * (until the lock is released by founder)
	 */
	private boolean locked;
	/**  */
	private final Map<String, String> scriptTags;
	/**
	 * contains lines of the script file.
	 * This is only used if type == 1.
	 */
	private List<String> replayScript;
	/**
	 * Stores script lines until we receive the SCRIPTEND command.
	 * Then we copy it to the "replayScript" object,
	 * and notify all clients about it.
	 * This is only used if type == 1.
	 */
	private final List<String> tempReplayScript;
	private Context context;

	public Battle(
			final int type,
			final int natType,
			final Client founder,
			final String password,
			final int port,
			final int maxPlayers,
			final int hashCode,
			final int rank,
			final int mapHash,
			final String mapName,
			final String title,
			final String gameName)
	{
		this.id = nextId++;
		this.type = type;
		this.natType = natType;
		this.title = title;
		this.founder = founder;
		this.clients = new ArrayList<Client>();
		this.bots = new ArrayList<Bot>();
		this.mapName = mapName;
		this.maxPlayers = maxPlayers;
		this.password = password;
		this.port = port;
		this.hashCode = hashCode;
		this.rank = rank;
		this.mapHash = mapHash;
		this.gameName = gameName;
		this.disabledUnits = new ArrayList<String>();
		// we assume this by default. The client must make sure it is unlocked.
		this.locked = false;
		this.scriptTags = new HashMap<String, String>();
		this.replayScript = new ArrayList<String>();
		this.tempReplayScript = new ArrayList<String>();
	}

	@Override
	public void receiveContext(final Context context) {

		this.context = context;
		initStartRects();
	}

	private void initStartRects() {

		final int maxAllyTeams = context.getEngine().getMaxAllyTeams();
		this.startRects = new ArrayList<StartRect>(maxAllyTeams);
		for (int at = 0; at < maxAllyTeams; at++) {
			this.getStartRects().add(new StartRect());
		}
	}

	/**
	 * Creates BATTLEOPENED command from this battle and returns it as a result
	 * using the external IP.
	 */
	public String createBattleOpenedCommand() {
		return createBattleOpenedCommandEx(false);
	}

	/**
	 * Same as createBattleOpenedCommand() but requires sender to specify
	 * what IP to use (local or external).
	 * @see #createBattleOpenedCommand()
	 */
	public String createBattleOpenedCommandEx(final boolean local) {

		final InetAddress addr;
		if (local) {
			addr = getFounder().getLocalIp();
		} else {
			addr = getFounder().getIp();
		}

		return String.format(
				"BATTLEOPENED %d %d %d %s %s %d %d %d %d %d %s\t%s\t%s",
				getId(),
				getType(),
				getNatType(),
				getFounder().getAccount().getName(),
				addr.getHostAddress(),
				getPort(),
				getMaxPlayers(),
				ProtocolUtil.boolToNumber(restricted()),
				getRank(),
				getMapHash(),
				getMapName(),
				getTitle(),
				getGameName());
	}

	private static class BattleStatusNotifyer implements Processor<Client> {

		private final Client client;

		BattleStatusNotifyer(final Client client) {
			this.client = client;
		}

		@Override
		public void process(final Client curClient) {
			if (curClient != client) {
				client.sendLine(String.format("CLIENTBATTLESTATUS %s %d %d",
						curClient.getAccount().getName(),
						curClient.getBattleStatus(),
						ProtocolUtil.colorJavaToSpring(curClient.getTeamColor())));
			}
		}
	}

	/**
	 * Sends series of CLIENTBATTLESTATUS command to client telling him
	 * about the battle stati of all clients in this battle EXCEPT for himself!
	 */
	public void notifyOfBattleStatuses(final Client client) {
		applyToClientsAndFounder(new BattleStatusNotifyer(client));
	}

	/**
	 * Notifies all clients in the battle (including the client)
	 * about the new battle status of the client.
	 */
	public void notifyClientsOfBattleStatus(final Client client) {

		sendToAllClients(String.format("CLIENTBATTLESTATUS %s %d %d",
				client.getAccount().getName(),
				client.getBattleStatus(),
				ProtocolUtil.colorJavaToSpring(client.getTeamColor())));
	}

	public void notifyClientJoined(final Client client) {

		// do the actuall joining and notifying
		client.setDefaultBattleStatus();
		client.setBattleID(getId());
		client.setRequestedBattleID(Battle.NO_BATTLE_ID);
		addClient(client);
		// notify client that he has successfully joined the battle
		client.sendLine(String.format("JOINBATTLE %d %d", getId(),
				getHashCode()));
		context.getClients().notifyClientsOfNewClientInBattle(this, client);
		notifyOfBattleStatuses(client);
		sendBotListToClient(client);
		// tell host about this client's IP and UDP source port
		// if battle is hosted using one of the NAT traversal techniques
		if ((getNatType() == 1) || (getNatType() == 2)) {
			// make sure that clients behind NAT get local IPs and not external
			// ones
			final InetAddress ipAddress
					= (getFounder().getIp().equals(client.getIp())
					? client.getLocalIp() : client.getIp());
			getFounder().sendLine(String.format("CLIENTIPPORT %s %s %d",
					client.getAccount().getName(),
					ipAddress.getHostAddress(),
					client.getUdpSourcePort()));
		}

		client.sendLine("REQUESTBATTLESTATUS");
		sendDisabledUnitsListToClient(client);
		sendStartRectsListToClient(client);
		sendScriptTagsToClient(client);

		if (getType() == 1) {
			sendScriptToClient(client);
		}
	}


	private static class MessageSender implements Processor<Client> {

		private final String message;

		MessageSender(final String message) {
			this.message = message;
		}

		@Override
		public void process(final Client curClient) {
			curClient.sendLine(message);
		}
	}
	/**
	 * Sends <code>message</code> to all clients participating in this battle.
	 */
	public void sendToAllClients(final String message) {
		applyToClientsAndFounder(new MessageSender(message));
	}

	/**
	 * Sends <code>message</code> to all clients participating in this battle
	 * except for the founder.
	 */
	public void sendToAllExceptFounder(final String message) {
		applyToClients(new MessageSender(message));
	}

	public String clientsToString() {

		final String clientsString;
		if (!clients.isEmpty()) {
			final StringBuilder clientStringBuilder = new StringBuilder();
			for (final Client curClient : clients) {
				clientStringBuilder.append(' ')
						.append(curClient.getAccount().getName());
			}
			// delete the initial " "
			clientStringBuilder.deleteCharAt(0);
			clientsString = clientStringBuilder.toString();
		} else {
			clientsString = "";
		}

		return clientsString;
	}

	public void applyToClients(final Processor<? super Client> clientsProcessor)
	{
		for (final Client client : clients) {
			clientsProcessor.process(client);
		}
	}

	public void applyToClientsAndFounder(
			final Processor<? super Client> clientsProcessor)
	{
		applyToClients(clientsProcessor);
		clientsProcessor.process(founder);
	}

	private void applyToBots(final Processor<? super Bot> botsProcessor) {

		for (final Bot bot : bots) {
			botsProcessor.process(bot);
		}
	}

	public void applyToTeamControllers(
			final Processor<? super TeamController> teamControllerProcessor)
	{
		applyToClients(teamControllerProcessor);
		teamControllerProcessor.process(founder);
		applyToBots(teamControllerProcessor);
	}

	/**
	 * Returns the number of clients participating in this battle.
	 * @return number of clients participating in this battle
	 */
	public int getClientsSize() {
		return clients.size();
	}

	public boolean addClient(final Client client) {
		return this.clients.add(client);
	}

	public boolean removeClient(final Client client) {
		return this.clients.remove(client);
	}

	public boolean restricted() {
		return !password.equals("*");
	}

	public boolean inGame() {
		return getFounder().isInGame();
	}

	private static class SpectatorCounter implements Processor<Client> {

		private int count = 0;

		@Override
		public void process(final Client curClient) {
			if (curClient.isSpectator()) {
				count++;
			}
		}

		public int getCount() {
			return count;
		}
	}

	/**
	 * Returns number of spectators in this battle. Note that this operation is
	 * not very fast - we have to go through the entire list of clients in this
	 * battle to figure out spectator count.
	 * @return number of spectators in this battle
	 */
	public int spectatorCount() {

		final SpectatorCounter spectatorCounter = new SpectatorCounter();
		applyToClientsAndFounder(spectatorCounter);

		return spectatorCounter.getCount();
	}

	public int nonSpectatorCount() {
		return getClientsSize() + 1 - spectatorCount();
	}

	public boolean isClientInBattle(final Client client) {
		return (client.equals(getFounder()) || clients.contains(client));
	}

	private void sendDisabledUnitsListToClient(final Client client) {

		if (getDisabledUnits().isEmpty()) {
			// nothing to send
			return;
		}

		final StringBuilder line = new StringBuilder("DISABLEUNITS ");
		line.append(getDisabledUnits().get(0));
		for (int i = 1; i < getDisabledUnits().size(); i++) {
			line.append(' ').append(getDisabledUnits().get(i));
		}

		client.sendLine(line.toString());
	}

	/** Returns number of bots in this battle (size of the bot list) */
	public int getBotsSize() {
		return bots.size();
	}

	/**
	 * Returns Bot object of the specified bot, or <code>null</code>
	 * if the bot does not exist
	 */
	public Bot getBot(final String name) {

		for (final Bot bot : bots) {
			if (bot.getName().equals(name)) {
				return bot;
			}
		}
		return null;
	}

	/** Returns <code>null</code> if index is out of bounds */
	public Bot getBot(final int index) {

		Bot bot;

		try {
			bot = bots.get(index);
		} catch (final IndexOutOfBoundsException ex) {
			LOG.error("Failed fetching a bot by index", ex);
			bot = null;
		}

		return bot;
	}

	/** Adds the specified bot to the bot list, and informs other clients */
	public void addBot(final Bot bot) {

		bots.add(bot);

		sendToAllClients(String.format("ADDBOT %d %s %s %d %d %s",
				getId(),
				bot.getName(),
				bot.getOwnerName(),
				bot.getBattleStatus(),
				ProtocolUtil.colorJavaToSpring(bot.getTeamColor()),
				bot.getSpecifier()));
	}

	/**
	 * Removes the specified bot from the bot list if present, and informs other
	 * clients.
	 */
	public boolean removeBot(final Bot bot) {

		final boolean containedBot = bots.remove(bot);

		if (containedBot) {
			sendToAllClients(String.format("REMOVEBOT %d %s", getId(),
					bot.getName()));
		}

		return containedBot;
	}

	/* Removes all bots owned by client */
	public void removeClientBots(final Client client) {

		final Set<Bot> clientsBots = new HashSet<Bot>();
		for (final Bot bot : bots) {
			if (bot.getOwnerName().equals(client.getAccount().getName())) {
				clientsBots.add(bot);
			}
		}

		for (final Bot bot : clientsBots) {
			removeBot(bot);
		}
	}

	private void sendBotListToClient(final Client client) {

		for (final Bot bot : bots) {
			client.sendLine(String.format("ADDBOT %d %s %s %d %d %s",
					getId(),
					bot.getName(),
					bot.getOwnerName(),
					bot.getBattleStatus(),
					ProtocolUtil.colorJavaToSpring(bot.getTeamColor()),
					bot.getSpecifier()));
		}
	}

	private void sendStartRectsListToClient(final Client client) {

		for (int i = 0; i < getStartRects().size(); i++) {
			final StartRect curStartRect = getStartRects().get(i);
			if (curStartRect.isEnabled()) {
				client.sendLine(String.format("ADDSTARTRECT %d %d %d %d %d",
						i,
						curStartRect.getLeft(),
						curStartRect.getTop(),
						curStartRect.getRight(),
						curStartRect.getBottom()));
			}
		}
	}

	private void sendScriptToClient(final Client client) {

		client.beginFastWrite();
		client.sendLine("SCRIPTSTART");
		for (int i = 0; i < getReplayScript().size(); i++) {
			client.sendLine("SCRIPT " + getReplayScript().get(i));
		}
		client.sendLine("SCRIPTEND");
		client.endFastWrite();
	}

	public void sendScriptToAllExceptFounder() {

		for (final Client client : clients) {
			sendScriptToClient(client);
		}
	}

	/**
	 * Will put together all script tags in a single line
	 * to be taken as an argument to the SETSCRIPTTAGS command.
	 */
	private String joinScriptTags() {

		final StringBuilder joined = new StringBuilder();

		for (final Map.Entry<String, String> tag : getScriptTags().entrySet()) {
			joined
					.append('\t')
					.append(tag.getKey()).append('=').append(tag.getValue());
		}
		// delete the initial "\t"
		if (joined.length() > 0) {
			joined.deleteCharAt(0);
		}

		return joined.toString();
	}

	private void sendScriptTagsToClient(final Client client) {

		if (getScriptTags().isEmpty()) {
			// nothing to send
			return;
		}
		client.sendLine("SETSCRIPTTAGS " + joinScriptTags());
	}

	/**
	 * Will copy tempReplayScript to replayScript.
	 * This method is called when SCRIPTEND command is received.
	 */
	public void ratifyTempScript() {
		replayScript = new ArrayList<String>(getTempReplayScript());
	}

	/**
	 * Returns the unique id of this battle.
	 * @see #NO_BATTLE_ID
	 * @return the unique id of this battle
	 */
	public int getId() {
		return id;
	}

	/**
	 * 0 = normal battle, 1 = battle replay
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * NAT traversal technique used by the host. Use 0 for none.
	 * @return the natType
	 */
	public int getNatType() {
		return natType;
	}

	/**
	 * description set by the founder of the battle
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * founder (host) of the battle
	 * @return the founder
	 */
	public Client getFounder() {
		return founder;
	}

	/**
	 * name of the map currently selected for this battle
	 * @return the mapName
	 */
	public String getMapName() {
		return mapName;
	}

	/**
	 * name of the map currently selected for this battle
	 * @param mapName the mapName to set
	 */
	public void setMapName(final String mapName) {
		this.mapName = mapName;
	}

	/**
	 * maximum number of players (including bots) that can participate
	 * @return the maxPlayers
	 */
	public int getMaxPlayers() {
		return maxPlayers;
	}

	/**
	 * use restricted() method to find out if battle is password-protected
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * IP port number this battle will be hosted on (default is 8452).
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * This is a checksum over all the content required to participate in
	 * the to be played game.
	 * See "Syncing" in the lobby protocol specs for more details.
	 * @return the content checksum
	 */
	public int getHashCode() {
		return hashCode;
	}

	/**
	 * If 0, no rank limit is set.
	 * If 1 or higher, only players with this rank (or higher)
	 * can join the battle
	 * (Note: rank index 1 means seconds rank, not the first one,
	 * since you can not limit the game to players of the first rank,
	 * because that means the game is open to all players
	 * and you do not have to limit it in that case)
	 * @return the rank
	 */
	public int getRank() {
		return rank;
	}

	/**
	 * see protocol description for details
	 * @return the mapHash
	 */
	public int getMapHash() {
		return mapHash;
	}

	/**
	 * see protocol description for details
	 * @param mapHash the mapHash to set
	 */
	public void setMapHash(final int mapHash) {
		this.mapHash = mapHash;
	}

	/**
	 * see protocol description for details
	 * @return the modName
	 */
	public String getGameName() {
		return gameName;
	}

	/**
	 * list of unit-definition names which are not allowed to be built
	 * @return the disabledUnits
	 */
	public List<String> getDisabledUnits() {
		return disabledUnits;
	}

	/**
	 * list of start rectangles
	 * @return the startRects
	 */
	public List<StartRect> getStartRects() {
		return startRects;
	}

	/**
	 * if the battle is locked, no-one can join it
	 * (until the lock is released by founder)
	 * @return the locked
	 */
	public boolean isLocked() {
		return locked;
	}

	/**
	 * if the battle is locked, no-one can join it
	 * (until the lock is released by founder)
	 * @param locked the locked to set
	 */
	public void setLocked(final boolean locked) {
		this.locked = locked;
	}

	/**
	 *
	 * @return the scriptTags
	 */
	public Map<String, String> getScriptTags() {
		return scriptTags;
	}

	/**
	 * contains lines of the script file.
	 * This is only used if type == 1.
	 * @return the replayScript
	 */
	public List<String> getReplayScript() {
		return replayScript;
	}

	/**
	 * Stores script lines until we receive the SCRIPTEND command.
	 * Then we copy it to the "replayScript" object,
	 * and notify all clients about it.
	 * This is only used if type == 1.
	 * @return the tempReplayScript
	 */
	public List<String> getTempReplayScript() {
		return tempReplayScript;
	}
}
