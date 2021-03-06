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
import com.springrts.springls.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("CREATEACCOUNT")
public class CreateAccountCommandProcessor extends AbstractCommandProcessor {

	public CreateAccountCommandProcessor() {
		super(2, 2, Account.Access.ADMIN);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = false;
		try {
			checksOk = super.process(client, args);
		} catch (InvalidNumberOfArgumentsCommandProcessingException ex) {
			client.sendLine("SERVERMSG bad params");
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		String username = args.get(0);
		String password = args.get(1);

		String valid = Account.isOldUsernameValid(username);
		if (valid != null) {
			client.sendLine(String.format(
					"SERVERMSG Invalid username (reason: %s)", valid));
			return false;
		}

		// validate password:
		valid = Account.isPasswordValid(password);
		if (valid != null) {
			client.sendLine(String.format(
					"SERVERMSG Invalid password (reason: %s)", valid));
			return false;
		}
		Account acc = getContext().getAccountsService().findAccountNoCase(username);
		if (acc != null) {
			client.sendLine("SERVERMSG Account already exists");
			return false;
		}
		if (Account.RESERVED_NAMES.contains(username)) {
			client.sendLine("SERVERMSG Invalid account name - you are trying"
					+ " to register a reserved account name");
			return false;
		}
		acc = new Account(username, password, client.getIp(), client.getCountry());
		getContext().getAccountsService().addAccount(acc);
		// let's save new accounts info to disk
		getContext().getAccountsService().saveAccounts(false);
		client.sendLine("SERVERMSG Account created.");
		return true;
	}
}
