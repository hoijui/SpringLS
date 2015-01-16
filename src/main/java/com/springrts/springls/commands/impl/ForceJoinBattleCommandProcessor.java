/*
	Copyright (c) 2012 Robin Vobruba <hoijui.quaero@gmail.com>

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
 * Sent by a client that is battle host or lobby moderator,
 * to request a user being moved to an other host.
 * @author cheesecan
 */
@SupportedCommand("FORCEJOINBATTLE")
public class ForceJoinBattleCommandProcessor extends AbstractCommandProcessor {

	public ForceJoinBattleCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("userName"),
						new Argument("destinationBattleId", Integer.class, Argument.PARSER_TO_INTEGER),
						new Argument("battlePassword", true)),
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

		final String userName = (String)args.getWords().get(0);
		final Client affectedClient = getContext().getClients().getClient(userName);
		if (affectedClient == null) {
			client.sendLine(String.format(
					"FORCEJOINBATTLEFAILED %s %s", userName,
					"Invalid user name was specified"));
			return false;
		}

		final int battleId = affectedClient.getBattleID();
		final Battle battle = getContext().getBattles().getBattleByID(battleId);
		if (battle == null) {
			client.sendLine(String.format(
					"FORCEJOINBATTLEFAILED %s %s", userName,
					"The user to be moved is not currently in any battle"));
			return false;
		}

		if (!battle.getFounder().equals(client)
				&& !client.getAccount().getAccess().isAtLeast(Account.Access.PRIVILEGED))
		{
			client.sendLine(String.format(
					"FORCEJOINBATTLEFAILED %s %s", userName,
					"The source client must be a lobby moderator or the host of the affected client's current battle"));
			return false;
		}

		final int destinationBattleId = (Integer)args.getWords().get(1);
		TODO we need to send the below FORCEJOINBATTLEFAILED msg when parsing failes!;
		final String destinationBattleIdStr = args.get(1);
		try {
			destinationBattleId = Integer.parseInt(destinationBattleIdStr);
		} catch (NumberFormatException ex) {
			client.sendLine(String.format(
					"FORCEJOINBATTLEFAILED %s %s", userName,
					"Invalid destination battle ID (needs to be an integer): " + destinationBattleIdStr));
			return false;
		}

		final Battle destinationBattle = getContext().getBattles().getBattleByID(destinationBattleId);
		if (destinationBattle == null) {
			client.sendLine(String.format(
					"FORCEJOINBATTLEFAILED %s %s", userName,
					"Invalid destination battle ID (battle does not exist): " + destinationBattleIdStr));
			return false;
		}
		if (destinationBattle.restricted()) {
			client.sendLine(String.format(
					"FORCEJOINBATTLEFAILED %s %s", userName,
					"The destination battle is password-protected, so we can not move to it"));
			return false;
		}
		if (destinationBattle.isLocked()) {
			client.sendLine(String.format(
					"FORCEJOINBATTLEFAILED %s %s", userName,
					"The destination battle is locked, so we can not move to it"));
			return false;
		}

		String battlePassword = null;
		if (args.getWords().size() > 2) { // if optional battlePassword was set
			battlePassword = (String)args.getWords().get(2);
		}

		final boolean clientSupportsCmd = affectedClient.getCompatFlags().contains("m"); // NOTE lobby protocol "0.35+ m"
		if (clientSupportsCmd) {
			final String successResponseMessage = (battlePassword == null)
					? String.format("FORCEJOINBATTLE %d", destinationBattleId)
					: String.format("FORCEJOINBATTLE %d %s", destinationBattleId, battlePassword);

			// Issue response command to notify affected client
			affectedClient.sendLine(successResponseMessage);
		} else {
			// Leave the current battle.
			getContext().getBattles().leaveBattle(affectedClient, battle);

			// Join the destination battle.
			// We fake a JOINBATTLE command, as if it was sent
			// by the affected client
//			final List<String> joinBattleArgs = new ArrayList<String>(1);
//			joinBattleArgs.add(destinationBattleIdStr);
//			getContext().getServerThread().executeCommand(affectedClient, "JOINBATTLE", joinBattleArgs);
			getContext().getServerThread().executeCommand(affectedClient, "JOINBATTLE", destinationBattleIdStr);
		}

		return true;
	}
}
