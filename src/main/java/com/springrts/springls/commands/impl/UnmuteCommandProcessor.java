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
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("UNMUTE")
public class UnmuteCommandProcessor extends AbstractCommandProcessor {

	public UnmuteCommandProcessor() {
		super(2, 2, Account.Access.PRIVILEGED);
	}

	@Override
	public boolean process(final Client client, final List<String> args)
			throws CommandProcessingException
	{
		final boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		final String chanelName = args.get(0);
		final String username = args.get(1);

		final Channel chan = getContext().getChannels().getChannel(chanelName);
		if (chan == null) {
			client.sendLine(String.format(
					"SERVERMSG %s failed: Channel #%s does not exist!",
					getCommandName(), chanelName));
			return false;
		}

		if (!chan.getMuteList().isMuted(username)) {
			client.sendLine(String.format(
					"SERVERMSG %s failed: User <%s> is not on the mute list!",
					getCommandName(), username));
			return false;
		}

		chan.getMuteList().unmute(username);
		client.sendLine(String.format(
				"SERVERMSG You have unmuted <%s> on channel #%s.",
				username, chan.getName()));
		chan.broadcast(String.format("<%s> has unmuted <%s>",
				client.getAccount().getName(), username));

		return true;
	}
}
