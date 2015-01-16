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
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Lets an administrator convert a long representing a date into a human
 * readable form.
 * NOTE This command is SpringLS specific, not part of the official lobby protocol!
 * @author hoijui
 */
@SupportedCommand("LONGTIMETODATE")
public class LongTimeToDateCommandProcessor extends AbstractCommandProcessor {

	public LongTimeToDateCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("time", Long.class, Argument.PARSER_TO_LONG)),
				Account.Access.ADMIN);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final long time = (Long) args.getWords().get(0);

		// As DateFormats are generally not-thread save,
		// we always create a new one.
		final DateFormat dateTimeFormat
				= new SimpleDateFormat("d MMM yyyy HH:mm:ss z");

		client.sendLine(String.format(
				"SERVERMSG %s result: %s",
				getCommandName(),
				dateTimeFormat.format(new Date(time))));
	}
}
