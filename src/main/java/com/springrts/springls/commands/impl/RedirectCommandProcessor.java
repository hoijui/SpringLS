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
import com.springrts.springls.util.Misc;
import com.springrts.springls.ServerNotification;
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
@SupportedCommand("REDIRECT")
public class RedirectCommandProcessor extends AbstractCommandProcessor {

	public RedirectCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("redirectIp")), // TODO use converter here!
				Account.Access.ADMIN);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final String redirectIpStr = (String)args.getWords().get(0);
		final InetAddress redirectIp = Misc.parseIp(redirectIpStr);
		if (redirectIp == null) {
			processingError();
		}
		getContext().getServer().setRedirectAddress(redirectIp);
		getContext().getClients().sendToAllRegisteredUsers(
				"BROADCAST Server has entered redirection mode");

		// add server notification:
		final ServerNotification srvNotif = new ServerNotification(
				"Entered redirection mode");
		srvNotif.addLine(String.format(
				"Admin <%s> has enabled redirection mode. New address: %s",
				client.getAccount().getName(), redirectIpStr));
		getContext().getServerNotifications().addNotification(srvNotif);
	}
}
