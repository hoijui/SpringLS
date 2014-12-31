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
 * Sent by founder of the battle changing username's handicap value
 * (of his battle status).
 * Only founder can change other users handicap values (even they themselves
 * can not change it).
 * @author hoijui
 */
@SupportedCommand("HANDICAP")
public class HandicapCommandProcessor extends AbstractCommandProcessor {

	public HandicapCommandProcessor() {
		// only the founder can change handicap value of another user
		super(2, 2, Account.Access.NORMAL, true, true);
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
		final String handicapStr = args.get(1);

		final int handicap;
		try {
			handicap = Integer.parseInt(handicapStr);
		} catch (final NumberFormatException ex) {
			return false;
		}
		// FIXME: handicap can be [-100, float_max], common: [-100, 100]
		if ((handicap < 0) || (handicap > 100)) {
			return false;
		}

		final Client target = getContext().getClients().getClient(username);
		if (target == null) {
			return false;
		}
		if (!battle.isClientInBattle(target)) {
			return false;
		}

		target.setHandicap(handicap);
		battle.notifyClientsOfBattleStatus(target);

		return true;
	}
}
