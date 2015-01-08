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
import java.util.List;

/**
 * Sent by client (battle host), to remove script tags in script.txt.
 * @author hoijui
 */
@SupportedCommand("REMOVESCRIPTTAGS")
public class RemoveScriptTagsCommandProcessor extends AbstractCommandProcessor {

	public RemoveScriptTagsCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("scriptTag")),
				Account.Access.NORMAL,
				true,
				true);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final Battle battle = getBattle(client);

		final StringBuilder lowerKeyCommand = new StringBuilder(getCommandName());
		final List<String> scriptTags = (List<String>)args.getWords();
		for (final String scriptTag : scriptTags) {
			final String lowerKeyScriptTag = scriptTag.toLowerCase();
			lowerKeyCommand.append(' ').append(lowerKeyScriptTag);
			battle.getScriptTags().remove(lowerKeyScriptTag);
		}

		// relay the command
		battle.sendToAllClients(lowerKeyCommand.toString());
	}
}
