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
import com.springrts.tasserver.Client;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Sent by client in response to a JOINBATTLEREQUEST command in order to allow
 * the user to join the battle.
 * @author hoijui
 */
@SupportedCommand("JOINBATTLEACCEPT")
public class JoinBattleAcceptCommandProcessor extends AbstractCommandProcessor {

	public JoinBattleAcceptCommandProcessor() {
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

		// check battle
		Battle bat = getContext().getBattles().getBattleByID(client.getBattleID());
		if (bat == null) {
			return false;
		} else if (bat.getFounder() != client) {
			// only founder can accept battle join
			return false;
		}

		String username = args.get(0);

		// check client
		Client joiningClient = getContext().getClients().getClient(username);
		if (joiningClient == null) {
			return false;
		} else if (joiningClient.getRequestedBattleID() !=  client.getBattleID()) {
			return false;
		}

		bat.notifyClientJoined(joiningClient);

		return true;
	}
}