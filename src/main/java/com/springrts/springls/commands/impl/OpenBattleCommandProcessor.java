/*
	Copyright (c) 2010 Robin Vobruba <hoijui.quaero@gmail.com>

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
import com.springrts.springls.Clients;
import com.springrts.springls.ServerConfiguration;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.IndexedArgument;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.util.Arrays;
import org.apache.commons.configuration.Configuration;

/**
 * @author hoijui
 */
@SupportedCommand("OPENBATTLE")
public class OpenBattleCommandProcessor extends AbstractCommandProcessor {

	public OpenBattleCommandProcessor() {
		super(
				new CommandArguments(Arrays.asList(new IndexedArgument[] {
						new Argument("type", Integer.class, Argument.PARSER_TO_INTEGER),
						new Argument("natType", Integer.class, Argument.PARSER_TO_INTEGER),
						new Argument("password"),
						new Argument("port", Integer.class, Argument.PARSER_TO_INTEGER),
						new Argument("maxPlayers", Integer.class, Argument.PARSER_TO_INTEGER),
						new Argument("hash", Integer.class, Argument.PARSER_TO_INTEGER),
						new Argument("rank", Integer.class, Argument.PARSER_TO_INTEGER),
						new Argument("maphash", Integer.class, Argument.PARSER_TO_INTEGER)
						}),
						new Argument("engineName", true),
						new Argument("engineVersion", true),
						new Argument("mapName"),
						new Argument("title"),
						new Argument("gameName")
						),
				Account.Access.NORMAL);
		setToClientErrorCommandName("OPENBATTLEFAILED");
	}

	/**
	 * Creates new Battle object from a command that client sent to server.
	 * This method parses the command 's', and tries to read
	 * battle attributes from it.
	 * @return the created battle or 'null' on failure
	 */
	private Battle createBattleFromString(final ParsedCommandArguments args, final Client founder) {

		final String pass = (String)args.getWords().get(2);
		if (!pass.equals("*") && !pass.matches("^[A-Za-z0-9_]+$")) {
			// invalid characters in the password
			return null;
		}

		final int type = (Integer)args.getWords().get(0);
		final int natType = (Integer)args.getWords().get(1);
		// args.getWords().get(2) is password
		final int port = (Integer)args.getWords().get(3);
		final int maxPlayers = (Integer)args.getWords().get(4);
		final int hash = (Integer)args.getWords().get(5);
		final int rank = (Integer)args.getWords().get(6);
		final int maphash = (Integer)args.getWords().get(7);

		final boolean engineArgs = founder.getCompatFlags().contains("cl"); // NOTE lobby protocol "0.36+ cl"

		final int requiredSentences = engineArgs ? 5 : 3; // NOTE lobby protocol "0.36+ cl"
		if (args.getSentences().size() != requiredSentences) {
			return null;
		}
		int sentenceIndex = 0;
		final String engineName;
		final String engineVersion;
		if (engineArgs) {
			engineName = (String)args.getSentences().get(sentenceIndex++); // For example: 'my spring'
			engineVersion = (String)args.getSentences().get(sentenceIndex++); // For example: '94.1.1-1062-g9d16c2d develop'
		} else {
			engineName = "spring"; // default; the same value uberserver uses
			final Configuration conf = getContext().getService(Configuration.class);
			engineVersion = conf.getString(ServerConfiguration.ENGINE_VERSION);
		}
		final String mapName = (String)args.getSentences().get(sentenceIndex++);
		final String title = (String)args.getSentences().get(sentenceIndex++);
		final String gameName = (String)args.getSentences().get(sentenceIndex++);

		if ((type < 0) || (type > 1)) {
			return null;
		}
		if ((natType < 0) || (natType > 2)) {
			return null;
		}

		return new Battle(type, natType, founder, pass, port,
				maxPlayers, hash, rank, maphash, mapName, title, gameName,
				engineName, engineVersion);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		if (client.getBattleID() != Battle.NO_BATTLE_ID) {
			processingError(client, "You are already hosting a battle!");
			return;
		}
		final Battle battle = createBattleFromString(args, client);
		if (battle == null) {
			processingError(client, "Invalid command format or bad arguments");
			return;
		}
		getContext().getBattles().addBattle(battle);
		client.setDefaultBattleStatus();
		client.setBattleID(battle.getId());
		client.setRequestedBattleID(Battle.NO_BATTLE_ID);

		boolean local;
		final Clients clients = getContext().getClients();
		for (int i = 0; i < clients.getClientsSize(); i++) {
			final Client curClient = clients.getClient(i);
			if (curClient.getAccount().getAccess().isLessThen(
					Account.Access.NORMAL))
			{
				continue;
			}
			// make sure that the clients behind NAT get local IPs and not
			// external ones:
			local = client.getIp().equals(curClient.getIp());
			curClient.sendLine(battle.createBattleOpenedCommandEx(local));
		}

		// notify client that he successfully opened a new battle
		client.sendLine("OPENBATTLE " + battle.getId());
		client.sendLine("REQUESTBATTLESTATUS");
	}
}
