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
 * @author hoijui
 */
@SupportedCommand("REMOVEACCOUNT")
public class RemoveAccountCommandProcessor extends AbstractCommandProcessor {

	public RemoveAccountCommandProcessor() {
		super(1, 1, Account.Access.ADMIN);
	}

	@Override
	public boolean process(final Client client, final List<String> args)
			throws CommandProcessingException
	{
		final boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		final String username = args.get(0);

		if (!getContext().getAccountsService().removeAccount(username)) {
			return false;
		}

		// if any user is connected to this account, kick him:
		for (int j = 0; j < getContext().getClients().getClientsSize(); j++) {
			final Client curClient = getContext().getClients().getClient(j);
			if (curClient.getAccount().getName().equals(username)) {
				getContext().getClients().killClient(curClient);
				j--;
			}
		}

		// let's save new accounts info to disk
		getContext().getAccountsService().saveAccounts(false);
		client.sendLine(String.format(
				"SERVERMSG You have successfully removed <%s> account!",
				username));

		return true;
	}
}
