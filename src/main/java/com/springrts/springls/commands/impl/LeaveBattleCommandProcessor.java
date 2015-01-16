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
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;

/**
 * Sent by the client when he leaves a battle.
 * Also sent by a founder of the battle when he closes the battle.
 * @author hoijui
 */
@SupportedCommand("LEAVEBATTLE")
public class LeaveBattleCommandProcessor extends AbstractCommandProcessor {

	public LeaveBattleCommandProcessor() {
		super(Account.Access.NORMAL, true);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		// If the client sent the LEAVEBATTLE command right after he was kicked
		// from the battle, this might be <code>null</code>, but we already
		// check for that earlier, in AbstractCommandProcessor
		// -> we always get a valid value here
		final Battle battle = getBattle(client);

		// automatically checks if the client is the founder and in that case
		// closes the battle
		getContext().getBattles().leaveBattle(client, battle);
	}
}
