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
import com.springrts.springls.commands.IndexedArgument;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.util.Arrays;

/**
 * Sent by client in response to a JOINBATTLEREQUEST command in order to prevent
 * the user from joining the battle.
 * @author hoijui
 */
@SupportedCommand("JOINBATTLEDENY")
public class JoinBattleDenyCommandProcessor extends AbstractCommandProcessor {

	public JoinBattleDenyCommandProcessor() {
		super(
				new CommandArguments(Arrays.asList(new IndexedArgument[] {
						new Argument("username")
						}),
						new Argument("reason", true)
						),
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
		final String username = (String)args.getWords().get(0);
		final Client joiningClient = getContext().getClients().getClient(username);
		if (joiningClient == null) {
			processingError();
			return;
		}
		if (joiningClient.getRequestedBattleID() != client.getBattleID()) {
			processingError();
			return;
		}
		joiningClient.setRequestedBattleID(Battle.NO_BATTLE_ID);
		if (!args.getSentences().isEmpty()) {
			final String reason = (String)args.getSentences().get(0);
			joiningClient.sendLine("JOINBATTLEFAILED Denied by battle founder"
					+ " - " + reason);
		} else {
			joiningClient.sendLine("JOINBATTLEFAILED Denied by battle founder");
		}
	}
}
