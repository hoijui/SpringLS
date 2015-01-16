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
import com.springrts.springls.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;

/**
 * Allows an administrator to change the latest (stable) engine version,
 * which is generally required to play online.
 * NOTE This command is SpringLS specific, not part of the official lobby protocol!
 * @author hoijui
 */
@SupportedCommand("SETLATESTSPRINGVERSION")
public class SetLatestSpringVersionCommandProcessor extends AbstractCommandProcessor {

	public SetLatestSpringVersionCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("engineVersion")),
				Account.Access.ADMIN);
	}

	@Override
	public boolean process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		boolean checksOk = false;
		try {
			checksOk = super.process(client, args);
		} catch (final InvalidNumberOfArgumentsCommandProcessingException ex) {
			client.sendLine("SERVERMSG Bad arguments to SETLATESTSPRINGVERSION command!");
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		final String engineVersion = (String)args.getWords().get(0);

		getConfiguration().setProperty(ServerConfiguration.ENGINE_VERSION,
				engineVersion);

		client.sendLine("SERVERMSG Latest spring version has been set to "
				+ engineVersion);

		return true;
	}
}
