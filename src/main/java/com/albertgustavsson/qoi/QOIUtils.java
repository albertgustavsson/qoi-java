package com.albertgustavsson.qoi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class QOIUtils {
	private final static Logger logger = LogManager.getLogger(QOIDecoder.class);

	public static void showImage(Image image) {
		ImageIcon icon = new ImageIcon(image);
		JFrame frame = new JFrame();
		frame.setLayout(new FlowLayout());
		frame.setSize(image.getWidth(null) + 50, image.getHeight(null) + 50);
		JLabel lbl = new JLabel();
		lbl.setIcon(icon);
		frame.add(lbl);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public static void showImage(Image image, float scale) {
		showImage(image.getScaledInstance((int) (image.getWidth(null)*scale), (int) (image.getHeight(null)*scale), Image.SCALE_REPLICATE));
	}

	public static long byteArrayToUnsignedInt(byte[] bytes, ByteOrder order) {
		if (bytes.length != 4) {
			throw new IllegalArgumentException("Byte array is not of length 4");
		}
		byte[] longBytes = new byte[8];
		System.arraycopy(bytes, 0, longBytes, order==ByteOrder.BIG_ENDIAN?4:0, 4);
		final ByteBuffer byteBuffer = ByteBuffer.wrap(longBytes);
		byteBuffer.order(order);
		return byteBuffer.getLong();
	}

	public static byte[] unsignedIntToByteArray(long value, ByteOrder order) {
		byte[] longBytes = ByteBuffer.allocate(8).order(order).putLong(value).array();
		byte[] bytes = new byte[4];
		System.arraycopy(longBytes, order==ByteOrder.BIG_ENDIAN?4:0, bytes, 0, 4);
		return bytes;
	}

	public static int byteToUint8(byte b) {
		return (int)b & 0xFF;
	}

	public static int wrapByteValue(int value) {
		return ((value % 256) + 256) % 256;
	}

	public static int colorHash(Color color) {
		return (color.getRed() * 3 + color.getGreen() * 5 + color.getBlue() * 7 + color.getAlpha() * 11) % 64;
	}

	public static String colorString(Color color) {
		return "[" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ", " + color.getAlpha() + "]";
	}

	public static byte createRunChunk(int count) {
		return (byte) (0xC0 | ((count-1) & 0x3F));
	}

	public static byte createIndexChunk(int index) {
		return (byte) (index & 0x3F);
	}

	public static byte createDiffChunk(int diffRed, int diffGreen, int diffBlue) {
		return (byte) (0x40 | (((diffRed+2)&0x3)<<4) | (((diffGreen+2)&0x3)<<2) | ((diffBlue+2)&0x3));
	}

	public static byte[] createLumaChunk(int drdg, int dg, int dbdg) {
		return new byte[] {(byte) (0x80 | ((dg+32)&0x3F)), (byte) ((((drdg+8)&0xF)<<4) | ((dbdg+8)&0xF))};
	}

	public static byte[] createRGBChunk(Color color) {
		return new byte[] {(byte) 0xFE, (byte) (color.getRed()&0xFF), (byte) (color.getGreen()&0xFF), (byte) (color.getBlue()&0xFF)};
	}

	public static byte[] createRGBAChunk(Color color) {
		return new byte[] {(byte) 0xFF, (byte) (color.getRed()&0xFF), (byte) (color.getGreen()&0xFF), (byte) (color.getBlue()&0xFF), (byte) (color.getAlpha()&0xFF)};
	}
}
