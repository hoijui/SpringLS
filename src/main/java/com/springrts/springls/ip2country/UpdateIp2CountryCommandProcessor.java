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

package com.springrts.springls.ip2country;


import com.springrts.springls.Account;
import com.springrts.springls.Client;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;

/**
 * Lets an administrator re-initialize the internal IP2Country database from a
 * specified data-source file.
 * NOTE This command is SpringLS specific, not part of the official lobby protocol!
 * @author hoijui
 */
@SupportedCommand("UPDATEIP2COUNTRY")
public class UpdateIp2CountryCommandProcessor extends AbstractCommandProcessor {

	public UpdateIp2CountryCommandProcessor() {
		super(Account.Access.ADMIN);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final IP2Country service = getContext().getService(IP2Country.class);
		if (service == null) {
			client.sendLine("SERVERMSG IP2Country service not available!");
		} else if (service.updateInProgress()) {
			client.sendLine("SERVERMSG IP2Country database update is"
					+ " already in progress, try again later.");
		} else {
			client.sendLine("SERVERMSG Updating IP2Country database."
					+ " The server will notify of success via the server"
					+ " notification system.");
			service.updateDatabase();
		}
	}
}
