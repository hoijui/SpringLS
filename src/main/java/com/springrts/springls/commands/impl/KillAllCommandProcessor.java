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

package com.springrts.springls.commands.impl;


import com.springrts.springls.Account;
import com.springrts.springls.Client;
import com.springrts.springls.Clients;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.util.Collections;

/**
 * Allows the administrator to kick all currently connected clients.
 * @author hoijui
 */
@SupportedCommand("KILLALL")
public class KillAllCommandProcessor extends AbstractCommandProcessor {

	public KillAllCommandProcessor() {
		super(
				new CommandArguments(
						Collections.EMPTY_LIST,
						new Argument("reason", true)
						),
				Account.Access.ADMIN);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		String reason = "Disconnected by server";
		if (!args.getSentences().isEmpty()) {
			reason += ", reason: " + (String)args.getSentences().get(0);
		}

		final Clients clients = getContext().getClients();
		while (clients.getClientsSize() > 0) {
			clients.killClient(clients.getClient(0), reason);
		}
	}
}
