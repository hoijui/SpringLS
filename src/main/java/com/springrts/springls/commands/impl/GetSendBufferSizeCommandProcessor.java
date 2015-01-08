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
 * Lets an administrator fetch the size of the send-buffer for a specific
 * client.
 * @author hoijui
 */
@SupportedCommand("GETSENDBUFFERSIZE")
public class GetSendBufferSizeCommandProcessor extends AbstractCommandProcessor {

	public GetSendBufferSizeCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("username")),
				Account.Access.ADMIN);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final String username = (String)args.getWords().get(0);

		final Client c = getContext().getClients().getClient(username);
		if (c == null) {
			processingError(client, String.format(
					"Error: user <%s> not found online!", username));
		}

		final int size;
		try {
			size = c.getSockChan().socket().getSendBufferSize();
		} catch (final Exception ex) {
			// this could perhaps happen if user just disconnected or something
			processingError(client, String.format(
					"Error while trying to get send buffer size for"
					+ " <%s>!", username));
		}

		client.sendLine(String.format(
				"SERVERMSG Send buffer size for <%s> is set to %d bytes.",
				c.getAccount().getName(), size));
	}
}
