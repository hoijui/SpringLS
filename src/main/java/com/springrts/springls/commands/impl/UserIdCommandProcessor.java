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
import com.springrts.springls.ServerNotification;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;

/**
 * Allows a user to set a new user ID for himself.
 * @author hoijui
 */
@SupportedCommand("USERID")
@Deprecated
public class UserIdCommandProcessor extends AbstractCommandProcessor {

	public UserIdCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("userId")),
				Account.Access.NORMAL);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		if (client.getCompatFlags().contains("cl")) { // NOTE lobby protocol "0.36+ cl"
			processingError(client, "Command " + getCommandName()
					+ " was removed after lobby protocol version 0.36"
					+ " with the 'cl' flag");
		}

		// TODO revise this parsing "algorithm", and see if it needs to be as complex, and how userId is parsed in other commands, and if it is differently defined (in the protocol spec.) for those other commands
		final String userIdStr = (String)args.getWords().get(0);
		int userId = Account.NO_USER_ID;
		try {
			final long tempUserId = Long.parseLong(userIdStr, 16);
			// we transform an unsigned 32 bit integer to a signed one
			userId = (int) tempUserId;
		} catch (final NumberFormatException ex) {
			processingError(client, "Bad USERID command - userID field should be an integer");
		}

		client.getAccount().setLastUserId(userId);
		final boolean mergeOk
				= getContext().getAccountsService().mergeAccountChanges(
						client.getAccount(),
						client.getAccount().getName());
		if (!mergeOk) {
			// FIXME set back?
			processingError(client, "Failed saving last User ID to persistent storage");
		}

		// add server notification:
		final ServerNotification srvNotif = new ServerNotification("User ID received");
		srvNotif.addLine(String.format("<%s> has generated a new user ID: %s(%d)",
				client.getAccount().getName(), userIdStr, userId));
		getContext().getServerNotifications().addNotification(srvNotif);
	}
}
