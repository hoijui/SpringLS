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
 * Sent by client in response to a JOINBATTLEREQUEST command in order to allow
 * the user to join the battle.
 * @author hoijui
 */
@SupportedCommand("JOINBATTLEACCEPT")
public class JoinBattleAcceptCommandProcessor extends AbstractCommandProcessor {

	public JoinBattleAcceptCommandProcessor() {
		// only the founder can accept battle join
		super(1, 1, Account.Access.NORMAL, true, true);
	}

	@Override
	public boolean process(final Client client, final List<String> args)
			throws CommandProcessingException
	{
		final boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		if (client.getBattleID() == Battle.NO_BATTLE_ID) {
			return false;
		}

		final Battle battle = getBattle(client);

		final String username = args.get(0);

		// check client
		final Client joiningClient = getContext().getClients().getClient(username);
		if (joiningClient == null) {
			return false;
		} else if (joiningClient.getRequestedBattleID()
				!= client.getBattleID())
		{
			return false;
		}

		battle.notifyClientJoined(joiningClient);

		return true;
	}
}
