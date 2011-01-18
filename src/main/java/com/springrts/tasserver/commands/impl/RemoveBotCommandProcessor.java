/*
	Copyright (c) 2010 Robin Vobruba <robin.vobruba@derisk.ch>

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

package com.springrts.tasserver.commands.impl;


import com.springrts.tasserver.Account;
import com.springrts.tasserver.Battle;
import com.springrts.tasserver.Bot;
import com.springrts.tasserver.Client;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Removes a bot from the battle.
 * @author hoijui
 */
@SupportedCommand("REMOVEBOT")
public class RemoveBotCommandProcessor extends AbstractCommandProcessor {

	public RemoveBotCommandProcessor() {
		super(1, 1, Account.Access.NORMAL);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		if (client.getBattleID() == Battle.NO_BATTLE_ID) {
			return false;
		}

		Battle bat = getContext().getBattles().getBattleByID(client.getBattleID());
		getContext().getBattles().verify(bat);

		String botName = args.get(0);

		Bot bot = bat.getBot(botName);
		if (bot == null) {
			return false;
		}

		bat.removeBot(bot);

		bat.sendToAllClients(new StringBuilder("REMOVEBOT ")
				.append(bat.getId()).append(" ")
				.append(bot.getName()).toString());

		return true;
	}
}