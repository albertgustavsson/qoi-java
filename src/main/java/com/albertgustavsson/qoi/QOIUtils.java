package com.albertgustavsson.qoi;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class QOIUtils {
	public static long byteArrayToBigEndianUnsignedInt(byte[] bytes) {
		if (bytes.length != 4) {
			throw new IllegalArgumentException("Byte array is not of length 4");
		}
		byte[] longBytes = new byte[8];
		System.arraycopy(bytes, 0, longBytes, 4, 4);
		final ByteBuffer byteBuffer = ByteBuffer.wrap(longBytes);
		byteBuffer.order(ByteOrder.BIG_ENDIAN);
		return byteBuffer.getLong();
	}

	public static int byteToUint8(byte b) {
		return (int)b & 0xFF;
	}

	static int wrapByteValue(int value) {
		return ((value % 256) + 256) % 256;
	}

	static int colorHash(Color color) {
		return (color.getRed() * 3 + color.getGreen() * 5 + color.getBlue() * 7 + color.getAlpha() * 11) % 64;
	}

	static String colorString(Color color) {
		return "[" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ", " + color.getAlpha() + "]";
	}
}
