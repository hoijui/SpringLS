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
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * Lets an administrator fetch the lobby-version of a specified client.
 * @author hoijui
 */
@SupportedCommand("GETLOBBYVERSION")
public class GetLobbyVersionCommandProcessor extends AbstractCommandProcessor {

	public GetLobbyVersionCommandProcessor() {
		super(1, 1, Account.Access.ADMIN);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		String username = args.get(0);

		Client targetClient = getContext().getClients().getClient(username);
		if (targetClient == null) {
			client.sendLine(String.format("SERVERMSG <%s> not found!",
					username));
			return false;
		}

		client.sendLine(String.format("SERVERMSG <%s> is using \"%s\"",
				username, targetClient.getLobbyVersion()));

		return true;
	}
}
