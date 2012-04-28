/*
	Copyright (c) 2012 Robin Vobruba <hoijui.quaero@gmail.com>

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
 * Will change or, in case there was none previously associated,
 * set the email associated with a user's account.
 * @author hoijui
 */
@SupportedCommand("CHANGEEMAIL")
public class ChangeEmailCommandProcessor extends AbstractCommandProcessor {

	public ChangeEmailCommandProcessor() {
		super(1, 2, Account.Access.NORMAL);
	}

	@Override
	public boolean process(final Client client, final List<String> args)
			throws CommandProcessingException
	{
		final boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		final String newEmail = args.get(0);

		Account toChange = client.getAccount();
		if (args.size() > 1) {
			final String username = args.get(1);
			toChange = getContext().getAccountsService().getAccount(username);
			if (toChange == null) {
				client.sendLine(String.format(
						"SERVERMSG Error: User <%s> not found!", username));
				return false;
			}
			if (!toChange.equals(client.getAccount())
					&& !client.getAccount().getAccess().isAtLeast(Account.Access.PRIVILEGED))
			{
				client.sendLine(
						"SERVERMSG Error: You need to be moderator to change the email of an other user!");
				return false;
			}
		}

		toChange.setEmail(newEmail);

		getContext().getAccountsService().saveAccounts(false); // save changes (non blockingly)

		client.sendLine(String.format(
				"SERVERMSG You have successfully changed the E-Mail address for user <%s>.",
				toChange.getName()));

		return true;
	}
}
