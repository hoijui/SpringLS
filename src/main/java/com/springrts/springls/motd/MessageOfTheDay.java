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

package com.springrts.springls.motd;


import com.springrts.springls.Client;
import com.springrts.springls.Context;
import com.springrts.springls.ContextReceiver;
import com.springrts.springls.util.Misc;
import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores and processes the "message of the day" (MOTD).
 * It can be set by an administrator, and is sent to clients when they log in.
 * @author Betalord
 * @author hoijui
 */
public class MessageOfTheDay implements ContextReceiver {

	private static final Logger LOG
			= LoggerFactory.getLogger(MessageOfTheDay.class);

	private static final String DEFAULT_TEXT = "Enjoy your stay :-)";
	private static final String DEFAULT_FILENAME = "motd.txt";


	private Context context;

	private String message;

	public MessageOfTheDay() {

		context = null;
		message = DEFAULT_TEXT;
	}

	@Override
	public void receiveContext(final Context context) {
		this.context = context;
	}

	protected Context getContext() {
		return context;
	}

	/**
	 * Reads MOTD from the default file from disk.
	 * @see #read(String fileName)
	 */
	public boolean read() {
		return read(DEFAULT_FILENAME);
	}
	/**
	 * Reads MOTD from disk (if file is found).
	 * @return true if read was successful
	 */
	public boolean read(final String fileName) {

		boolean success;

		try {
			message = Misc.readTextFile(new File(fileName));
			success = true;
			LOG.info("Using MOTD from file '{}'.", fileName);
		} catch (final IOException ex) {
			LOG.warn("Could not find or read from file '{}'; reason: {}."
					+ " -> Using the default MOTD.", fileName, ex.getMessage());
			success = false;
		}

		return success;
	}

	/**
	 * Sends MOTD to a client.
	 * @return true if sent successfully
	 */
	public boolean sendTo(final Client client) {

		client.beginFastWrite();
		client.sendLine(String.format("MOTD Welcome, %s!",
				client.getAccount().getName()));
		// -1 is because we should not count the client to which we are sending
		// the MOTD
		client.sendLine(String.format(
				"MOTD There are currently %d clients connected",
				getContext().getClients().getClientsSize() - 1));
		client.sendLine(String.format(
				"MOTD to the server, talking in %d open channels and",
				getContext().getChannels().getChannelsSize()));
		client.sendLine(String.format("MOTD participating in %d battles.",
				getContext().getBattles().getBattlesSize()));
		client.sendLine(String.format("MOTD Server's uptime is %s.",
				Misc.timeToDHM(context.getServer().getUpTime())));
		client.sendLine("MOTD");
		final String[] lines = message.split(Misc.EOL);
		for(final String line : lines){
			client.sendLine(String.format("MOTD %s", line));
		}
		client.endFastWrite();

		return true;
	}
}
