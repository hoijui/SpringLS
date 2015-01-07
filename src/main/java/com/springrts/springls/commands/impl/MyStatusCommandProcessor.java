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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sent by client to server telling him his status changed.
 * To figure out if battle is "in-game", client must check in-game status of the
 * host.
 * @author hoijui
 */
@SupportedCommand("MYSTATUS")
public class MyStatusCommandProcessor extends AbstractCommandProcessor {

	private static final Logger LOG
			= LoggerFactory.getLogger(MyStatusCommandProcessor.class);

	public MyStatusCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("newStatus", Integer.class, Argument.PARSER_TO_INTEGER)),
				Account.Access.NORMAL);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final int newStatus = (Integer)args.getWords().get(0);

		final boolean oldInGame = client.isInGame();

		client.setStatus(newStatus, false);

		if (client.isInGame() != oldInGame) {
			// user changed his in-game status.
			if (!oldInGame) { // client just entered game
				final Battle battle = getBattle(client);
				if ((battle != null) && (battle.getClientsSize() > 0)) {
					client.setInGameTime(System.currentTimeMillis()); // FIXME ... that looks wrong! should be now - joinTime. also: change name of the inGameTime property in Client, as it is misnamed (shoudl rather be split in two: joinTime & inGameTime)
				} else {
					// we will not update clients that play by themselves
					// (or with bots), since some try to exploit the system by
					// leaving thier computer alone in-battle for hours, to
					// increase their ranks
					client.setInGameTime(0);
				}
				if ((battle != null) && (battle.getFounder() == client)
						&& (battle.getNatType() == 1))
				{
					// the client is a battle host using the "hole punching"
					// technique

					// tell clients to replace the battle port with the
					// founder's public UDP source port
					battle.sendToAllExceptFounder(String.format("HOSTPORT %d",
							client.getUdpSourcePort()));
				}
			} else { // back from game
				if (client.getInGameTime() != 0) {
					// We will not update clients that play
					// by themselves (or with bots only),
					// since some try to exploit the system
					// by leaving their computer alone in-battle
					// for hours, to increase their ranks.
					final long diffMins = (System.currentTimeMillis()
							- client.getInGameTime()) / 60000;
					client.getAccount().addMinsToInGameTime(diffMins);
					final boolean mergeOk = getContext().getAccountsService()
							.mergeAccountChanges(client.getAccount(),
							client.getAccount().getName());
					if (!mergeOk) {
						// as this is no serious problem, only log a message
						LOG.warn("Failed updating users in-game-time in"
								+ " persistent storage: {}",
								client.getAccount().getName());
						return false;
					}
				}
			}
		}
		getContext().getClients().notifyClientsOfNewClientStatus(client);
	}
}
