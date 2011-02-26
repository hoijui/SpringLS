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
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("FLOODLEVEL")
public class FloodLevelCommandProcessor extends AbstractCommandProcessor {

	public FloodLevelCommandProcessor() {
		super(2, 2, Account.Access.ADMIN);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		String type = args.get(0).toUpperCase();

		if (type.equals("PERIOD")) {
			int seconds = Integer.parseInt(args.get(1));
			getContext().getFloodProtection().setReceivedRecordPeriod(seconds);
			client.sendLine(String.format(
					"SERVERMSG The antiflood period is now %d seconds.",
					seconds));
		} else if (type.equals("USER")) {
			int bytes = Integer.parseInt(args.get(1));
			getContext().getFloodProtection().setMaxBytesAlert(bytes);
			client.sendLine(String.format(
					"SERVERMSG The antiflood amount for a normal user is now %d"
					+ " bytes.", bytes));
		} else if (type.equals("BOT")) {
			int bytes = Integer.parseInt(args.get(1));
			getContext().getFloodProtection().setMaxBytesAlertForBot(bytes);
			client.sendLine(String.format(
					"SERVERMSG The antiflood amount for a bot is now %d bytes.",
					bytes));
		}

		return true;
	}
}