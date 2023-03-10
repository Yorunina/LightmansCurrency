package io.github.lightman314.lightmanscurrency.client.util;

import io.github.lightman314.lightmanscurrency.util.MathUtil;
import io.github.lightman314.lightmanscurrency.util.NumberUtil;
import net.minecraft.client.gui.widget.TextFieldWidget;

public class TextInputUtil {

	private static final String INTEGER_WHITELIST = "0123456789";
	private static final String FLOAT_WHITELIST = "0123456789.";
	
	public static int getIntegerValue(TextFieldWidget textInput)
	{
		return getIntegerValue(textInput, 0);
	}
	
	public static int getIntegerValue(TextFieldWidget textInput, int defaultValue)
	{
		return NumberUtil.GetIntegerValue(textInput.getValue(), defaultValue);
	}
	
	public static boolean isLong(TextFieldWidget textInput)
	{
		return isLong(textInput.getValue());
	}
	
	public static boolean isLong(String text)
	{
		if(text == null)
			return false;
		try
		{
			@SuppressWarnings("unused")
			long i = Long.parseLong(text);
		} 
		catch(NumberFormatException nfe)
		{
			return false;
		}
		return true;
	}
	
	public static long getLongValue(TextFieldWidget textInput)
	{
		return getLongValue(textInput, 0);
	}
	
	public static long getLongValue(TextFieldWidget textInput, int defaultValue)
	{
		if(isLong(textInput))
			return Long.parseLong(textInput.getValue());
		return defaultValue;
	}
	
	public static boolean isFloat(TextFieldWidget textInput)
	{
		return isFloat(textInput.getValue());
	}
	
	public static boolean isFloat(String text)
	{
		if(text == null)
			return false;
		try {
			@SuppressWarnings("unused")
			float f = Float.parseFloat(text);
		} catch(NumberFormatException nfe)
		{
			return false;
		}
		return true;
	}
	
	public static float getFloatValue(TextFieldWidget textInput)
	{
		return getFloatValue(textInput, 0f);
	}
	
	public static float getFloatValue(TextFieldWidget textInput, float defaultValue)
	{
		if(isFloat(textInput))
			return Float.parseFloat(textInput.getValue());
		return defaultValue;
	}
	
	public static boolean isDouble(TextFieldWidget textInput)
	{
		return isDouble(textInput.getValue());
	}
	
	public static boolean isDouble(String text)
	{
		if(text == null)
			return false;
		try {
			@SuppressWarnings("unused")
			double d = Double.parseDouble(text);
		} catch(NumberFormatException nfe)
		{
			return false;
		}
		return true;
	}
	
	public static double getDoubleValue(TextFieldWidget textInput)
	{
		return getDoubleValue(textInput, 0d);
	}
	
	public static double getDoubleValue(TextFieldWidget textInput, double defaultValue)
	{
		if(isDouble(textInput))
			return Double.parseDouble(textInput.getValue());
		return defaultValue;
	}
	
	/**
	 * Also works for long values.
	 */
	public static void whitelistInteger(TextFieldWidget textInput)
	{
		whitelistText(textInput, INTEGER_WHITELIST);
	}
	
	/**
	 * Also works for long values.
	 */
	public static void whitelistInteger(TextFieldWidget textInput, long minValue, long maxValue)
	{
		whitelistText(textInput, INTEGER_WHITELIST);
		if(textInput.getValue().length() > 0)
		{
			long currentValue = getLongValue(textInput);
			if(currentValue < minValue || currentValue > maxValue)
			{
				currentValue = MathUtil.clamp(currentValue, minValue, maxValue);
				textInput.setValue(Long.toString(currentValue));
			}
		}
	}
	
	/**
	 * Also works for double values.
	 */
	public static void whitelistFloat(TextFieldWidget textInput)
	{
		whitelistText(textInput, FLOAT_WHITELIST);
	}
	
	public static void whitelistText(TextFieldWidget textInput, String allowedChars)
	{
		StringBuilder newText = new StringBuilder(textInput.getValue());
		for(int i = 0; i < newText.length(); i++)
		{
			boolean allowed = false;
			for(int x = 0; x < allowedChars.length() && !allowed; x++)
			{
				if(allowedChars.charAt(x) == newText.charAt(i))
					allowed = true;
			}
			if(!allowed)
			{
				newText.deleteCharAt(i--);
			}
		}
		textInput.setValue(newText.toString());
	}
	
}
