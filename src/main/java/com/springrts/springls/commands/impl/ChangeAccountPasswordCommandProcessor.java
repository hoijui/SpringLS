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
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;

/**
 * @author hoijui
 */
@SupportedCommand("CHANGEACCOUNTPASS")
public class ChangeAccountPasswordCommandProcessor
		extends AbstractCommandProcessor
{
	public ChangeAccountPasswordCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("username"),
						new Argument("password")),
				Account.Access.ADMIN);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final String username = (String)args.getWords().get(0);
		final String password = (String)args.getWords().get(1);

		final Account account
				= getContext().getAccountsService().getAccount(username);
		if (account == null) {
			return false;
		}
		// validate password:
		if (Account.isPasswordValid(password) != null) {
			return false;
		}

		final String oldPasswd = account.getPassword();
		account.setPassword(password);
		final boolean mergeOk = getContext().getAccountsService()
				.mergeAccountChanges(account, account.getName());
		if (!mergeOk) {
			account.setPassword(oldPasswd);
			processingError(client, String.format(
					"%s failed: Failed saving to persistent storage.",
					getCommandName()));
		}

		getContext().getAccountsService().saveAccounts(false); // save changes

		// add server notification:
		final ServerNotification srvNotif = new ServerNotification(
				"Account password changed by admin");
		srvNotif.addLine(String.format(
				"Admin <%s> has changed password for account <%s>",
				client.getAccount().getName(), account.getName()));
		getContext().getServerNotifications().addNotification(srvNotif);
	}
}
