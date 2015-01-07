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

package com.springrts.springls.floodprotection;


import com.springrts.springls.Account;
import com.springrts.springls.Client;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;

/**
 * @author hoijui
 * NOTE This command is SpringLS specific, not part of the official lobby protocol!
 */
@SupportedCommand("FLOODLEVEL")
public class FloodLevelCommandProcessor extends AbstractCommandProcessor {

	public FloodLevelCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("objectType", String.class, Argument.PARSER_TO_UPPER_CASE),
						new Argument("objectIndex", Integer.class, Argument.PARSER_TO_INTEGER)),
				Account.Access.ADMIN);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final String type = (String)args.getWords().get(0);

		final FloodProtection floodProtection
				= getContext().getService(FloodProtection.class);
		if (floodProtection == null) {
			client.sendLine("SERVERMSG The anti-flood service is not running.");
		} else {
			if (type.equals("PERIOD")) {
				final int seconds = (Integer) args.getWords().get(1);
				floodProtection.setReceivedRecordPeriod(seconds);
				client.sendLine(String.format(
						"SERVERMSG The anti-flood period is now %d seconds.",
						seconds));
			} else if (type.equals("USER")) {
				final int bytes = (Integer) args.getWords().get(1);
				floodProtection.setMaxBytesAlert(bytes);
				client.sendLine(String.format(
						"SERVERMSG The anti-flood amount for a normal user is"
						+ " now %d bytes.", bytes));
			} else if (type.equals("BOT")) {
				final int bytes = (Integer) args.getWords().get(1);
				floodProtection.setMaxBytesAlertForBot(bytes);
				client.sendLine(String.format(
						"SERVERMSG The anti-flood amount for a bot is now %d"
						+ " bytes.",
						bytes));
			}
		}
	}
}
