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
 * Sent by the founder the of the battle when he is trying to force some other
 * clients team number to 'teamno'.
 * The server will update the clients battle status automatically.
 * @author hoijui
 */
@SupportedCommand("FORCETEAMNO")
public class ForceTeamNumberCommandProcessor extends AbstractCommandProcessor {

	public ForceTeamNumberCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("username"),
						new Argument("teamNumber", Integer.class, Argument.PARSER_TO_INTEGER)),
				Account.Access.NORMAL,
				true,
				true); // only the founder can
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final Battle battle = getBattle(client);

		final String username = (String)args.getWords().get(0);
		final int teamNumber = (Integer)args.getWords().get(1);

		if ((teamNumber < 0)
				|| (teamNumber > getContext().getEngine().getMaxTeams() - 1))
		{
			processingError();
			return;
		}

		final Client target = getContext().getClients().getClient(username);
		if (target == null) {
			processingError();
			return;
		}
		if (!battle.isClientInBattle(target)) {
			processingError();
			return;
		}

		target.setTeam(teamNumber);
		battle.notifyClientsOfBattleStatus(target);
	}
}
