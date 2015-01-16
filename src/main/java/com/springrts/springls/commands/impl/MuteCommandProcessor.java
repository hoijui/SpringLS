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
import java.net.InetAddress;

/**
 * @author hoijui
 */
@SupportedCommand("MUTE")
public class MuteCommandProcessor extends AbstractCommandProcessor {

	public MuteCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("chanelName"),
						new Argument("username"),
						new Argument("minutes", Long.class, Argument.PARSER_TO_LONG),
						new Argument("option", true)),
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

		final String chanelName = (String)args.getWords().get(0);
		final String username = (String)args.getWords().get(1);

		final Channel chan = getContext().getChannels().getChannel(chanelName);
		if (chan == null) {
			client.sendLine(String.format(
					"SERVERMSG %s failed: Channel #%s does not exist!",
					getCommandName(), chanelName));
			return false;
		}

		if (chan.getMuteList().isMuted(username)) {
			client.sendLine(String.format(
					"SERVERMSG %s failed: User <%s> is already muted. Unmute first!",
					getCommandName(), username));
			return false;
		}

		final Account targetAccount = getContext().getAccountsService().getAccount(username);
		if (targetAccount == null) {
			client.sendLine(String.format(
					"SERVERMSG %s failed: User <%s> does not exist",
					getCommandName(), username));
			return false;
		}

		boolean muteByIP = false;
		if (args.getWords().size() > 3) {
			final String option = (String)args.getWords().get(3);
			if (option.toUpperCase().equals("IP")) {
				muteByIP = true;
			} else {
				client.sendLine(String.format(
						"SERVERMSG %s failed: Invalid argument: \"%s\"",
						getCommandName(), option));
				return false;
			}
		}

		final long minutes = (Long)args.getWords().get(2);
		try {
			minutes = Long.parseLong(args.get(2));
		} catch (final NumberFormatException ex) {
			client.sendLine(String.format(
					"SERVERMSG %s failed: Invalid argument - should be an integer",
					getCommandName()));
			return false;
		}

		final InetAddress muteIp = muteByIP ? targetAccount.getLastIp() : null;
		chan.getMuteList().mute(username, minutes * 60, muteIp);

		client.sendLine(String.format(
				"SERVERMSG You have muted <%s> on channel #%s.",
				username, chan.getName()));
		chan.broadcast(String.format("<%s> has muted <%s>",
				client.getAccount().getName(), username));

		return true;
	}
}
