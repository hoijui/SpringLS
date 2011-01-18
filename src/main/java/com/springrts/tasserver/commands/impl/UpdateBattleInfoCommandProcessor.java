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
import com.springrts.tasserver.Misc;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Sent by server to all registered clients telling them some of the parameters
 * of the battle changed. Battle's inside changes, like starting metal, energy,
 * starting position etc., are sent only to clients.
 * @author hoijui
 */
@SupportedCommand("UPDATEBATTLEINFO")
public class UpdateBattleInfoCommandProcessor extends AbstractCommandProcessor {

	public UpdateBattleInfoCommandProcessor() {
		super(4, ARGS_MAX_NOCHECK, Account.Access.NORMAL);
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
		Battle bat = getContext().getBattles().getBattleByID(client.getBattleID());
		if (bat == null) {
			return false;
		}
		if (bat.getFounder() != client) {
			return false; // only founder may change battle parameters!
		}

		String spectatorCountStr = args.get(0);
		String lockedStr = args.get(1);
		String mapHashStr = args.get(2);
		String mapName = Misc.makeSentence(args, 3);

		int spectatorCount = 0;
		boolean locked;
		int maphash;
		try {
			spectatorCount = Integer.parseInt(spectatorCountStr);
			locked = Misc.strToBool(lockedStr);
			maphash = Integer.decode(mapHashStr);
		} catch (NumberFormatException e) {
			return false;
		}

		bat.setMapName(mapName);
		bat.setLocked(locked);
		bat.setMapHash(maphash);
		getContext().getClients().sendToAllRegisteredUsers(
				new StringBuilder("UPDATEBATTLEINFO ")
				.append(bat.getId()).append(" ")
				.append(spectatorCount).append(" ")
				.append(Misc.boolToStr(bat.isLocked())).append(" ")
				.append(maphash).append(" ")
				.append(bat.getMapName()).toString());

		return true;
	}
}