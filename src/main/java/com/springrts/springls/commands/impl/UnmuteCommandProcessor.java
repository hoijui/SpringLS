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
@SupportedCommand("UNMUTE")
public class UnmuteCommandProcessor extends AbstractCommandProcessor {

	public UnmuteCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("channelName"),
						new Argument("username")),
				Account.Access.PRIVILEGED);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final String channelName = (String)args.getWords().get(0);
		final String username = (String)args.getWords().get(1);

		final Channel chan = getContext().getChannels().getChannel(channelName);
		if (chan == null) {
			processingError(client, String.format(
					"%s failed: Channel #%s does not exist!",
					getCommandName(), channelName));
		}

		if (!chan.getMuteList().isMuted(username)) {
			processingError(client, String.format(
					"%s failed: User <%s> is not on the mute list!",
					getCommandName(), username));
		}

		chan.getMuteList().unmute(username);
		client.sendLine(String.format(
				"SERVERMSG You have unmuted <%s> on channel #%s.",
				username, chan.getName()));
		chan.broadcast(String.format("<%s> has unmuted <%s>",
				client.getAccount().getName(), username));
	}
}
