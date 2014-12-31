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
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * Sent by client to server when trying to play a "ring" sound to user
 * 'username'.
 * Only privileged users can ring anyone, although "normal" clients can ring
 * only when they are hosting and only players participating in their battle.
 * @author hoijui
 */
@SupportedCommand("RING")
public class RingCommandProcessor extends AbstractCommandProcessor {

	public RingCommandProcessor() {
		super(1, 1, Account.Access.NORMAL);
	}

	@Override
	public boolean process(final Client client, final List<String> args)
			throws CommandProcessingException
	{
		final boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		final String username = args.get(0);

		if (client.getAccount().getAccess().isLessThen(Account.Access.PRIVILEGED)) {
			// normal users can ring only when they are hosting
			// and only clients who are participating in their battle
			final Client target = getContext().getClients().getClient(username);
			if (target == null) {
				return false;
			}

			if (client.getBattleID() == Battle.NO_BATTLE_ID) {
				client.sendLine(String.format(
						"SERVERMSG %s command failed: You can only ring players"
						+ " participating in your own battle!",
						getCommandName()));
				return false;
			}

			final Battle battle = getBattle(client);
			getContext().getBattles().verify(battle);

			if (!battle.isClientInBattle(target)) {
				client.sendLine(String.format(
						"SERVERMSG %s command failed: You do not have"
						+ " permission to ring players other than those"
						+ " participating in your battle!", getCommandName()));
				return false;
			}

			// only host can ring players participating in his own battle,
			// unless target is host himself:
			if ((client != battle.getFounder())
					&& (target != battle.getFounder()))
			{
				client.sendLine(String.format(
						"SERVERMSG %s command failed: You can ring only battle"
						+ " host, or if you are the battle host, only players"
						+ " participating in your own battle!",
						getCommandName()));
				return false;
			}

			target.sendLine(String.format("RING %s",
					client.getAccount().getName()));
		} else {
			// privileged users can ring anyone
			final Client target = getContext().getClients().getClient(username);
			if (target == null) {
				return false;
			}

			target.sendLine(String.format("RING %s",
					client.getAccount().getName()));
		}

		return true;
	}
}
