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
import java.net.InetAddress;

/**
 * Sent by a client trying to join a battle. Password is an optional parameter.
 * @author hoijui
 */
@SupportedCommand("JOINBATTLE")
public class JoinBattleCommandProcessor extends AbstractCommandProcessor {

	public JoinBattleCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("battleId", Integer.class, Argument.PARSER_TO_INTEGER),
						new Argument("password", true),
						new Argument("scriptPassword", true)),
				Account.Access.NORMAL);
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

		final int battleID = (Integer)args.getWords().get(0);
		final int battleID;
		try {
			battleID = Integer.parseInt(battleIdStr);
		} catch (NumberFormatException e) {
			client.sendLine("JOINBATTLEFAILED No battle ID!");
			return false;
		}

		if (client.getBattleID() != Battle.NO_BATTLE_ID) {
			// ca not join a battle if we are already participating in one
			client.sendLine("JOINBATTLEFAILED Cannot participate in multiple"
					+ " battles at the same time!");
			return false;
		}

		final Battle battle = getContext().getBattles().getBattleByID(battleID);

		if (battle == null) {
			client.sendLine("JOINBATTLEFAILED Invalid battle ID!");
			return false;
		}

		if (battle.restricted()) {
			if (args.getWords().size() < 2) {
				client.sendLine("JOINBATTLEFAILED Password required");
				return false;
			}

			final String password = (String)args.getWords().get(1);

			if (!battle.getPassword().equals(password)) {
				client.sendLine("JOINBATTLEFAILED Invalid password");
				return false;
			}
		}

		if (battle.isLocked()) {
			client.sendLine("JOINBATTLEFAILED You cannot join locked battles!");
			return false;
		}

		if (args.getWords().size() > 2) {
			final String scriptPassword = (String)args.getWords().get(2);
			client.setScriptPassword(scriptPassword);
		}

		if (battle.getFounder().isHandleBattleJoinAuthorization()) {
			client.setRequestedBattleID(battleID);
			final InetAddress ipAddress
					= battle.getFounder().getIp().equals(client.getIp())
					? client.getLocalIp()
					: client.getIp();
			battle.getFounder().sendLine(String.format(
					"JOINBATTLEREQUEST %s %s",
					client.getAccount().getName(),
					ipAddress.getHostAddress()));
		} else {
			battle.notifyClientJoined(client);
		}

		return true;
	}
}
