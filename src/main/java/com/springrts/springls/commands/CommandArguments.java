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


import com.springrts.springls.Client;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The description of the arguments to a specific command.
 * @author hoijui
 */
public class CommandArguments {

	private final Logger log  = LoggerFactory.getLogger(CommandArguments.class);

	/**
	 * We need this, because there might be commands without arguments
	 * which still may support (or not) named arguments in the future.
	 */
	private final boolean usingNamedArguments;
	private final List<IndexedArgument> words;
	/**
	 * Whether the command supports variable words.
	 * If true, then the command supports a variable amount of word arguments.
	 * This is only possible if there are no sentence arguments.
	 */
	private final boolean supportingVariableWords;
	private final List<IndexedArgument> sentences;
	/**
	 * Whether the command supports variable sentences.
	 * If true, then the command supports a variable amount of sentence
	 * arguments.
	 */
	private final boolean supportingVariableSentences;
	private final Set<NamedArgument> namedArgs;
	private final List<ParsingExceptionListener> parsingExceptionListeners;

	public CommandArguments(final boolean usingNamedArguments) {

		this.usingNamedArguments = usingNamedArguments;
		this.supportingVariableWords = false;
		this.supportingVariableSentences = false;
		if (usingNamedArguments) {
			this.words = null;
			this.sentences = null;
			this.namedArgs = Collections.EMPTY_SET;
		} else {
			this.words = Collections.EMPTY_LIST;
			this.sentences = Collections.EMPTY_LIST;
			this.namedArgs = null;
		}
		this.parsingExceptionListeners = new LinkedList<ParsingExceptionListener>();
	}

	public CommandArguments(
			final boolean supportingVariableWords,
			final IndexedArgument... words)
	{
		this.usingNamedArguments = false;
		this.words = Collections.unmodifiableList(Arrays.asList(words));
		this.supportingVariableWords = supportingVariableWords;
		this.sentences = Collections.EMPTY_LIST;
		this.supportingVariableSentences = false;
		this.namedArgs = null;
		this.parsingExceptionListeners = new LinkedList<ParsingExceptionListener>();
	}

	public CommandArguments(final IndexedArgument... words) {
		this(false, words);
	}

	public CommandArguments(
			final List<IndexedArgument> words,
			final boolean supportingVariableSentences,
			final IndexedArgument... sentences)
	{
		this.usingNamedArguments = false;
		this.words = Collections.unmodifiableList(words);
		this.supportingVariableWords = false;
		this.sentences = Collections.unmodifiableList(Arrays.asList(sentences));
		this.supportingVariableSentences = supportingVariableSentences;
		this.namedArgs = null;
		if (!this.sentences.isEmpty()) {
			for (final IndexedArgument word : words) {
				if (word.isOptional()) {
					throw new IllegalArgumentException(
							"Due to arguments separators"
							+ " (SPACE for words, TAB for sentences),"
							+ " it is not possible to use sentence arguments"
							+ " together with a variable ammount of word"
							+ " arguments");
				}
			}
		}
		this.parsingExceptionListeners = new LinkedList<ParsingExceptionListener>();
	}

	public CommandArguments(
			final List<IndexedArgument> words,
			final IndexedArgument... sentences)
	{
		this(words, false, sentences);
	}

	public CommandArguments(final NamedArgument... namedArgs) {

		this.usingNamedArguments = true;
		this.words = null;
		this.supportingVariableWords = false;
		this.sentences = null;
		this.supportingVariableSentences = false;
		this.namedArgs = Collections.unmodifiableSet(new HashSet<NamedArgument>(
				Arrays.asList(namedArgs)));
		this.parsingExceptionListeners = new LinkedList<ParsingExceptionListener>();
	}

	public void addParsingExceptionListener(final ParsingExceptionListener lst) {
		parsingExceptionListeners.add(lst);
	}

	public void removeParsingExceptionListener(final ParsingExceptionListener lst) {
		parsingExceptionListeners.remove(lst);
	}

	private void fireParsingExceptionOccurred(final CommandParsingException exception) {

		final ParsingExceptionEvent evt = new ParsingExceptionEvent(this, exception);
		for (final ParsingExceptionListener lst : parsingExceptionListeners) {
			lst.parsingExceptionOccurred(evt);
		}
	}

	private static class StringToParse implements Iterator<String> {

		private final String str;
		private char delimiter;
		private int nextIndex;

		StringToParse(final String str, final int nextIndex, final char delimiter) {

			this.str = str;
			this.delimiter = delimiter;
			this.nextIndex = nextIndex;
		}

		public void setDelimiter(final char delimiter) {
			this.delimiter = delimiter;
		}

		@Override
		public String next() {

			final String part;

			final int endOfPart = str.indexOf(delimiter, nextIndex);
			if (endOfPart == -1) {
				part = str.substring(nextIndex);
				nextIndex = -1;
			} else {
				part = str.substring(nextIndex, endOfPart - 1);
				nextIndex = endOfPart + 1;
			}

			return part;
		}

		/**
		 * Indicates that there is nothing of the string left.
		 * @return
		 */
		private boolean isEnd() {
			return (nextIndex == -1) || (nextIndex == str.length());
		}

		@Override
		public boolean hasNext() {
			return !isEnd();
		}
	}

	private Object convert(final Client client, final IndexedArgument arg, final String valueStr) {

		try {
			final Object convertedValue = arg.getValueParser().parse(valueStr);
			return convertedValue;
		} catch (final Exception ex) {
			if (client != null) {
				TODO;
				client.sendLine("SERVERMSG Bad USERID command - userID field should be an integer"); // USERID @Deprecated
				client.sendLine("SERVERMSG Invalid 'mode' parameter (has to be 0 or 1)!"); // SETBOTMODE
				{ // REMOVESTARTRECT
					client.sendLine(String.format(
							"SERVERMSG Serious error: inconsistent data (%s command)."
							+ " You will now be disconnected ...", getCommandName()));
					getContext().getClients().killClient(client,
							"Quit: inconsistent data");
				}
				client.sendLine(String.format(
						"SERVERMSG %s failed: Invalid argument - should be an integer",
						getCommandName())); // MUTE
				client.sendLine("SERVERMSG LONGTIMETODATE failed: invalid argument."); // LONGTIMETODATE
				client.sendLine("DENIED <cpu> field should be an integer"); // LOGIN
				client.sendLine("JOINBATTLEFAILED No battle ID!"); // JOINBATTLE
				client.sendLine("SERVERMSG Invalid IP address/range: " + ipAddress); // IP2COUNTRY
				client.sendLine(String.format(
						"FORCEJOINBATTLEFAILED %s %s", userName,
						"Invalid destination battle ID (needs to be an integer): " + destinationBattleIdStr)); // FORCEJOINBATTLE
			}
			log.trace("TODO"); TODO;
			return null;
		}
	}

	public ParsedCommandArguments parse(final Client client, final String command, final int argsStartIndex) {

		final StringToParse input = new StringToParse(command, argsStartIndex, ' ');
		if (isUsingNamedArguments()) {
			input.setDelimiter('\t');
			if (!input.hasNext()) {
				return new ParsedCommandArguments(
						command,
						Collections.EMPTY_MAP);
			}

			// parse name-value pairs
			final Map<String, Object> parsedNamedArgs
					= new HashMap<String, Object>();
			while (input.hasNext()) {
				final String nameValuePair = input.next();
				final String[] nameAndValue = nameValuePair.split("=", 2);
				if (nameAndValue.length != 2) {
					fireParsingExceptionOccurred(new CommandParsingException(
							"Not a valid named argument format: \"" + nameValuePair + "\""));
					return null;
				}
				parsedNamedArgs.put(nameAndValue[0].trim(), nameAndValue[1]);
			}

			// check if (required) keys are present
			final Set<String> parsedNamedArgsKeysCopy
					= new HashSet<String>(parsedNamedArgs.keySet());
			for (final NamedArgument definedArgument : getNamedArgs()) {
				final String definedName = definedArgument.getName();
				final String valueStr = (String)parsedNamedArgs.get(definedName);
				if (definedName != null) {
					final Object convertedValue = convert(client, definedArgument, valueStr);
					parsedNamedArgs.put(definedName, convertedValue);
					parsedNamedArgsKeysCopy.remove(definedName);
				} else if (!definedArgument.isOptional()) {
					fireParsingExceptionOccurred(new CommandParsingException(String.format(
							"Missing required named argument %s in command \"%s\"",
							definedArgument.getName(),
							command);
					log.debug("Missing required named argument {} in command \"{}\"",
							definedArgument.getName(),
							command);
					return null;
				}
			}

			// check if unknown keys are present
			if (!parsedNamedArgsKeysCopy.isEmpty() && log.isDebugEnabled()) {
				log.debug("Unknown named arguments {} in command \"{}\"",
						parsedNamedArgsKeysCopy.toString(),
						command);
			}

			return new ParsedCommandArguments(command, parsedNamedArgs);
		} else {
			input.setDelimiter(' ');
			if (!input.hasNext()) {
				return new ParsedCommandArguments(
						command,
						Collections.EMPTY_LIST,
						Collections.EMPTY_LIST);
			}

			// parse & convert words
			final int numWords = supportingVariableWords
					? Integer.MAX_VALUE : getWords().size();
			final List<Object> parsedWords
					= new ArrayList<Object>(getWords().size());
			final Iterator<IndexedArgument> wordsIt = getWords().iterator();
			IndexedArgument curWord = null;
			while ((parsedWords.size() < numWords) && input.hasNext()) {
				final String parsedWord = input.next();
				if (wordsIt.hasNext()) {
					curWord = wordsIt.next();
				}
				final Object convertedValue = convert(client, curWord, parsedWord);
				parsedWords.add(convertedValue);
			}
			// check if required ones are present
			if (parsedWords.size() < getWords().size()) {
				log.debug("Missing {} required word argument(s) in command \"{}\"",
						getWords().size() - parsedWords.size(),
						command);
				return null;
			}

			// parse & convert sentences
			input.setDelimiter('\t');
			final int numSentences = supportingVariableSentences
					? Integer.MAX_VALUE : getSentences().size();
			final List<Object> parsedSentences
					= new ArrayList<Object>(getSentences().size());
			final Iterator<IndexedArgument> sentenceIt = getSentences().iterator();
			IndexedArgument curSentence = null;
			while ((parsedSentences.size() < numSentences) && input.hasNext()) {
				final String parsedSentence = input.next();
				if (wordsIt.hasNext()) {
					curSentence = sentenceIt.next();
				}
				final Object convertedValue = convert(client, curSentence, parsedSentence);
				parsedSentences.add(convertedValue);
			}
			// check if required ones are present
			if (parsedSentences.size() < getSentences().size()) {
				log.debug("Missing {} required sentence argument(s) in command \"{}\"",
						getSentences().size() - parsedSentences.size(),
						command);
				return null;
			}

			return new ParsedCommandArguments(
					command, parsedWords, parsedSentences);
		}
	}

	public boolean isUsingNamedArguments() {
		return usingNamedArguments;
	}

	public List<IndexedArgument> getWords() {
		return words;
	}

	public List<IndexedArgument> getSentences() {
		return sentences;
	}

	public Set<NamedArgument> getNamedArgs() {
		return namedArgs;
	}
}
