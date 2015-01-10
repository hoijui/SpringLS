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
import com.springrts.springls.commands.IndexedArgument;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.util.Arrays;

/**
 * @author hoijui
 */
@SupportedCommand("KICKUSER")
public class KickUserCommandProcessor extends AbstractCommandProcessor {

	public KickUserCommandProcessor() {
		super(
				new CommandArguments(Arrays.asList(new IndexedArgument[] {
						new Argument("username")
						}),
						new Argument("reason", true)
						),
				Account.Access.PRIVILEGED);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final String username = (String)args.getWords().get(0);

		final Client target = getContext().getClients().getClient(username);
		final String reason;
		if (!args.getSentences().isEmpty()) {
			reason = String.format(
					" (reason: %s)",
					(String)args.getSentences().get(0));
		} else {
			reason = "";
		}
		if (target == null) {
			processingError();
			return;
		}
		final String broadcastMsg = String.format(
				"<%s> has kicked <%s> from server%s",
				client.getAccount().getName(),
				target.getAccount().getName(),
				reason);
		for (int i = 0; i < getContext().getChannels().getChannelsSize(); i++) {
			if (getContext().getChannels().getChannel(i).isClientInThisChannel(target)) {
				getContext().getChannels().getChannel(i).broadcast(broadcastMsg);
			}
		}
		target.sendLine(String.format(
				"SERVERMSG You have been kicked from the server by <%s>%s",
				client.getAccount().getName(), reason));
		getContext().getClients().killClient(target, "Quit: kicked from server");
	}
}
