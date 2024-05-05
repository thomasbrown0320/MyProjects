package prog10;

import prog02.GUI;
import prog02.UserInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Jumble {
	/**
	 * Sort the letters in a word.
	 *
	 * @param word Input word to be sorted, like "computer".
	 * @return Sorted version of word, like "cemoptru".
	 */
	public static String sort(String word) {
		char[] sorted = new char[word.length()];
		for (int n = 0; n < word.length(); n++) {
			char c = word.charAt(n);
			int i = n;

			while (i > 0 && c < sorted[i - 1]) {
				sorted[i] = sorted[i - 1];
				i--;
			}

			sorted[i] = c;
		}

		return new String(sorted, 0, word.length());
	}

	public static void main(String[] args) {
		UserInterface ui = new GUI("Jumble");
		// UserInterface ui = new ConsoleUI();

		// Map<String,String> map = new TreeMap<String,String>();
		// Map<String,String> map = new PDMap();
		//Map<String,String> map = new LinkedMap<String,String>();
		//Map<String,String> map = new SkipMap<String,String>();
		//Map<String,String> map = new BST<String,String>();
		// Map<String,String> map = new BTree(128);
		Map<String, List<String>> map = new HashTable<>();
		Scanner in = null;
		do {
			String fileName = null;
			try {
				fileName = ui.getInfo("Enter word file.");
				if (fileName == null)
					return;
				in = new Scanner(new File(fileName));
			} catch (Exception e) {
				ui.sendMessage(fileName + " not found.");
				System.out.println(e);
				System.out.println("Try again.");
			}
		} while (in == null);

		int n = 0;
		while (in.hasNextLine()) {
			String word = in.nextLine();
			if (n++ % 1000 == 0)
				System.out.println(word + " sorted is " + sort(word));

			// EXERCISE: Insert an entry for word into map.
			// What is the key?  What is the value?
			///
			String sortedWord = sort(word);

			if (map.containsKey(sortedWord)) {

				List<String> list = map.get(sortedWord);
				list.add(word);
			} else {

				List<String> newList = new ArrayList<>();
				newList.add(word);
				map.put(sortedWord, newList);
			}
		/*
		map.remove(key);
		if(map.get(key)!=null) {
			System.out.println("ERROR");
		}
		map.put(key,word);
		 */

			///
		}

		while (true) {
			String jumble = ui.getInfo("Enter jumble.");
			if (jumble == null) {
				StringBuilder result = new StringBuilder();
				while (true) {
					String clues = ui.getInfo("Enter letters from clues.");
					if (clues == null)
						break;
					String lengthOfPun = ui.getInfo("How many letters in first word?");
					String sortedLetters = sort(clues);

					boolean foundMatch = false;
					for (String key1 : map.keySet()) {
						if (key1.length() == Integer.parseInt(lengthOfPun)) {
							String key2 = "";
							int key1index = 0;
							for (int i = 0; i < sortedLetters.length(); i++) {
								char currentLetter = sortedLetters.charAt(i);
								if (key1index >= key1.length())
									key2 += currentLetter;
								else if (currentLetter == key1.charAt(key1index))
									key1index++;
								else if (currentLetter > key1.charAt(key1index))
									break;
								else
									key2 += currentLetter;
							}
							if (key1index==key1.length() && map.containsKey(key2)) {
								foundMatch = true;
								System.out.println("Lists for " + key1 + ": " + map.get(key1));
								System.out.println("Lists for " + key2 + ": " + map.get(key2));
								result.append(map.get(key1)).append(" ").append(map.get(key2)).append("\n");
							}
						}
					}
					if (foundMatch) {
						ui.sendMessage(result.toString());
						break;
					} else {
						ui.sendMessage("No match found for the provided clues.");
					}
				}
			} else {
				// Look up the jumble in the map
				List<String> words = map.get(sort(jumble));
				if (words == null)
					ui.sendMessage("No match for " + jumble);
				else
					ui.sendMessage(jumble + " unjumbled is " + words);
			}
		}



		// EXERCISE:  Look up the jumble in the map.
					// What key do you use?
					///
				}
		}

