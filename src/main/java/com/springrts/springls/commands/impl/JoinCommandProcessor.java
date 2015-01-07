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
 * Allows a user to join a channel.
 * @author hoijui
 */
@SupportedCommand("JOIN")
public class JoinCommandProcessor extends AbstractCommandProcessor {

	public JoinCommandProcessor() {
		super(
				new CommandArguments(Arrays.asList(new IndexedArgument[] {
						new Argument("channelName")
						}),
						new Argument("channelKey")
						),
				Account.Access.NORMAL);
		setToClientErrorCommandName("JOINFAILED");
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final String channelName = (String)args.getWords().get(0);
		final String channelKey = (String)args.getSentences().get(0);

		// check if channel name is OK:
		final String valid = getContext().getChannels().isChanNameValid(channelName);
		if (valid != null) {
			processingError(client, String.format(
					"Bad channel name (\"#%s\"). Reason: %s",
					channelName, valid));
		}

		// check if key is correct (if channel is locked):
		Channel chan = getContext().getChannels().getChannel(channelName);
		if ((chan != null) && (chan.isLocked())
				// we allow admins to join locked channels
				&& client.getAccount().getAccess().isLessThen(Account.Access.ADMIN)
				&& !channelKey.equals(chan.getKey()))
		{
			processingError(client, String.format(
					"%s Wrong key (this channel is locked)!",
					channelName));
		}

		chan = client.joinChannel(channelName);
		if (chan == null) {
			processingError(client, String.format(
					"%s Already in the channel!", channelName));
		}
		client.sendLine(String.format("JOIN %s", channelName));
		getContext().getChannels().sendChannelInfoToClient(chan, client);
		getContext().getChannels().notifyClientsOfNewClientInChannel(chan, client);
	}
}
