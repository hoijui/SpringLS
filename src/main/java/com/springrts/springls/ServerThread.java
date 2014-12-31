/*
	Copyright (c) 2011 Robin Vobruba <hoijui.quaero@gmail.com>

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


import com.springrts.springls.accounts.AccountsService;
import com.springrts.springls.util.Misc;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.CommandProcessor;
import com.springrts.springls.floodprotection.FloodProtectionService;
import com.springrts.springls.nat.NatHelpServer;
import com.springrts.springls.util.ProtocolUtil;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.configuration.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the thread that handles connections by and messages sent from clients.
 * @author Betalord
 * @author hoijui
 */
public class ServerThread implements ContextReceiver, LiveStateListener, Updateable {

	private static final Logger LOG = LoggerFactory.getLogger(ServerThread.class);

	private static class DeprecatedCommand {

		private final String name;
		private final String message;

		DeprecatedCommand(final String name, final String message) {

			this.name = name;
			this.message = message;
		}

		public String getName() {
			return name;
		}

		public String getMessage() {
			return message;
		}
	}

	/**
	 * Contains a list of deprecated commands, for example:
	 * "WHITELIST" -> "deprecated feature: white-listing"
	 */
	private final Map<String, DeprecatedCommand> deprecatedCommands;

	/**
	 * The size of the ByteBuffer used to read data from the socket channel.
	 * This size does not really matter, as the server will work with any size
	 * (tested with READ_BUFFER_SIZE==1), but too small buffer size may impact
	 * the performance.
	 */
	private static final int READ_BUFFER_SIZE = 256;
	/**
	 * The socket's send buffer size.
	 */
	private static final int SEND_BUFFER_SIZE = 8192 * 2;
	private static final long MAIN_LOOP_SLEEP = 10L;

	private Context context;
	private ServerSocketChannel sSockChan;
	private Selector readSelector;
	private boolean running;
	/**
	 * See
	 * <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/nio/ByteBuffer.html">
	 * ByteBuffer JavaDoc</a> for the difference between direct and non-direct
	 * buffers. In this case, we should use direct buffers. They are also used
	 * by the author of the <code>java.nio</code> chat example (see links) upon
	 * which this code is built on.
	 */
	private final ByteBuffer readBuffer;
	private final List<Updateable> updateables;
	private UpdateableTracker updateableTracker;


	public ServerThread() {

		this.deprecatedCommands = createDefaultDeprecatedCommands();
		this.context = null;
		this.readBuffer = ByteBuffer.allocateDirect(READ_BUFFER_SIZE);
		this.updateables = new ArrayList<Updateable>();
	}

	private static void add(
			final Map<String, DeprecatedCommand> deprecatedCommands,
			final DeprecatedCommand command)
	{
		deprecatedCommands.put(command.getName(), command);
	}

	private static Map<String, DeprecatedCommand> createDefaultDeprecatedCommands() {

		final Map<String, DeprecatedCommand> deprecatedCommands
					= new HashMap<String, DeprecatedCommand>();

		add(deprecatedCommands, new DeprecatedCommand(
				"WHITELIST",
				"IP white-listing is disabled"));
		add(deprecatedCommands, new DeprecatedCommand(
				"UNWHITELIST",
				"IP white-listing is disabled"));
		add(deprecatedCommands, new DeprecatedCommand(
				"RETRIEVELATESTBANLIST",
				"Fetching ban entries is not needed anymore."
				+ " Therefore, this is a no-op now."));
		add(deprecatedCommands, new DeprecatedCommand(
				"OUTPUTDBDRIVERSTATUS",
				"This command is not supported anymore,"
				+ " as JPA is used for DB access for bans."
				+ " Therefore, this is a no-op now."));

		return deprecatedCommands;
	}

	public void addUpdateable(final Updateable updateable) {
		updateables.add(updateable);
	}

	public void removeUpdateable(final Updateable updateable) {
		updateables.remove(updateable);
	}

	@Override
	public void setContext(final Context context) {

		this.context = context;

		// TODO move this into a BundleActivator.start() method
		updateableTracker = new UpdateableTracker(context.getFramework().getBundleContext());
		updateableTracker.open();
		// TODO move this into a BundleActivator.stop() method
//		updateableTracker.close();

		addUpdateable(getContext().getServerThread());
		addUpdateable(getContext().getClients());
		addUpdateable(getContext().getChannels());
	}

	private Context getContext() {
		return context;
	}

	/** Check for new client connections */
	private void acceptNewConnections() {

		try {
			SocketChannel clientChannel;
			// since sSockChan is non-blocking, this will return immediately
			// regardless of whether there is a connection available
			while ((clientChannel = sSockChan.accept()) != null) {
				if (getContext().getServer().isRedirectActive()) {
					LOG.debug("Client redirected to {}: {}",
							getContext().getServer().getRedirectAddress().getHostAddress(),
							clientChannel.socket().getInetAddress().getHostAddress());
					redirectAndKill(clientChannel.socket());
					continue;
				}

				final Client client = getContext().getClients().addNewClient(
						clientChannel, readSelector, SEND_BUFFER_SIZE);
				if (client == null) {
					continue;
				}

				// from this point on, we know that client
				// has been successfully connected
				client.sendWelcomeMessage();

				LOG.debug("New client connected: {}",
						client.getIp().getHostAddress());
			}
		} catch (final Exception ex) {
			LOG.error("Exception in acceptNewConnections(): " + ex.getMessage(), ex);
		}
	}

	public boolean redirectAndKill(final Socket socket) {
		if (!context.getServer().isRedirectActive()) {
			return false;
		}
		try {
			(new PrintWriter(socket.getOutputStream(), true)).println(
					String.format("REDIRECT %s",
					context.getServer().getRedirectAddress().getHostAddress()));
			socket.close();
		} catch (final IOException ex) {
			return false;
		}
		return true;
	}

	/** Check for incoming messages */
	private void readIncomingMessages() {

		Client client = null;
		try {
			// non-blocking select, returns immediately regardless of
			// how many keys are ready
			readSelector.selectNow();

			// fetch the keys
			final Set<SelectionKey> readyKeys = readSelector.selectedKeys();

			// run through the keys and process each one
			while (!readyKeys.isEmpty()) {
				final SelectionKey key = readyKeys.iterator().next();
				readyKeys.remove(key);
				final SocketChannel channel = (SocketChannel) key.channel();
				client = (Client) key.attachment();
				if (client.isHalfDead()) {
					continue;
				}
				readBuffer.clear();

				client.setTimeOfLastReceive(System.currentTimeMillis());

				// read from the channel into our buffer
				final long nBytes = channel.read(readBuffer);
				client.addReceived(nBytes);

				// basic anti-flood protection
				final FloodProtectionService floodProtection
						= getContext().getService(FloodProtectionService.class);
				if ((floodProtection != null)
						&& floodProtection.isFlooding(client))
				{
					continue;
				}

				// check for end-of-stream
				if (nBytes == -1) {
					LOG.debug("Socket disconnected - killing client");
					channel.close();
					// this will also close the socket channel
					getContext().getClients().killClient(client);
				} else {
					// use a CharsetDecoder to turn those bytes into a string
					// and append it to the client's StringBuilder
					readBuffer.flip();
					final String str = getContext().getServer().getAsciiDecoder().decode(readBuffer).toString();
					readBuffer.clear();
					client.appendToRecvBuf(str);

					// TODO move this to Client#appendToRecvBuf(String)
					// check for a full line
					String line = client.readLine();
					while (line != null) {
						executeCommandWrapper(line, client);

						if (!client.isAlive()) {
							// in case the client was killed within the
							// executeCommand() method
							break;
						}
						line = client.readLine();
					}
				}
			}
		} catch (final IOException ioex) {
			LOG.info("exception during select(): possibly due to force disconnect. Killing the client ...");
			if (client != null) {
				getContext().getClients().killClient(client, "Quit: connection lost");
			}
			LOG.debug("... the exception was:", ioex);
		}
	}

	private void executeCommandWrapper(final String command, final Client client) {

		long time = System.currentTimeMillis();
		executeCommand(command, client);
		time = System.currentTimeMillis() - time;
		if (time > 200) {
			final String message = String.format(
					"SERVERMSG [broadcast to all admins]: (DEBUG) User <%s>"
					+ " caused %d ms load on the server. Command issued: %s",
					client.getAccount().getName(), time, command);
			getContext().getClients().sendToAllAdministrators(message);
		}
	}

	/**
	 * Note: this method is not synchronized!
	 * Note2: this method may be called recursively!
	 */
	public boolean executeCommand(final String command, final Client client) {

		String commandClean = command.trim();
		if (commandClean.isEmpty()) {
			return false;
		}

		if (LOG.isTraceEnabled()) {
			LOG.trace("[<-{}] \"{}\"",
					(client.getAccount().getAccess() != Account.Access.NONE)
						? client.getAccount().getName()
						: client.getIp().getHostAddress(),
					commandClean);
		}

		int msgId = Client.NO_MSG_ID;
		if (commandClean.charAt(0) == '#') {
			try {
				if (!commandClean.matches("^#\\d+\\s[\\s\\S]*")) {
					return false; // malformed command
				}
				msgId = Integer.parseInt(commandClean.substring(1).split("\\s")[0]);
				// remove id field from the rest of command:
				commandClean = commandClean.replaceFirst("#\\d+\\s", "");
			} catch (final NumberFormatException ex) {
				return false; // this means that the command is malformed
			} catch (final PatternSyntaxException ex) {
				return false; // this means that the command is malformed
			}
		}

		// parse command into tokens:
		final String[] commands = commandClean.split(" ");
		commands[0] = commands[0].toUpperCase();

		client.setSendMsgId(msgId);

		try {
			final CommandProcessor cmdProcessor = getContext().getCommandProcessors().get(commands[0]);
			if (cmdProcessor != null) {
				final List<String> args = new ArrayList<String>(Arrays.asList(commands));
				args.remove(0);
				try {
					final boolean ret = cmdProcessor.process(client, args);
					if (!ret) {
						return false;
					}
				} catch (final CommandProcessingException ex) {
					LOG.debug(cmdProcessor.getClass().getCanonicalName()
							+ " failed to handle command from client: \""
							+ Misc.makeSentence(commands) + "\"", ex);
					return false;
				}
			} else if (deprecatedCommands.containsKey(commands[0])) {
				final DeprecatedCommand deprecatedCommand = deprecatedCommands.get(commands[0]);
				client.sendLine(String.format(
						"SERVERMSG Command %s is deprecated: %s",
						deprecatedCommand.getName(),
						deprecatedCommand.getMessage()));
			} else {
				// unknown command!
				return false;
			}
		} finally {
			client.setSendMsgId(Client.NO_MSG_ID);
		}

		return true;
	}


	@Override
	public void update() {

		acceptNewConnections();

		readIncomingMessages();
	}

	public void run() {

		running = true;
		while (running) { // main loop

			for (final Updateable updateable : updateables) {
				updateable.update();
			}

			// sleep a bit
			try {
				Thread.sleep(MAIN_LOOP_SLEEP);
			} catch (final InterruptedException iex) {
			}
		}

		getContext().stopping();

		// close everything:
		getContext().getAccountsService().saveAccounts(true);
		final NatHelpServer natHelpServer = getContext().getService(NatHelpServer.class);
		if ((natHelpServer != null) && natHelpServer.isRunning()) {
			natHelpServer.stopServer();
		}

		// add server notification:
		final ServerNotification srvNotif = new ServerNotification("Server stopped");
		srvNotif.addLine("Server has just been stopped gracefully. See server log for more info.");
		getContext().getServerNotifications().addNotification(srvNotif);

		LOG.info("Server closed gracefully!");

		getContext().stopped();
	}

	public boolean startServer() {

		context.starting();

		final Configuration configuration =
				getContext().getService(Configuration.class);
		final int port = configuration.getInt(ServerConfiguration.PORT);

		try {
			context.getServer().setCharset("ISO-8859-1");

			// open a non-blocking server socket channel
			sSockChan = ServerSocketChannel.open();
			sSockChan.configureBlocking(false);

			// bind to localhost on designated port
			//***InetAddress addr = InetAddress.getLocalHost();
			//***sSockChan.socket().bind(new InetSocketAddress(addr, port));
			sSockChan.socket().bind(new InetSocketAddress(port));

			// get a selector for multiplexing the client channels
			readSelector = Selector.open();

		} catch (final IOException ex) {
			LOG.error("Could not listen on port: " + port, ex);
			return false;
		}

		LOG.info("Listening for connections on TCP port {} ...", port);

		context.started();

		return true;
	}

	/**
	 * Shuts down the server gracefully.
	 */
	public void stop() {
		running = false;
	}

	/**
	 * Shuts down the server forcefully.
	 */
	public void closeServerAndExit() {

		// FIXME these things do not get called on normal exit yet!
		//   see (end of) method run()

		//getContext().stopping();

		// add server notification
		if ((getContext() != null)
				&& (getContext().getServerNotifications() != null))
		{
			final ServerNotification srvNotif = new ServerNotification("Server stopped");
			srvNotif.addLine("Server has just been stopped. See server log for more info.");
			getContext().getServerNotifications().addNotification(srvNotif);
		}

		//getContext().stopped();
		LOG.warn("Server stopped forcefully, please see the log for details");

		System.exit(127);
	}


	@Override
	public void starting() {
		LOG.info("starting...");
	}
	@Override
	public void started() {

		// As DateFormats are generally not-thread save,
		// we always create a new one.
		final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy.MM.dd 'at' hh:mm:ss z");

		LOG.info("{} {} started on {}",
				new Object[] {
					Server.getApplicationName(),
					Misc.getAppVersionNonNull(),
					dateTimeFormat.format(new Date())
				});

		// add server notification
		final ServerNotification srvNotif
				= new ServerNotification("Server started");
		final Configuration conf = getContext().getService(Configuration.class);
		final int port = conf.getInt(ServerConfiguration.PORT);
		srvNotif.addLine(String.format(
				"Server has been started on port %d."
				+ " There are %d accounts. "
				+ "See server log for more info.",
				port,
				context.getAccountsService().getAccountsSize()));
		context.getServerNotifications().addNotification(srvNotif);

		createAdminIfNoUsers();
	}

	/**
	 * Adds a default administrator account, if not running in LAN mode,
	 * and if there are no accounts in the active accounts service.
	 */
	private void createAdminIfNoUsers() {

		final Configuration conf = context.getService(Configuration.class);
		if (!conf.getBoolean(ServerConfiguration.LAN_MODE)) {
			final AccountsService accountsService = context.getAccountsService();
			if (accountsService.getAccountsSize() == 0) {
				final Configuration defaults = ServerConfiguration.getDefaults();
				final String username = defaults.getString(
						ServerConfiguration.LAN_ADMIN_USERNAME);
				final String password = defaults.getString(
						ServerConfiguration.LAN_ADMIN_PASSWORD);
				LOG.info("As there are no accounts yet, we are creating an"
						+ " admin account: username=\"{}\", password=\"{}\"",
						username, password);
				final Account admin = createAdmin(username, password);
				accountsService.addAccount(admin);
				accountsService.saveAccountsIfNeeded();
			}
		}
	}

	/**
	 * Creates a simple account with administrator rights.
	 */
	private static Account createAdmin(final String username, final String password) {

		final Account admin = new Account(username, ProtocolUtil.encodePassword(password));
		admin.setAccess(Account.Access.ADMIN);
		return admin;
	}

	@Override
	public void stopping() {
		LOG.info("Server stopping ...");
	}
	@Override
	public void stopped() {

		// As DateFormats are generally not-thread save,
		// we always create a new one.
		final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy.MM.dd 'at' hh:mm:ss z");

		running = false;
		LOG.info("Server stopped on {}", dateTimeFormat.format(new Date()));
	}
}
