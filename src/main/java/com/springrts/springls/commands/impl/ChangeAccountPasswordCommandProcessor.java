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
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("CHANGEACCOUNTPASS")
public class ChangeAccountPasswordCommandProcessor
		extends AbstractCommandProcessor
{
	public ChangeAccountPasswordCommandProcessor() {
		super(2, 2, Account.Access.ADMIN);
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
		String password = args.get(1);

		Account acc = getContext().getAccountsService().getAccount(username);
		if (acc == null) {
			return false;
		}
		// validate password:
		if (Account.isPasswordValid(password) != null) {
			return false;
		}

		final String oldPasswd = acc.getPassword();
		acc.setPassword(password);
		final boolean mergeOk = getContext().getAccountsService()
				.mergeAccountChanges(acc, acc.getName());
		if (!mergeOk) {
			acc.setPassword(oldPasswd);
			client.sendLine(String.format(
					"SERVERMSG %s failed: Failed saving to persistent storage.",
					getCommandName()));
			return false;
		}

		getContext().getAccountsService().saveAccounts(false); // save changes

		// add server notification:
		ServerNotification sn = new ServerNotification(
				"Account password changed by admin");
		sn.addLine(String.format(
				"Admin <%s> has changed password for account <%s>",
				client.getAccount().getName(), acc.getName()));
		getContext().getServerNotifications().addNotification(sn);

		return true;
	}
}
