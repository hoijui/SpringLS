/*
	Copyright (c) 2015 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.springls.commands;


import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The parsed arguments to a command.
 * @author hoijui
 */
public class ParsedCommandArguments {

	private final String fullCommand;
	private final List<?> words;
	private final List<?> sentences;
	private final Map<String, ?> named;

	public ParsedCommandArguments(
			final String fullCommand,
			final List<?> words,
			final List<?> sentences)
	{
		this.fullCommand = fullCommand;
		this.words = Collections.unmodifiableList(words);
		this.sentences = Collections.unmodifiableList(sentences);
		this.named = null;
	}

	public ParsedCommandArguments(
			final String fullCommand,
			final Map<String, ?> named)
	{
		this.fullCommand = fullCommand;
		this.words = null;
		this.sentences = null;
		this.named = Collections.unmodifiableMap(named);
	}

	public String getFullCommand() {
		return fullCommand;
	}

	public List<?> getWords() {
		return words;
	}

	public List<?> getSentences() {
		return sentences;
	}

	public Map<String, ?> getNamed() {
		return named;
	}
}
