package org.golde.auncher;

import java.util.HashSet;
import java.util.Set;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

@SuppressWarnings("serial")
/**
 * This class is a special text field for Minecraft usernames.
 * Usernames can only be valid if the following is met:
 *		- must be no less the 3 characters long
 *		- must be at max 16 characters long
 *		- only uses A-Z a-z 0-9 _ 
 *
 * @author Eric Golde
 *
 */
public class UsernameFieldHandler extends PlainDocument {
	
	//max string length
	private static final int MAX = 16;
	
	private static final Set<Character> allowedCharacters = new HashSet<Character>();
	
	public interface OnTypedCallback {
		public void onTyped(int length);
	}
	
	//allowed characters populating the array
	static {
		for(char c : "abcdefghijklmnopqrstuvwyxzABCDEFGHIJKLMNOPQRSTUVWYXZ0123456789_".toCharArray()) {
			allowedCharacters.add(c);
		}
	}
	
	private final OnTypedCallback callback;
	
	public UsernameFieldHandler(OnTypedCallback callback) {
		this.callback = callback;
	}
	
	//Every time a character is typed
	@Override
	public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
		if (str == null) return;
		
		//I feel like there should be another way, but this works as of now
		for(char c : str.toCharArray()) {
			
			if(!allowedCharacters.contains(c)) {
				return;
			}
			
		}

		int currLength = getLength() + str.length();
		
		callback.onTyped(currLength);
		
		//allow that character to be typed
		if (currLength <= MAX) {
			super.insertString(offset, str, attr);
		}
	}
	
	@Override
	public String getText(int offset, int length) throws BadLocationException {
		callback.onTyped(offset);
		return super.getText(offset, length);
	}

}
