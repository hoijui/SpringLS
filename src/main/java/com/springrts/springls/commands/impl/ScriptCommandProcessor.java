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
import com.springrts.springls.Client;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.util.Collections;

/**
 * Sent by client who previously sent SCRIPTSTART command.
 * Multiple commands of this type are expected after SCRIPTSTART command,
 * ending with SCRIPTEND command as the last command in the sequence
 * (also see SCRIPTSTART comments when sent by the client).
 * @author hoijui
 */
@SupportedCommand("SCRIPT")
@Deprecated
public class ScriptCommandProcessor extends AbstractCommandProcessor {

	public ScriptCommandProcessor() {
		super(
				new CommandArguments(
						Collections.EMPTY_LIST,
						new Argument("scriptLine")),
				Account.Access.NORMAL,
				true);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final Battle battle = getBattle(client);

		final String scriptLine = (String)args.getSentences().get(0);
		battle.getTempReplayScript().add(scriptLine);
	}
}
