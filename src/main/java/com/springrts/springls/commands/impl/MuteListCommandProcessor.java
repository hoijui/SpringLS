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
import com.springrts.springls.Channel;
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
@SupportedCommand("MUTELIST")
public class MuteListCommandProcessor extends AbstractCommandProcessor {

	public MuteListCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("channelName")),
				Account.Access.NORMAL);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final String channelName = (String)args.getWords().get(0);

		final Channel chan = getContext().getChannels().getChannel(channelName);
		if (chan == null) {
			processingError(client, String.format(
					"MUTELIST failed: Channel #%s does not exist!",
					channelName));
		}

		client.sendLine(String.format("MUTELISTBEGIN %s", chan.getName()));

		final int size = chan.getMuteList().size(); // we mustn't call muteList.size() in for loop since it will purge expired records each time and so we could have ArrayOutOfBounds exception
		for (int i = 0; i < size; i++) {
			if (chan.getMuteList().getRemainingSeconds(i) == 0) {
				client.sendLine(String.format(
						"MUTELIST %s, indefinite time remaining",
						chan.getMuteList().getUsername(i)));
			} else {
				client.sendLine(String.format(
						"MUTELIST %s, %d seconds remaining",
						chan.getMuteList().getUsername(i),
						chan.getMuteList().getRemainingSeconds(i)));
			}
		}

		client.sendLine("MUTELISTEND");
	}
}
