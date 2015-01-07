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
import com.springrts.springls.util.ProtocolUtil;
import java.util.Arrays;

/**
 * Sent by server to all registered clients telling them some of the parameters
 * of the battle changed. Battle internal changes, like starting metal, energy,
 * starting position etc., are sent only to clients.
 * @author hoijui
 */
@SupportedCommand("UPDATEBATTLEINFO")
public class UpdateBattleInfoCommandProcessor extends AbstractCommandProcessor {

	public UpdateBattleInfoCommandProcessor() {
		super(
				new CommandArguments(Arrays.asList(new IndexedArgument[] {
						new Argument("spectatorCount", Integer.class, Argument.PARSER_TO_INTEGER),
						new Argument("locked", Boolean.class, Argument.PARSER_TO_BOOLEAN),
						new Argument("mapHash")
						}),
						new Argument("mapName")),
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
		final Battle battle = getBattle(client);

		final int spectatorCount = (Integer)args.getWords().get(0);
		final boolean locked = (Boolean)args.getWords().get(1);
		final String mapHashStr = (String)args.getWords().get(2);
		final String mapName = (String)args.getSentences().get(0);

		int maphash;
		try {
			maphash = Integer.decode(mapHashStr);
		} catch (final NumberFormatException ex) {
			return false;
		}

		battle.setMapName(mapName);
		battle.setLocked(locked);
		battle.setMapHash(maphash);
		getContext().getClients().sendToAllRegisteredUsers(
				String.format("UPDATEBATTLEINFO %d %d %d %s %s",
				battle.getId(),
				spectatorCount,
				ProtocolUtil.boolToNumber(battle.isLocked()),
				maphash,
				battle.getMapName()));
	}
}
