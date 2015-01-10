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
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;

/**
 * Allows an administrator to set/unset the bot mode on an account.
 * @author hoijui
 */
@SupportedCommand("SETBOTMODE")
public class SetBotModeCommandProcessor extends AbstractCommandProcessor {

	public SetBotModeCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("userName"),
						new Argument("mode", Integer.class, Argument.PARSER_TO_INTEGER)),
				Account.Access.ADMIN);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final int mode = (Integer)args.getWords().get(1);
		if ((mode != 0) && (mode != 1)) {
			processingError(client, "Invalid 'mode' parameter (has to be 0 or 1)!");
			return;
		}

		final String userName = (String)args.getWords().get(0);

		final Account acc
				= getContext().getAccountsService().getAccount(userName);
		if (acc == null) {
			processingError(client, String.format("User <%s> not found!",
					userName));
			return;
		}

		final boolean wasBot = acc.isBot();
		final boolean isBot = (mode == 1);
		acc.setBot(isBot);
		final boolean mergeOk = getContext().getAccountsService()
				.mergeAccountChanges(acc, acc.getName());
		if (!mergeOk) {
			acc.setBot(wasBot);
			processingError(client, String.format(
					"%s failed: Failed saving to persistent storage.",
					getCommandName()));
			return;
		}

		client.sendLine(String.format(
				"SERVERMSG Bot mode set to %d for user <%s>",
				mode, acc.getName()));
	}
}
