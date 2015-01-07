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
import com.springrts.springls.util.Processor;
import com.springrts.springls.TeamController;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.awt.Color;

/**
 * Sent by a client to the server telling him his status in the battle changed.
 * @author hoijui
 */
@SupportedCommand("MYBATTLESTATUS")
public class MyBattleStatusCommandProcessor extends AbstractCommandProcessor {

	public MyBattleStatusCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("newBattleStatus", Integer.class, Argument.PARSER_TO_INTEGER),
						new Argument("teamColor", Color.class, Argument.PARSER_TO_COLOR)),
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

		final int newBattleStatus = (Integer)args.getWords().get(0);
		final Color newTeamColor = (Color)args.getWords().get(1);

		return setBattleStatus(client, battle, newBattleStatus, newTeamColor);
	}

	private static class AllyTeamAndColorAligner
			implements Processor<TeamController>
	{
		private final TeamController alignTo;

		AllyTeamAndColorAligner(final TeamController alignTo) {
			this.alignTo = alignTo;
		}

		@Override
		public void process(final TeamController curTeamController) {
			if ((curTeamController != alignTo)
					&& (curTeamController.getTeam() == alignTo.getTeam())
					&& !curTeamController.isSpectator())
			{
				alignTo.setAllyTeam(curTeamController.getAllyTeam());
				alignTo.setTeamColor(curTeamController.getTeamColor());
			}
		}
	}

	private boolean setBattleStatus(
			final Client client,
			final Battle battle,
			final int newBattleStatus,
			final Color newTeamColor)
	{
		final int oldHandicap = client.getHandicap();
		client.setBattleStatus(newBattleStatus);
		// Note: We ignore the handicap value, as it can be changed only by the
		// founder with the HANDICAP command!
		client.setHandicap(oldHandicap);
		client.setTeamColor(newTeamColor);

		// if game is full or game type is "battle replay", force player's mode
		// to spectator:
		if (((battle.getClientsSize() + 1 - battle.spectatorCount())
				> battle.getMaxPlayers()) || (battle.getType() == 1))
		{
			client.setSpectator(true);
		}
		// if player has chosen a team number which is already used by some
		// other player/bot, force his ally number and team color to be the same
		// as the one of that player/bot:
		battle.applyToTeamControllers(new AllyTeamAndColorAligner(client));

		battle.notifyClientsOfBattleStatus(client);

		return true;
	}
}
