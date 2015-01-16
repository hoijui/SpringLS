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
import com.springrts.springls.Battle;
import com.springrts.springls.Bot;
import com.springrts.springls.Client;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.IndexedArgument;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.awt.Color;
import java.util.Arrays;

/**
 * @author hoijui
 */
@SupportedCommand("ADDBOT")
public class AddBotCommandProcessor extends AbstractCommandProcessor {

	public AddBotCommandProcessor() {
		super(
				new CommandArguments(Arrays.asList(new IndexedArgument[] {
						new Argument("botName"),
						new Argument("battleStatus", Integer.class, Argument.PARSER_TO_INTEGER),
						new Argument("teamColor", Color.class, Argument.PARSER_TO_COLOR),
						}),
						new Argument("specifier")
						),
				Account.Access.NORMAL,
				true);
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

		final Battle battle = getBattle(client);

		final String botName = (String)args.getWords().get(0);
		final int battleStatus = (Integer) args.getWords().get(1);
		final Color teamColor = (Color) args.getWords().get(2);
		final String specifier = (String)args.getSentences().get(0);

		if (!Bot.isValidName(botName)) {
			client.sendLine("SERVERMSG Bad bot name. Try another!");
			return false;
		}

		if (battle.getBot(botName) != null) {
			client.sendLine("SERVERMSG Bot name already assigned."
					+ " Choose another!");
			return false;
		}

		final Bot bot = new Bot(botName, client.getAccount().getName(),
				specifier, battleStatus, teamColor);
		battle.addBot(bot);

		return true;
	}
}
