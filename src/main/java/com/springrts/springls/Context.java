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

package com.springrts.springls;


import com.springrts.springls.accounts.AccountsService;
import com.springrts.springls.commands.CommandProcessors;
import java.util.LinkedList;
import java.util.List;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;

/**
 * Contains global instances, unique for per server instance.
 * @author hoijui
 */
public class Context implements LiveStateListener {

	private final List<ContextReceiver> contextReceivers;
	private final List<LiveStateListener> liveStateListeners;

	private Framework framework;
	private AccountsService accountsService;
	private Battles battles;
	private Channels channels;
	private Clients clients;
	private Engine engine;
	private Server server;
	private ServerNotifications serverNotifications;
	private CommandProcessors commandProcessors;
	private ServerThread serverThread;


	public Context() {

		this.contextReceivers = new LinkedList<ContextReceiver>();
		this.liveStateListeners = new LinkedList<LiveStateListener>();

		this.framework = null;
		this.accountsService = null;
		this.battles = null;
		this.channels = null;
		this.clients = null;
		this.engine = null;
		this.server = null;
		this.serverNotifications = null;
		this.commandProcessors = null;
		this.serverThread = null;
	}

	public void init() {

		this.accountsService = null;
		setBattles(new Battles());
		setChannels(new Channels());
		setClients(new Clients());
		setEngine(new Engine());
		setServer(new Server());
		setServerNotifications(new ServerNotifications());
		setCommandProcessors(new CommandProcessors());
		setServerThread(new ServerThread());
	}


	public static <T> T getService(
			final BundleContext bundleContext,
			final Class<T> serviceClass)
	{
		T service = null;

		final ServiceReference serviceReference
				= bundleContext.getServiceReference(serviceClass.getName());

		if (serviceReference != null) {
			service = (T) bundleContext.getService(serviceReference);
		}

		return service;
	}

	public <T> T getService(final Class<T> serviceClass) {
		return getService(getFramework().getBundleContext(), serviceClass);
	}

	public void push() {

		for (final ContextReceiver contextReceiver : contextReceivers) {
			contextReceiver.setContext(this);
		}
	}

	public void addContextReceiver(final ContextReceiver contextReceiver) {

		if (!contextReceivers.contains(contextReceiver)) {
			contextReceivers.add(contextReceiver);
		}
	}

	public void addLiveStateListener(final LiveStateListener liveStateListener) {

		if (!liveStateListeners.contains(liveStateListener)) {
			liveStateListeners.add(liveStateListener);
		}
	}

	@Override
	public void starting() {

		for (final LiveStateListener liveStateListener : liveStateListeners) {
			liveStateListener.starting();
		}
	}
	@Override
	public void started() {

		for (final LiveStateListener liveStateListener : liveStateListeners) {
			liveStateListener.started();
		}
	}

	@Override
	public void stopping() {

		for (final LiveStateListener liveStateListener : liveStateListeners) {
			liveStateListener.stopping();
		}
	}
	@Override
	public void stopped() {

		for (final LiveStateListener liveStateListener : liveStateListeners) {
			liveStateListener.stopped();
		}
	}

	public Framework getFramework() {
		return framework;
	}

	public void setFramework(final Framework framework) {
		this.framework = framework;
	}

	public AccountsService getAccountsService() {
		return accountsService;
	}

	public void setAccountsService(final AccountsService accountsService) {

		this.accountsService = accountsService;
		addContextReceiver(accountsService);
		addLiveStateListener(accountsService);
	}

	public Battles getBattles() {
		return battles;
	}

	public void setBattles(final Battles battles) {

		this.battles = battles;
		addContextReceiver(battles);
	}

	public Channels getChannels() {
		return channels;
	}

	public void setChannels(final Channels channels) {

		this.channels = channels;
		addContextReceiver(channels);
		addLiveStateListener(channels);
	}

	public Clients getClients() {
		return clients;
	}

	public void setClients(final Clients clients) {

		this.clients = clients;
		addContextReceiver(clients);
	}

	public Engine getEngine() {
		return engine;
	}

	public void setEngine(final Engine engine) {

		this.engine = engine;
	}

	public ServerNotifications getServerNotifications() {
		return serverNotifications;
	}

	public void setServerNotifications(
			final ServerNotifications serverNotifications)
	{
		this.serverNotifications = serverNotifications;
		addContextReceiver(serverNotifications);
	}

	public Server getServer() {
		return server;
	}

	public void setServer(final Server server) {

		this.server = server;
	}

	public CommandProcessors getCommandProcessors() {
		return commandProcessors;
	}

	public void setCommandProcessors(final CommandProcessors commandProcessors)
	{
		this.commandProcessors = commandProcessors;
		addContextReceiver(commandProcessors);
	}

	public ServerThread getServerThread() {
		return serverThread;
	}

	public void setServerThread(final ServerThread serverThread) {

		this.serverThread = serverThread;
		addContextReceiver(serverThread);
		addLiveStateListener(serverThread);
	}
}
