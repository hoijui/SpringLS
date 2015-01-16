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
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.InsufficientAccessCommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;

/**
 * @author hoijui
 */
@SupportedCommand("GETINGAMETIME")
public class GetInGameTimeCommandProcessor extends AbstractCommandProcessor {

	public GetInGameTimeCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("username", true)),
				Account.Access.NORMAL);
	}

	@Override
	public boolean process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		if (args.getWords().isEmpty()) {
			client.sendLine(String.format(
					"SERVERMSG Your in-game time is %d minutes.",
					client.getAccount().getInGameTimeInMins()));
		} else {
			if (client.getAccount().getAccess().isLessThen(Account.Access.PRIVILEGED)) {
				client.sendLine("SERVERMSG You have no access to see other player's in-game time!");
				throw new InsufficientAccessCommandProcessingException(
						getCommandName(), Account.Access.PRIVILEGED,
						client.getAccount().getAccess());
			}

			final String username = (String)args.getWords().get(0);

			final Account account
					= getContext().getAccountsService().getAccount(username);
			if (account == null) {
				client.sendLine(String.format(
						"SERVERMSG %s failed: user %s not found!",
						getCommandName(), username));
				return false;
			}

			client.sendLine(String.format(
					"SERVERMSG %s's in-game time is %d minutes.",
					account.getName(), account.getInGameTimeInMins()));
		}

		return true;
	}
}
