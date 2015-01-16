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
import com.springrts.springls.commands.IndexedArgument;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.util.Arrays;

/**
 * @author hoijui
 */
@SupportedCommand("CHANNELMESSAGE")
public class ChannelMessageCommandProcessor extends AbstractCommandProcessor {

	public ChannelMessageCommandProcessor() {
		super(
				new CommandArguments(Arrays.asList(new IndexedArgument[] {
						new Argument("channelName"),
						}),
						new Argument("message")
						),
				Account.Access.PRIVILEGED);
	}

	@Override
	public boolean process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		final String channelName = (String)args.getWords().get(0);

		final Channel chan = getContext().getChannels().getChannel(channelName);
		if (chan == null) {
			client.sendLine(String.format(
					"SERVERMSG %s failed: Channel #%s does not exist!",
					getCommandName(), channelName));
			return false;
		}

		final String message = (String)args.getSentences().get(0);
		chan.broadcast(message);

		return true;
	}
}
