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

/**
 * @author hoijui
 */
@SupportedCommand("FORCECLOSEBATTLE")
public class ForceCloseBattleCommandProcessor extends AbstractCommandProcessor {

	public ForceCloseBattleCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("battleId", Integer.class, Argument.PARSER_TO_INTEGER)),
				Account.Access.ADMIN);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final int battleId = (Integer)args.getWords().get(0);
//		try {
//			battleId = Integer.parseInt(args.get(0));
//		} catch (final NumberFormatException ex) {
//			client.sendLine("SERVERMSG Invalid BattleID!");
//			return false;
//		}

		final Battle battle = getContext().getBattles().getBattleByID(battleId);
		if (battle == null) {
			processingError(client, "Error: unknown BATTLE_ID!");
			return;
		}

		getContext().getBattles().closeBattleAndNotifyAll(battle);
	}
}
