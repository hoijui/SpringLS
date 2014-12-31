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
 * Sent to client for whom founder requested kick with KICKFROMBATTLE command.
 * Client doesn't need to send LEAVEBATTLE command, that is already done by the
 * server.
 * The only purpose this commands serves to is to notify client that he was
 * kicked from the battle. Note that client should close the battle internally,
 * since he is no longer a part of it (or he can do that once he receives
 * LEFTBATTLE command containing his username).
 * @author hoijui
 */
@SupportedCommand("KICKFROMBATTLE")
public class KickFromBattleCommandProcessor extends AbstractCommandProcessor {

	public KickFromBattleCommandProcessor() {
		// only the founder can kick other clients
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

		final Battle battle = getBattle(client);

		final String username = args.get(0);

		final Client target = getContext().getClients().getClient(username);
		if (target == null) {
			return false;
		}
		if (!battle.isClientInBattle(target)) {
			return false;
		}

		battle.sendToAllClients(String.format(
				"SAIDBATTLEEX %s kicked %s from battle",
				client.getAccount().getName(),
				target.getAccount().getName()));
		// notify client that he was kicked from the battle:
		target.sendLine("FORCEQUITBATTLE");
		// force client to leave battle:
		getContext().getServerThread().executeCommand("LEAVEBATTLE", target);

		return true;
	}
}
