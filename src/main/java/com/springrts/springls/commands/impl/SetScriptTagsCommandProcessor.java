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
import com.springrts.springls.util.Misc;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Sent by client (battle host), to set script tags in script.txt.
 * The [pair] format is "key=value can have spaces".
 * Keys may not contain spaces, and are expected to use the '/' character to
 * separate tables (see example). In version 0.35 of this software, the command
 * UPDATEBATTLEDETAILS was completely replaced by this command.
 * @author hoijui
 */
@SupportedCommand("SETSCRIPTTAGS")
public class SetScriptTagsCommandProcessor extends AbstractCommandProcessor {

	private static final Pattern INVALID_KEY   = Pattern.compile("[ =;{}\\[\\]\\n\\r]");
	private static final Pattern INVALID_VALUE = Pattern.compile("[;}\\[\\n\\r]");

	public SetScriptTagsCommandProcessor() {
		super(1, ARGS_MAX_NOCHECK, Account.Access.NORMAL, true, true);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = false;
		try {
			checksOk = super.process(client, args);
		} catch (InvalidNumberOfArgumentsCommandProcessingException ex) {
			// kill client since it is not using this command correctly
			client.sendLine(String.format(
					"SERVERMSG Serious error: inconsistent data (%s command)."
					+ " You will now be disconnected ...", getCommandName()));
			getContext().getClients().killClient(client, "Quit: inconsistent data");
			return false;
		}
		if (!checksOk) {
			return false;
		}

		Battle battle = getBattle(client);

		String scriptTagsOrig = Misc.makeSentence(args, 0);

		Map<String, String> scriptTags = parseScriptTags(scriptTagsOrig);

		String scriptTagsClean = createScriptTagsString(scriptTags);

		battle.getScriptTags().putAll(scriptTags);

		// relay the valid pairs
		if (scriptTagsClean.length() > 0) {
			battle.sendToAllClients("SETSCRIPTTAGS " + scriptTagsClean);
		}

		return true;
	}

	private static Map<String, String> parseScriptTags(String keyValuePairs) {

		Map<String, String> scriptTags = new HashMap<String, String>();

		String[] pairs = keyValuePairs.split("\t");
		for (int i = 0; i < pairs.length; i++) {
			parseKeyValuePair(scriptTags, pairs[i]);
		}

		return scriptTags;
	}

	private static boolean isValidKey(String key) {

		if (key.isEmpty()) {
			return false;
		}
		if (INVALID_KEY.matcher(key).matches()) {
			return false;
		}

		return true;
	}

	private static boolean isValidValue(String value) {

		// forbid trailing/leading spaces
		if (!value.equals(value.trim())) {
			return false;
		}
		if (INVALID_VALUE.matcher(value).matches()) {
			return false;
		}

		return true;
	}

	private static boolean parseKeyValuePair(Map<String, String> properties, String keyValuePair) {

		int equalPos = keyValuePair.indexOf('=');
		if (equalPos < 1) {
			return false;
		}

		// parse the key
		String key = keyValuePair.substring(0, equalPos).toLowerCase();
		if (!isValidKey(key)) {
			return false;
		}

		// parse the value
		String value = keyValuePair.substring(equalPos + 1);
		if (!isValidValue(value)) {
			return false;
		}

		properties.put(key, value);

		return true;
	}

	private static String createScriptTagsString(Map<String, String> scriptTags) {

		StringBuilder scriptTagsClean = new StringBuilder();

		for (Entry<String, String> entry : scriptTags.entrySet()) {
			if (scriptTagsClean.length() > 0) {
				scriptTagsClean.append("\t");
			}
			scriptTagsClean.append(entry.getKey()).append("=").append(entry.getValue());
		}

		return scriptTagsClean.toString();
	}
}
