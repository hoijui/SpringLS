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
 * @author hoijui
 */
@SupportedCommand("CREATEACCOUNT")
public class CreateAccountCommandProcessor extends AbstractCommandProcessor {

	public CreateAccountCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("userName"),
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

		String valid = Account.isOldUsernameValid(username);
		if (valid != null) {
			processingError(client, String.format(
					"Invalid username (reason: %s)", valid));
			return;
		}

		// validate password:
		valid = Account.isPasswordValid(password);
		if (valid != null) {
			processingError(client, String.format(
					"Invalid password (reason: %s)", valid));
			return;
		}
		Account account
				= getContext().getAccountsService().findAccountNoCase(username);
		if (account != null) {
			processingError(client, "Account already exists");
			return;
		}
		if (Account.RESERVED_NAMES.contains(username)) {
			processingError(client, "Invalid account name - you are trying"
					+ " to register a reserved account name");
			return;
		}
		account = new Account(username, password, client.getIp(),
				client.getCountry());
		getContext().getAccountsService().addAccount(account);
		// let's save new accounts info to disk
		getContext().getAccountsService().saveAccounts(false);
		client.sendLine("SERVERMSG Account created.");
	}
}
