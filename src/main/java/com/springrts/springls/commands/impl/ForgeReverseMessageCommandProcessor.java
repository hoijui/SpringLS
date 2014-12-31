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
import com.springrts.springls.util.Misc;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * This command is used only for debugging purposes. It sends the string
 * to the client specified as first argument as if it were sent by the user
 * specified in this command.
 * @author hoijui
 */
@SupportedCommand("FORGEREVERSEMSG")
@Deprecated
public class ForgeReverseMessageCommandProcessor extends AbstractCommandProcessor {

	public ForgeReverseMessageCommandProcessor() {
		super(2, ARGS_MAX_NOCHECK, Account.Access.ADMIN);
	}

	@Override
	public boolean process(final Client client, final List<String> args)
			throws CommandProcessingException
	{
		final boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		if (client.getCompatFlags().contains("cl")) { // NOTE lobby protocol "0.36+ cl"
			client.sendLine("SERVERMSG Command " + getCommandName()
					+ " was removed after lobby protocol version 0.36"
					+ " with the 'cl' flag");
			return false;
		}

		final String username = args.get(0);
		final String message = Misc.makeSentence(args, 1);

		final Client targetClient = getContext().getClients().getClient(username);
		if (targetClient == null) {
			return false;
		}

		getContext().getServerThread().executeCommand(message, targetClient);

		return true;
	}
}
