package org.golde.auncher;

import java.util.HashSet;
import java.util.Set;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

@SuppressWarnings("serial")
public class UsernameFieldHandler extends PlainDocument {
	
	private static final int MAX = 16;
	
	private static final Set<Character> allowedCharacters = new HashSet<Character>();
	
	static {
		for(char c : "abcdefghijklmnopqrstuvwyxzABCDEFGHIJKLMNOPQRSTUVWYXZ0123456789_".toCharArray()) {
			allowedCharacters.add(c);
		}
	}

	public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
		if (str == null) return;

		for(char c : str.toCharArray()) {
			
			if(!allowedCharacters.contains(c)) {
				return;
			}
			
		}

		int currLength = getLength() + str.length();
		
		if (currLength <= MAX) {
			super.insertString(offset, str, attr);
		}
	}

}
