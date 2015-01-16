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
 * Sent by the founder of the battle when he is trying to force some other
 * clients mode to spectator.
 * The server will update client's battle status automatically.
 * @author hoijui
 */
@SupportedCommand("FORCESPECTATORMODE")
public class ForceSpectatorModeCommandProcessor
		extends AbstractCommandProcessor
{
	public ForceSpectatorModeCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("username")),
				Account.Access.NORMAL,
				true,
				true); // only the founder can
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

		final String username = (String)args.getWords().get(0);

		final Client target = getContext().getClients().getClient(username);
		if (target == null) {
			return false;
		}
		if (!battle.isClientInBattle(target)) {
			return false;
		}

		if (target.isSpectator()) {
			// no need to change this, it is already set to spectator mode!
			return false;
		}

		target.setSpectator(true);
		battle.notifyClientsOfBattleStatus(target);

		return true;
	}
}
