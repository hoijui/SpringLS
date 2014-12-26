/*
	Copyright (c) 2012 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.springls.commands.impl;


import com.springrts.springls.Account;
import com.springrts.springls.Client;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * Sent by a client that is battle host or lobby moderator,
 * to request a user joining a currently running game.
 * This is useful for <i>Decoupling games from battle-rooms</i>
 * (you may want to read this section in the protocol documentation header.
 * @author hoijui
 */
@SupportedCommand("CONNECTUSER")
public class ConnectUserCommandProcessor extends AbstractCommandProcessor {

	public ConnectUserCommandProcessor() {
		super(2, 3, Account.Access.NORMAL);
	}

	@Override
	public boolean process(final Client client, final List<String> args)
			throws CommandProcessingException
	{
		final boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		final String userName = args.get(0);
		final Client affectedClient
				= getContext().getClients().getClient(userName);
		if (affectedClient == null) {
			client.sendLine(String.format(
					"CONNECTUSERFAILED %s %s", userName,
					"An invalid user name was specified"));
			return false;
		}

		if (affectedClient.isInGame()) {
			client.sendLine(String.format(
					"CONNECTUSERFAILED %s %s", userName,
					"The affected client is currently in-game,"
					+ " and can therefore not connect to an other game"));
			return false;
		}

		final boolean clientSupportsCmd = affectedClient.getCompatFlags().contains("cu"); // NOTE lobby protocol "0.35+ cu"
		if (!clientSupportsCmd) {
			client.sendLine(String.format(
					"CONNECTUSERFAILED %s %s",
					userName,
					"The affected client does not support the command"
					+ " CONNECTUSERFAILED (comp-flag \"cu\")"));
			return false;
		}

		final String ipAndPort = args.get(1);

		final String scriptPassword = (args.size() > 1) ? args.get(2) : null;

		final String successResponseMessage = (scriptPassword == null)
				? String.format("CONNECTUSER %s", ipAndPort)
				: String.format("CONNECTUSER %s %s", ipAndPort, scriptPassword);

		affectedClient.sendLine(successResponseMessage);

		return true;
	}
}
