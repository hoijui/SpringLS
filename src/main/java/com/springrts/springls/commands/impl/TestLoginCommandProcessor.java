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
 * Allows an administrator to check a pair of login credentials for validity.
 * @author hoijui
 */
@SupportedCommand("TESTLOGIN")
public class TestLoginCommandProcessor extends AbstractCommandProcessor {

	public TestLoginCommandProcessor() {
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
		final String userName = (String)args.getWords().get(0);
		final String password = (String)args.getWords().get(1);

		if (getContext().getAccountsService().verifyLogin(userName, password)
				== null)
		{
			client.sendLine("TESTLOGINDENY");
			processingError();
		}

		// We don't check here if agreement bit is set yet,
		// or if user is banned.
		// We only verify if login info is correct
		client.sendLine("TESTLOGINACCEPT");
	}
}
