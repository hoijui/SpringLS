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

package com.springrts.springls.motd;


import com.springrts.springls.Account;
import com.springrts.springls.Client;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lets an administrator update the "message of the day" (MOTD) from a server
 * local text file.
 * @author hoijui
 */
@SupportedCommand("UPDATEMOTD")
public class UpdateMotdCommandProcessor extends AbstractCommandProcessor {

	private static final Logger LOG
			= LoggerFactory.getLogger(UpdateMotdCommandProcessor.class);

	public UpdateMotdCommandProcessor() {
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

		final String motdFileName = args.get(0);

		final MessageOfTheDay messageOfTheDay
				= getContext().getService(MessageOfTheDay.class);
		if (messageOfTheDay == null) {
			client.sendLine("SERVERMSG MOTD Error: service is not available");
			LOG.error("MessageOfTheDay service not available");
		} else {
			if (messageOfTheDay.read(motdFileName)) {
				client.sendLine(String.format(
						"SERVERMSG MOTD has been successfully updated from %s",
						motdFileName));
			} else {
				client.sendLine(String.format(
						"SERVERMSG Error: unable to read MOTD from %s",
						motdFileName));
				return false;
			}
		}

		return true;
	}
}
