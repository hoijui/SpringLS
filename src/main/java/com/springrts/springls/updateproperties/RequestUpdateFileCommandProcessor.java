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

package com.springrts.springls.updateproperties;


import com.springrts.springls.Client;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.util.Collections;

/**
 * The client sends his current engine version, to request a suitable update
 * file.
 * If no update could be found, the client will be killed.
 * @author hoijui
 */
@SupportedCommand("REQUESTUPDATEFILE")
@Deprecated
public class RequestUpdateFileCommandProcessor extends AbstractCommandProcessor {

	public RequestUpdateFileCommandProcessor() {
		super(new CommandArguments(
				Collections.EMPTY_LIST, // words
				new Argument("version")));
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		if (client.getCompatFlags().contains("cl")) { // NOTE lobby protocol "0.36+ cl"
			processingError(client, "Command " + getCommandName()
					+ " was removed after lobby protocol version 0.36"
					+ " with the 'cl' flag");
		}

		final String version = (String)args.getSentences().get(0);
		final UpdateProperties updateProperties = getService(UpdateProperties.class);
		final String response = updateProperties.getResponse(version);

		// send a response to the client:
		client.sendLine(response);

		// kill client if no update has been found for him:
		if (response.substring(0, 12).equalsIgnoreCase("SERVERMSGBOX")) {
			getContext().getClients().killClient(client);
		}
	}
}
