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
import com.springrts.springls.ServerConfiguration;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;

/**
 * Allows a user to change his password.
 * @author hoijui
 */
@SupportedCommand("CHANGEPASSWORD")
public class ChangePasswordCommandProcessor extends AbstractCommandProcessor {

	public ChangePasswordCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("oldPassword"),
						new Argument("newPassword")),
				Account.Access.NORMAL);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final String oldPassword = (String)args.getWords().get(0);
		final String newPassword = (String)args.getWords().get(1);

		if (getConfiguration().getBoolean(ServerConfiguration.LAN_MODE)) {
			processingError(client, String.format(
					"%s failed: You can not change your password"
					+ " while the server is running in LAN mode!",
					getCommandName()));
			return;
		}

		if (!oldPassword.equals(client.getAccount().getPassword())) {
			processingError(client, String.format(
					"%s failed: The old password is incorrect!",
					getCommandName()));
			return;
		}

		// validate password:
		final String valid = Account.isPasswordValid(newPassword);
		if (valid != null) {
			processingError(client, String.format(
					"%s failed: Invalid password (reason: %s)",
					getCommandName(), valid));
			return;
		}

		final String oldPasswd = client.getAccount().getPassword();
		client.getAccount().setPassword(newPassword);
		final boolean mergeOk = getContext().getAccountsService()
				.mergeAccountChanges(client.getAccount(),
				client.getAccount().getName());
		if (!mergeOk) {
			client.getAccount().setPassword(oldPasswd);
			processingError(client, String.format(
					"%s failed: Failed saving to persistent storage.",
					getCommandName()));
			return;
		}

		// let's save new accounts info to disk
		getContext().getAccountsService().saveAccounts(false);
		client.sendLine("SERVERMSG Your password has been successfully updated!"
				);
	}
}
