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
import com.springrts.springls.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.util.Collections;
import java.util.HashMap;
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
		super(
				new CommandArguments(
						Collections.EMPTY_LIST,
						new Argument("scriptTags")),
				Account.Access.NORMAL,
				true,
				true);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		boolean checksOk = false;
		try {
			checksOk = super.process(client, args);
		} catch (final InvalidNumberOfArgumentsCommandProcessingException ex) {
			client.sendLine(String.format(
					"SERVERMSG Serious error: inconsistent data (%s command)",
					getCommandName()));
			return false;
		}
		if (!checksOk) {
			return false;
		}

		final Battle battle = getBattle(client);

		final String scriptTagsOrig = (String)args.getSentences().get(0);

		final Map<String, String> scriptTags = parseScriptTags(scriptTagsOrig);

		final String scriptTagsClean = createScriptTagsString(scriptTags);

		battle.getScriptTags().putAll(scriptTags);

		// relay the valid pairs
		if (scriptTagsClean.length() > 0) {
			battle.sendToAllClients("SETSCRIPTTAGS " + scriptTagsClean);
		}
	}

	private static Map<String, String> parseScriptTags(final String keyValuePairs) {

		final Map<String, String> scriptTags = new HashMap<String, String>();

		final String[] pairs = keyValuePairs.split("\t");
		for (final String pair : pairs) {
			parseKeyValuePair(scriptTags, pair);
		}

		return scriptTags;
	}

	private static boolean isValidKey(final String key) {

		if (key.isEmpty()) {
			return false;
		}
		if (INVALID_KEY.matcher(key).matches()) {
			return false;
		}

		return true;
	}

	private static boolean isValidValue(final String value) {

		// forbid trailing/leading spaces
		if (!value.equals(value.trim())) {
			return false;
		}
		if (INVALID_VALUE.matcher(value).matches()) {
			return false;
		}

		return true;
	}

	private static boolean parseKeyValuePair(
			final Map<String, String> properties,
			final String keyValuePair)
	{
		final int equalPos = keyValuePair.indexOf('=');
		if (equalPos < 1) {
			return false;
		}

		// parse the key
		final String key = keyValuePair.substring(0, equalPos).toLowerCase();
		if (!isValidKey(key)) {
			return false;
		}

		// parse the value
		final String value = keyValuePair.substring(equalPos + 1);
		if (!isValidValue(value)) {
			return false;
		}

		properties.put(key, value);

		return true;
	}

	private static String createScriptTagsString(final Map<String, String> scriptTags) {

		final StringBuilder scriptTagsClean = new StringBuilder();

		for (final Entry<String, String> entry : scriptTags.entrySet()) {
			if (scriptTagsClean.length() > 0) {
				scriptTagsClean.append("\t");
			}
			scriptTagsClean.append(entry.getKey()).append("=").append(entry.getValue());
		}

		return scriptTagsClean.toString();
	}
}
