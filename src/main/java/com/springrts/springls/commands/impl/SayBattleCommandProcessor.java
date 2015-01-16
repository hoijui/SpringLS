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
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.util.Misc;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.util.Collections;
import java.util.List;


/**
 * Sent by client who is participating in a battle to server, who forwards this
 * message to all other clients in the battle. BATTLE_ID is not required since
 * every user can participate in only one battle at the time. If user is not
 * participating in the battle, this command is ignored and is considered
 * invalid.
 * @author hoijui
 */
@SupportedCommand("SAYBATTLE")
public class SayBattleCommandProcessor extends AbstractSayCommandProcessor {

	private final String returnCommandName;

	protected SayBattleCommandProcessor(final String returnCommandName) {
		super(
				new CommandArguments(
						Collections.EMPTY_LIST,
						new Argument("message")),
				Account.Access.NORMAL,
				true);

		this.returnCommandName = returnCommandName;
	}

	public SayBattleCommandProcessor() {
		this("SAIDBATTLE");
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

		final String message = (String)args.getSentences().get(0);

		checkFlooding(client, message);

		battle.sendToAllClients(String.format("%s %s %s",
				returnCommandName,
				client.getAccount().getName(),
				message));

		return true;
	}
}
