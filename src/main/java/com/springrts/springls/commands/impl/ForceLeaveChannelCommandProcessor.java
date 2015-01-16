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
import com.springrts.springls.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.util.Arrays;

/**
 * Lets a moderator kick a client from a channel.
 * @author hoijui
 */
@SupportedCommand("FORCELEAVECHANNEL")
public class ForceLeaveChannelCommandProcessor
		extends AbstractCommandProcessor
{
	public ForceLeaveChannelCommandProcessor() {
		super(
				new CommandArguments(Arrays.asList(new IndexedArgument[] {
						new Argument("channelName"),
						new Argument("username")
						}),
						new Argument("reason", true)
						),
				Account.Access.PRIVILEGED);
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
			client.sendLine(String.format(
					"SERVERMSG Bad arguments (command %s)", getCommandName()));
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		final String channelName = (String)args.getWords().get(0);
		final String username = (String)args.getWords().get(1);

		final Channel chan = getContext().getChannels().getChannel(channelName);
		if (chan == null) {
			client.sendLine(String.format(
					"SERVERMSG Error: Channel does not exist: %s",
					channelName));
			return false;
		}

		final Client target = getContext().getClients().getClient(username);
		if (target == null) {
			client.sendLine(String.format(
					"SERVERMSG Error: <%s> not found!", username));
			return false;
		}

		if (!chan.isClientInThisChannel(target)) {
			client.sendLine(String.format(
					"SERVERMSG Error: <%s> is not in the channel #%s!",
					username, chan.getName()));
			return false;
		}

		String reason = null;
		if (!args.getSentences().isEmpty()) {
			reason = (String)args.getSentences().get(0);
		}

		chan.broadcast(String.format("<%s> has kicked <%s> from the channel%s",
				client.getAccount().getName(),
				target.getAccount().getName(),
				(reason == null) ? "" : " (reason: " + reason + ")"));
		target.sendLine(String.format("FORCELEAVECHANNEL %s %s%s",
				chan.getName(),
				client.getAccount().getName(),
				(reason == null) ? "" : " " + reason));
		target.leaveChannel(chan, "kicked from channel");

		return true;
	}
}
