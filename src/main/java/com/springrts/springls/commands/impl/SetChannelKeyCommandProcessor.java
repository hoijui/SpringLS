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
 * Lets a moderator set the key of a channel.
 * @see Channel#getKey()
 * @author hoijui
 */
@SupportedCommand("SETCHANNELKEY")
public class SetChannelKeyCommandProcessor extends AbstractCommandProcessor {

	public SetChannelKeyCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("channelName"),
						new Argument("key")),
				Account.Access.PRIVILEGED);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final String channelName = (String)args.getWords().get(0);
		final String key = (String)args.getWords().get(1);

		final Channel chan = getContext().getChannels().getChannel(channelName);
		if (chan == null) {
			processingError(client, String.format(
					"Error: Channel does not exist: %s",
					channelName));
		}

		if (key.equals("*")) {
			if (!chan.isLocked()) {
				processingError(client,
						"Error: Unable to unlock channel - channel is not locked!");
			}
			chan.setKey(Channel.KEY_NONE);
			chan.broadcast(String.format("<%s> has just unlocked #%s",
					client.getAccount().getName(),
					chan.getName()));
		} else {
			if (!key.matches("^[A-Za-z0-9_]+$")) {
				processingError(client, String.format("Error: Invalid key: %s",
						key));
			}
			chan.setKey(key);
			chan.broadcast(String.format(
					"<%s> has just locked #%s with private key",
					client.getAccount().getName(),
					chan.getName()));
		}
	}
}
