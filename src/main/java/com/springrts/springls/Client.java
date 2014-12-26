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


import com.springrts.springls.ip2country.IP2CountryService;
import com.springrts.springls.util.Misc;
import com.springrts.springls.util.ProtocolUtil;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import org.apache.commons.configuration.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Client instance can be seen as a session of a human user, which has logged
 * into the Lobby using his <code>Account</code> info.
 *
 * @see Account
 * @author Betalord
 * @author hoijui
 */
public class Client extends TeamController implements ContextReceiver {

	private static final Logger LOG = LoggerFactory.getLogger(Client.class);

	/**
	 * Indicates a message is not using an ID
	 * (see protocol description on message/command IDs)
	 */
	public static final int NO_MSG_ID = -1;

	/**
	 * Indicates that no script-password is set.
	 */
	public static final String NO_SCRIPT_PASSWORD = "";

	/**
	 * If false, then this client is not "valid" anymore.
	 * We already killed him and closed his socket.
	 */
	private boolean alive;
	/**
	 * When we schedule client for kill (via Clients.killClientDelayed(),
	 * for example) this flag is set to true.
	 * When true, we do not read or send any data to this client.
	 */
	private boolean halfDead;

	private Account account;
	/**
	 * External IP
	 */
	private InetAddress ip;
	/**
	 * Local IP, which has to be sent with LOGIN command
	 * The server can not figure out the clients local IP by himself of course.
	 */
	private InetAddress localIp;
	/**
	 * Public UDP source port used with some NAT traversal techniques,
	 * e.g. "hole punching".
	 */
	private int udpSourcePort;
	private boolean inGame;
	private boolean away;
	/**
	 * ID of the battle in which this client is participating.
	 * Has to be -1 if not participating in any battle.
	 */
	private int battleID;
	/**
	 * ID of the battle which this client is requesting to join.
	 * Must be -1 if not requesting to join any battle.
	 */
	private int requestedBattleID;
	/**
	 * List of channels user is participating in.
	 */
	private final List<Channel> channels;

	private SocketChannel sockChan;
	private SelectionKey selKey;
	private final StringBuilder recvBuf;
	/**
	 * This is the message/command ID used when sending command
	 * as described in the "lobby protocol description" document.
	 * Use setSendMsgId() and resetSendMsgId() methods to manipulate it.
	 * NO_MSG_ID means no ID is used.
	 */
	private int myMsgId;
	/**
	 * Queue of "delayed data".
	 * We failed sending this the first time, so we will have to try sending it
	 * again some time later.
	 */
	private final Queue<ByteBuffer> sendQueue;
	/**
	 * Temporary StringBuilder used by some internal methods.
	 * @see beginFastWrite()
	 * @see endFastWrite()
	 */
	private StringBuilder fastWrite;

	/**
	 * In milliseconds.
	 * Used internally to remember time when the user entered the game.
	 * @see java.lang.System#currentTimeMillis()
	 */
	private long inGameTime;
	/**
	 * Specifying the origin of the user. So far, only the country part is used.
	 */
	private Locale locale;
	/**
	 * In MHz if possible, or in MHz*1.4 if AMD.
	 * 0 means the client can not figure out its CPU speed.
	 */
	private int cpu;
	/**
	 * e.g. "TASClient 1.0" (gets updated when server receives LOGIN command)
	 */
	private String lobbyVersion;
	/**
	 * How many bytes did this client send to us since he logged in.
	 */
	private long receivedSinceLogin = 0;
	/**
	 * Time (in milli-seconds) when we last heard from client
	 * (last data received).
	 * @see java.lang.System#currentTimeMillis()
	 */
	private long timeOfLastReceive;
	/**
	 * Does the client accept accountIDs in ADDUSER command?
	 */
	private boolean acceptAccountIDs;
	/**
	 * Does the client accept JOINBATTLEREQUEST command?
	 */
	private boolean handleBattleJoinAuthorization;

	private Context context = null;

	/**
	 * Script password for the battle this client currently is in.
	 * If it is not currently in a battle, this is irrelevant.
	 * The script password is used for spoof-protection, which means
	 * someone illegally joining the battle under wrong user-name.
	 */
	private String scriptPassword;

	/**
	 * Does the client accept the scriptPassord argument to the
	 * JOINEDBATTLE command?
	 */
	private boolean scriptPasswordSupported;

	/**
	 * A list of compatibility-flags, each representing a certain minor change
	 * in protocol since the last protocol version number release.
	 * These features all have to be implemented in a backwards compatible way,
	 * so lobby clients not supporting them are still able to function normally.
	 * This is comparable to IRC user/channel flags.
	 * By default, all the optional functionalities are considered
	 * as not supported by the client.
	 *
	 * See the "Recent Changes" section or the LOGIN command
	 * in the lobby protocol documentation for a list of the current flags.
	 */
	private List<String> compatFlags;


	public Client(final SocketChannel sockChan) {

		this.alive = true;
		this.halfDead = false;

		// no info on user/pass, zero access
		this.account = new Account();
		this.sockChan = sockChan;
		this.ip = sockChan.socket().getInetAddress();
		// this fixes the issue with local user connecting to the server at
		// "127.0.0.1", as he can not host battles with that ip
		if (ip.isLoopbackAddress()) {
			final InetAddress newIP = Misc.getLocalIpAddress();
			if (newIP != null) {
				ip = newIP;
			} else {
				LOG.warn("Could not resolve local IP address."
						+ " The user may have problems with hosting battles.");
			}
		}
		localIp = ip; // will be changed later once the client logs in
		udpSourcePort = 0; // yet unknown
		selKey = null;
		recvBuf = new StringBuilder();
		inGame = false;
		away = false;
		locale = ProtocolUtil.countryToLocale(ProtocolUtil.COUNTRY_UNKNOWN);
		myMsgId = NO_MSG_ID;
		sendQueue = new LinkedList<ByteBuffer>();
		inGameTime = 0;
		battleID = Battle.NO_BATTLE_ID;
		requestedBattleID = Battle.NO_BATTLE_ID;
		channels = new ArrayList<Channel>();
		cpu = 0;
		scriptPassword = NO_SCRIPT_PASSWORD;
		scriptPasswordSupported = false;
		compatFlags = new ArrayList<String>(0);

		timeOfLastReceive = System.currentTimeMillis();
	}


	@Override
	public void receiveContext(final Context context) {

		this.context = context;

		// TODO when bundle-context is available in ctor, move this code there
		final IP2CountryService ip2CountryService = context.getService(IP2CountryService.class);
		if (ip2CountryService != null) {
			setLocale(ip2CountryService.getLocale(ip));
		}

		final Set<String> supportedCompFlags = context.getServer().getSupportedCompFlags();
		supportedCompFlags.add("a");
		supportedCompFlags.add("b");
		supportedCompFlags.add("sp");
	}

	@Override
	public int hashCode() {

		int hash = 5;
		hash = 67 * hash + (this.sockChan != null ? this.sockChan.hashCode()
				: 0);
		return hash;
	}

	@Override
	public boolean equals(final Object obj) {

		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Client other = (Client) obj;
		if (this.getSockChan() != other.getSockChan()
				&& (this.getSockChan() == null))
		{
			return false;
		}
		return this.sockChan.equals(other.getSockChan());
	}

	/**
	 * Any messages sent via sendLine() method will contain this ID.
	 * See "lobby protocol description" document for more info
	 * on message/command IDs.
	 */
	public void setSendMsgId(final int msgId) {
		this.myMsgId = msgId;
	}

	public void resetSendMsgId(final int msgId) {
		this.myMsgId = msgId;
	}

	/**
	 * Will prefix the message with a msgId value, if it was previously
	 * set via setSendMsgId() method.
	 */
	public boolean sendLine(final String text) {
		return sendLine(text, myMsgId);
	}

	/**
	 * @param msgId overrides any previously set message ID,
	 *   use NO_MSG_ID for none.
	 * @see #setSendMsgId(int msgId)
	 */
	private boolean sendLine(final String text, final int msgId) {

		if (!alive || halfDead) {
			return false;
		}

		final StringBuilder data = new StringBuilder();

		// prefix message with a message ID:
		if (msgId != NO_MSG_ID) {
			data.append('#').append(msgId).append(' ');
		}
		data.append(text);

		if (fastWrite != null) {
			if (fastWrite.length() != 0) {
				fastWrite.append(Misc.EOL);
			}
			fastWrite.append(data);
			return true;
		}

		if (LOG.isTraceEnabled()) {
			final String nameOrIp = (account.getAccess() != Account.Access.NONE)
						? account.getName()
						: ip.getHostAddress();
			LOG.trace("[->{}] \"{}\"", nameOrIp, data);
		}

		try {
			// prepare data and add it to the send queue

			data.append(Misc.EOL);

			if ((sockChan == null) || (!sockChan.isConnected())) {
				LOG.warn("SocketChannel is not ready to be written to."
						+ " Killing the client next loop ...");
				context.getClients().killClientDelayed(this,
						"Quit: undefined connection error");
				return false;
			}

			ByteBuffer buf;
			try {
				buf = context.getServer().getAsciiEncoder().encode(
						CharBuffer.wrap(data));
			} catch (final CharacterCodingException ex) {
				LOG.warn("Unable to encode message. Killing the client next"
						+ " loop ...", ex);
				context.getClients().killClientDelayed(this,
						"Quit: undefined encoder error");
				return false;
			}

			if (!sendQueue.isEmpty()) {
				sendQueue.add(buf);
			} else {
				sendQueue.add(buf);
				final boolean empty = tryToFlushData();
				if (!empty) {
					context.getClients().enqueueDelayedData(this);
				}
			}
		} catch (final Exception ex) {
			LOG.error("Failed sending data (undefined). Killing the client next"
					+ " loop ...", ex);
			context.getClients().killClientDelayed(this,
					"Quit: undefined connection error");
			return false;
		}
		return true;
	}

	public void sendWelcomeMessage() {

		final Configuration conf = context.getService(Configuration.class);

		// the welcome messages command-name is hardcoded to TASSERVER
		// XXX maybe change TASSERVER to WELCOME or the like -> protocol change
		sendLine(String.format("TASSERVER %s %s %d %d",
				conf.getString(ServerConfiguration.LOBBY_PROTOCOL_VERSION),
				conf.getString(ServerConfiguration.ENGINE_VERSION),
				conf.getInt(ServerConfiguration.NAT_PORT),
				conf.getBoolean(ServerConfiguration.LAN_MODE) ? 1 : 0));
	}

	/** Should only be called by Clients.killClient() method! */
	public void disconnect() {

		if (!alive) {
			LOG.error("PROBLEM DETECTED: disconnecting dead client."
					+ " Skipping ...");
			return;
		}

		try {
			sockChan.close();
		} catch (final Exception ex) {
			LOG.error("Failed disconnecting socket!", ex);
		}

		sockChan = null;
		selKey = null;
	}

	/**
	 * joins client to <chanName> channel. If channel with that name
	 * does not exist, it is created. Method returns channel object as a result.
	 * If client is already in the channel, it returns null as a result.
	 * This method does not check for a correct key in case the channel is
	 * locked, caller of this method should do that before calling it.
	 * This method also does not do any notifying of other clients in the
	 * channel, the caller must do all that.
	 */
	public Channel joinChannel(final String chanName) {

		Channel chan = context.getChannels().getChannel(chanName);
		if (chan == null) {
			chan = new Channel(chanName);
			context.getChannels().addChannel(chan);
		} else if (this.channels.indexOf(chan) != -1) {
			// already in the channel
			return null;
		}

		chan.addClient(this);
		this.channels.add(chan);
		return chan;
	}

	/**
	 * Removes this client from a specific channel and notifies all other
	 * clients in that channel about it.
	 * If this was the last client in the channel, then the channel is removed
	 * from channels list.
	 * @param reason may be left blank ("") if no reason is to be given.
	 */
	public boolean leaveChannel(final Channel chan, final String reason) {

		final boolean left = chan.removeClient(this);

		if (left) {
			if (chan.getClientsSize() == 0) {
				// since channel is empty, there is no point in keeping it
				// in a channels list. If we would keep it, the channels list
				// would grow larger and larger in time.
				// We don't want that!
				context.getChannels().removeChannel(chan);
			} else {
				final StringBuilder message = new StringBuilder("LEFT ");
				message
						.append(chan.getName()).append(' ')
						.append(this.account.getName());
				if (!reason.isEmpty()) {
					message.append(' ').append(reason);
				}

				final String messageStr = message.toString();
				for (int i = 0; i < chan.getClientsSize(); i++) {
					chan.getClient(i).sendLine(messageStr);
				}
			}
			this.channels.remove(chan);
		}

		return left;
	}

	/**
	 * Calls leaveChannel() for every channel client is participating in.
	 * Also notifies all clients of his departure.
	 * Also see comments for leaveChannel() method.
	 */
	public void leaveAllChannels(final String reason) {

		while (!channels.isEmpty()) {
			leaveChannel(channels.get(0), reason);
		}
		this.channels.clear();
	}

	/**
	 * Will search the list of channels this user is participating in
	 * and return the specified channel or 'null' if client is not participating
	 * in this channel.
	 */
	public Channel getChannel(final String chanName) {

		for (final Channel channel : channels) {
			if (channel.getName().equals(chanName)) {
				return channel;
			}
		}
		return null;
	}

	/**
	 * Tries to send the data from the sendQueue.
	 * @return true if all data has been flushed; false otherwise.
	 */
	public boolean tryToFlushData() {

		if (!alive || halfDead) {
			// disregard any other scheduled writes:
			while (sendQueue.size() != 0) {
				sendQueue.remove();
			}
			return true; // no more data left to be flushed, so return true
		}

		ByteBuffer buf;
		while ((buf = sendQueue.peek()) != null) {
			try {
				sockChan.write(buf);

				if (buf.hasRemaining()) {
					// This happens when send buffer is full and no more data
					// can be written to it.
					// Lets just skip it without removing the packet
					// from the send queue (we will retry sending it later).
					break;
				}
				// remove element from queue (it was sent entirely)
				sendQueue.remove();
			} catch (final ClosedChannelException ccex) {
				// no point sending the rest to the closed channel
				if (alive) {
					context.getClients().killClientDelayed(this,
							"Quit: socket channel closed exception");
				}
				break;
			} catch (final IOException ioex) {
				if (alive) {
					context.getClients().killClientDelayed(this,
							"Quit: socket channel closed exception");
				}
				break;
			}
		}

		return sendQueue.size() == 0;
	}

	public void beginFastWrite() {

		if (fastWrite != null) {
			LOG.error("Invalid use of beginFastWrite()."
					+ " Check your code! Shutting down the server ...");
			context.getServerThread().closeServerAndExit();
		}

		fastWrite = new StringBuilder();
	}

	public void endFastWrite() {

		if (fastWrite == null) {
			LOG.error("Invalid use of endFastWrite()."
					+ " Check your code! Shutting down the server ...");
			context.getServerThread().closeServerAndExit();
		}

		final String data = fastWrite.toString();
		fastWrite = null;
		if (data.isEmpty()) {
			return;
		}
		sendLine(data);
	}


	// various methods dealing with client status

	public boolean isInGame() {
		return inGame;
	}

	public void setInGame(final boolean inGame) {
		this.inGame = inGame;
	}

	public boolean isAway() {
		return away;
	}

	public void setAway(final boolean away) {
		this.away = away;
	}

	/**
	 * The access status tells us whether this client is a server moderator or
	 * not.
	 * This is only used for generating the status bits for the CLIENTSTATUS
	 * command.
	 */
	private boolean isAccess() {
		return getAccount().getAccess().isAtLeast(Account.Access.PRIVILEGED);
	}

	/**
	 * If false, then this client is not "valid" anymore.
	 * We already killed him and closed his socket.
	 * @return the alive
	 */
	public boolean isAlive() {
		return alive;
	}

	/**
	 * If false, then this client is not "valid" anymore.
	 * We already killed him and closed his socket.
	 * @param alive the alive to set
	 */
	public void setAlive(final boolean alive) {
		this.alive = alive;
	}

	/**
	 * When we schedule client for kill (via Clients.killClientDelayed(),
	 * for example) this flag is set to true.
	 * When true, we do not read or send any data to this client.
	 * @return the halfDead
	 */
	public boolean isHalfDead() {
		return halfDead;
	}

	/**
	 * When we schedule client for kill (via Clients.killClientDelayed(),
	 * for example) this flag is set to true.
	 * When true, we do not read or send any data to this client.
	 * @param halfDead the halfDead to set
	 */
	public void setHalfDead(final boolean halfDead) {
		this.halfDead = halfDead;
	}

	/**
	 * @return the account
	 */
	public Account getAccount() {
		return account;
	}

	/**
	 * @param account the account to set
	 */
	public void setAccount(final Account account) {
		this.account = account;
	}

	/**
	 * External IP
	 * @return the IP
	 */
	public InetAddress getIp() {
		return ip;
	}

	/**
	 * External IP
	 * @param ipAddress the IP to set
	 */
	public void setIp(final InetAddress ipAddress) {
		this.ip = ipAddress;
	}

	/**
	 * Local IP, which has to be sent with LOGIN command
	 * The server can not figure out the clients local IP by himself of course.
	 * @return the localIP
	 */
	public InetAddress getLocalIp() {
		return localIp;
	}

	/**
	 * Local IP, which has to be sent with LOGIN command
	 * The server can not figure out the clients local IP by himself of course.
	 * @param localIp the local IP to set
	 */
	public void setLocalIP(final InetAddress localIp) {
		this.localIp = localIp;
	}

	/**
	 * Public UDP source port used with some NAT traversal techniques,
	 * e.g. "hole punching".
	 * @return the udpSourcePort
	 */
	public int getUdpSourcePort() {
		return udpSourcePort;
	}

	/**
	 * Public UDP source port used with some NAT traversal techniques,
	 * e.g. "hole punching".
	 * @param udpSourcePort the udpSourcePort to set
	 */
	public void setUdpSourcePort(final int udpSourcePort) {
		this.udpSourcePort = udpSourcePort;
	}

	/**
	 * See the 'MYSTATUS' command for valid values
	 * @return the status
	 */
	public int getStatus() {

		int status = 0;

		status +=  isInGame()           ? 1 : 0;
		status += (isAway()             ? 1 : 0)    << 1;
		status +=  getAccount().getRank().ordinal() << 2;
		status += (isAccess()           ? 1 : 0)    << 5;
		status += (getAccount().isBot() ? 1 : 0)    << 6;

		return status;
	}

	/**
	 * See the 'MYSTATUS' command for valid values
	 * @param status the status to set
	 * @param priviledged rank, access and bot are only changed if this is true
	 */
	public void setStatus(final int status, final boolean priviledged) {

		setInGame((status & 0x1)        == 1);
		setAway( ((status & 0x2)  >> 1) == 1);

		// This method is only used in MYSTATUS, which priviledged == false.
		// Therefore, the following is never used, and only stays here for
		// historical reasons.
		if (priviledged) {
			// use the highest rank, if a too high value was specified
//			int rankIndex = (status & 0x1C) >> 2;
//			Account.Rank newRank = (rankIndex < Account.Rank.values().length)
//					? Account.Rank.values()[rankIndex]
//					: Account.Rank.values()[Account.Rank.values().length - 1];
//			getAccount().setRank(newRank);
//			setAccess(((status & 0x20) >> 5) == 1);
			getAccount().setBot(((status & 0x40) >> 6) == 1);
		}
	}

	/**
	 * ID of the battle in which this client is participating.
	 * Has to be -1 if not participating in any battle.
	 * @return the battleID
	 */
	public int getBattleID() {
		return battleID;
	}

	/**
	 * ID of the battle in which this client is participating.
	 * Has to be -1 if not participating in any battle.
	 * @param battleID the battleID to set
	 */
	public void setBattleID(final int battleID) {

		this.battleID = battleID;
		if (battleID == Battle.NO_BATTLE_ID) {
			setScriptPassword(NO_SCRIPT_PASSWORD);
		}
	}

	/**
	 * ID of the battle which this client is requesting to join.
	 * Must be -1 if not requesting to join any battle.
	 * @return the requestedBattleID
	 */
	public int getRequestedBattleID() {
		return requestedBattleID;
	}

	/**
	 * ID of the battle which this client is requesting to join.
	 * Must be -1 if not requesting to join any battle.
	 * @param requestedBattleID the requestedBattleID to set
	 */
	public void setRequestedBattleID(final int requestedBattleID) {
		this.requestedBattleID = requestedBattleID;
	}

	/**
	 * @return the sockChan
	 */
	public SocketChannel getSockChan() {
		return sockChan;
	}

	/**
	 * @param selKey the selKey to set
	 */
	public void setSelKey(final SelectionKey selKey) {
		this.selKey = selKey;
	}

	public void appendToRecvBuf(final String received) {
		recvBuf.append(received);
	}

	private static boolean isWhiteSpace(final char chr) {
		return (" \n\r\t\f".indexOf(chr) != -1);
	}

	private static void deleteLeadingWhiteSpace(final StringBuilder str) {

		int wsPos = 0;
		while ((wsPos < str.length()) && isWhiteSpace(str.charAt(wsPos))) {
			wsPos++;
		}
		str.delete(0, wsPos);
	}

	/**
	 * Removes carriage-return chars (first part of windows EOL "\r\n").
	 * @param str where to search in
	 * @param posUntil only chars from 0 until this position are searched
	 * @return number of deleted chars
	 */
	private static int deleteCarriageReturnChars(
			final StringBuilder str,
			final int posUntil)
	{
		int deleted = 0;

		int rPos = str.lastIndexOf("\r", posUntil);
		while (rPos != -1) {
			str.deleteCharAt(rPos);
			deleted++;
			rPos = str.lastIndexOf("\r", rPos);
		}

		return deleted;
	}

	/**
	 * Tries to read a line from the clients input buffer.
	 * If the this returns non-<code>null</code>, then the line returned is
	 * already removed from the buffer.
	 * @return the older line from the clients input buffer or
	 *   <code>null</code>, if there is no full line available.
	 */
	public String readLine() {

		String line = null;

		deleteLeadingWhiteSpace(recvBuf);
		if (recvBuf.length() > 0) {
			final int nPos = recvBuf.indexOf("\n");
			if (nPos != -1) {
				final int deleted = deleteCarriageReturnChars(recvBuf, nPos);

				line = recvBuf.substring(0, nPos - deleted);
				recvBuf.delete(0, nPos - deleted);
			}
		}

		return line;
	}

	/**
	 * In milliseconds.
	 * Used internally to remember time when the user entered the game.
	 * @see java.lang.System#currentTimeMillis()
	 * @return the inGameTime
	 */
	public long getInGameTime() {
		return inGameTime;
	}

	/**
	 * In milliseconds.
	 * Used internally to remember time when the user entered the game.
	 * @see java.lang.System#currentTimeMillis()
	 * @param inGameTime the inGameTime to set
	 */
	public void setInGameTime(final long inGameTime) {
		this.inGameTime = inGameTime;
	}

	/**
	 * Specifying the origin of the user. So far, only the country part is used.
	 * @return the locale specifying the country
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Specifying the origin of the user. So far, only the country part is used.
	 * @param locale the locale specifying the country
	 */
	public void setLocale(final Locale locale) {
		this.locale = locale;
	}

	/**
	 * Two letter country code, as defined in ISO 3166-1 alpha-2.
	 * "XX" is used for unknown country.
	 * @return the country
	 */
	public String getCountry() {

		if ((locale == null) || locale.getCountry().isEmpty()) {
			return ProtocolUtil.COUNTRY_UNKNOWN;
		} else {
			return locale.getCountry();
		}
	}

	/**
	 * Two letter country code, as defined in ISO 3166-1 alpha-2.
	 * "XX" is used for unknown country.
	 * @param country the country to set
	 */
	public void setCountry(final String country) {
		ProtocolUtil.countryToLocale(country);
	}

	/**
	 * In MHz if possible, or in MHz*1.4 if AMD.
	 * 0 means the client can not figure out its CPU speed.
	 * @return the CPU
	 */
	public int getCpu() {
		return cpu;
	}

	/**
	 * In MHz if possible, or in MHz*1.4 if AMD.
	 * 0 means the client can not figure out its CPU speed.
	 * @param cpu the CPU to set
	 */
	public void setCpu(final int cpu) {
		this.cpu = cpu;
	}

	/**
	 * e.g. "TASClient 1.0" (gets updated when server receives LOGIN command)
	 * @return the lobbyVersion
	 */
	public String getLobbyVersion() {
		return lobbyVersion;
	}

	/**
	 * e.g. "TASClient 1.0" (gets updated when server receives LOGIN command)
	 * @param lobbyVersion the lobbyVersion to set
	 */
	public void setLobbyVersion(final String lobbyVersion) {
		this.lobbyVersion = lobbyVersion;
	}

	/**
	 * Time (in milli-seconds) when we last heard from client
	 * (last data received).
	 * @see java.lang.System#currentTimeMillis()
	 * @return the timeOfLastReceive
	 */
	public long getTimeOfLastReceive() {
		return timeOfLastReceive;
	}

	/**
	 * Time (in milli-seconds) when we last heard from client
	 * (last data received).
	 * @see java.lang.System#currentTimeMillis()
	 * @param timeOfLastReceive the timeOfLastReceive to set
	 */
	public void setTimeOfLastReceive(final long timeOfLastReceive) {
		this.timeOfLastReceive = timeOfLastReceive;
	}

	/**
	 * Returns the clients script password for the battle it currently is in.
	 * If it is not currently in a battle, this is irrelevant.
	 * The script password is used for spoof-protection, which means
	 * someone illegally joining the battle under wrong user-name.
	 */
	public String getScriptPassword() {
		return scriptPassword;
	}

	/**
	 * Sets the clients script password for the battle it currently is in.
	 * If it is not currently in a battle, this is irrelevant.
	 * The script password is used for spoof-protection, which means
	 * someone illegally joining the battle under wrong user-name.
	 */
	public void setScriptPassword(final String scriptPassword) {
		this.scriptPassword = scriptPassword;
	}

	/**
	 * Does the client accept accountIDs in ADDUSER command?
	 * @return the acceptAccountIDs
	 */
	public boolean isAcceptAccountIDs() {
		return acceptAccountIDs;
	}

	/**
	 * Does the client accept accountIDs in ADDUSER command?
	 * @param acceptAccountIDs the acceptAccountIDs to set
	 */
	private void setAcceptAccountIDs(final boolean acceptAccountIDs) {
		this.acceptAccountIDs = acceptAccountIDs;
	}

	/**
	 * Does the client accept JOINBATTLEREQUEST command?
	 * @return the handleBattleJoinAuthorization
	 */
	public boolean isHandleBattleJoinAuthorization() {
		return handleBattleJoinAuthorization;
	}

	/**
	 * Does the client accept JOINBATTLEREQUEST command?
	 */
	private void setHandleBattleJoinAuthorization(
			final boolean handleBattleJoinAuthorization)
	{
		this.handleBattleJoinAuthorization = handleBattleJoinAuthorization;
	}

	/**
	 * Does the client accept the scriptPassord argument to the
	 * JOINEDBATTLE command?
	 */
	public boolean isScriptPassordSupported() {
		return scriptPasswordSupported;
	}

	/**
	 * Does the client accept the scriptPassord argument to the
	 * JOINEDBATTLE command?
	 */
	private void setScriptPassordSupported(final boolean supported) {
		scriptPasswordSupported = supported;
	}

	/**
	 * How much data did this client send to us since he logged in.
	 * This is used with anti-flood protection.
	 * @return the number of bytes received from this client since login.
	 */
	public long getReceivedSinceLogin() {
		return receivedSinceLogin;
	}

	/**
	 * Adds to the number of bytes received from this client.
	 * @param nBytes to add number of bytes
	 */
	public void addReceived(final long nBytes) {

		receivedSinceLogin += nBytes;
	}

	/**
	 * A list of compatibility-flags, each representing a certain minor change
	 * in protocol since the last protocol version number release.
	 * These features all have to be implemented in a backwards compatible way,
	 * so lobby clients not supporting them are still able to function normally.
	 * This is comparable to IRC user/channel flags.
	 * By default, all the optional functionalities are considered
	 * as not supported by the client.
	 *
	 * See the "Recent Changes" section or the LOGIN command
	 * in the lobby protocol documentation for a list of the current flags.
	 * @return the compatFlags
	 */
	public List<String> getCompatFlags() {
		return compatFlags;
	}

	/**
	 * A list of compatibility-flags, each representing a certain minor change
	 * in protocol since the last protocol version number release.
	 * @see #getCompatFlags
	 * @param compatFlags the compatFlags to set
	 */
	public void setCompatFlags(final List<String> compatFlags) {

		this.compatFlags = Collections.unmodifiableList(compatFlags);

		// protocol version 0.37-SNAPSHOT (after 0.36) compat-flags:
		setAcceptAccountIDs(compatFlags.contains("a"));
		setHandleBattleJoinAuthorization(compatFlags.contains("b"));
		setScriptPassordSupported(compatFlags.contains("sp"));
	}
}
