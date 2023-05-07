package com.albertgustavsson.qoi;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QOIUtilsTest {

	@Test
	void byteArrayToUnsignedInt() {
		assertEquals(0,QOIUtils.byteArrayToUnsignedInt(new byte[] {0,0,0,0}, ByteOrder.BIG_ENDIAN));
		assertEquals(0,QOIUtils.byteArrayToUnsignedInt(new byte[] {0,0,0,0}, ByteOrder.LITTLE_ENDIAN));

		assertEquals(1,QOIUtils.byteArrayToUnsignedInt(new byte[] {0,0,0,1}, ByteOrder.BIG_ENDIAN));
		assertEquals(1,QOIUtils.byteArrayToUnsignedInt(new byte[] {1,0,0,0}, ByteOrder.LITTLE_ENDIAN));

		assertEquals(16777216,QOIUtils.byteArrayToUnsignedInt(new byte[] {1,0,0,0}, ByteOrder.BIG_ENDIAN));
		assertEquals(16777216,QOIUtils.byteArrayToUnsignedInt(new byte[] {0,0,0,1}, ByteOrder.LITTLE_ENDIAN));

		assertEquals(4294967295L,QOIUtils.byteArrayToUnsignedInt(new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255}, ByteOrder.BIG_ENDIAN));
		assertEquals(4294967295L,QOIUtils.byteArrayToUnsignedInt(new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255}, ByteOrder.LITTLE_ENDIAN));
	}

	@Test
	void unsignedIntToByteArray() {
		assertArrayEquals(new byte[] {0,0,0,0},QOIUtils.unsignedIntToByteArray(0, ByteOrder.BIG_ENDIAN));
		assertArrayEquals(new byte[] {0,0,0,0},QOIUtils.unsignedIntToByteArray(0, ByteOrder.LITTLE_ENDIAN));

		assertArrayEquals(new byte[] {0,0,0,1},QOIUtils.unsignedIntToByteArray(1, ByteOrder.BIG_ENDIAN));
		assertArrayEquals(new byte[] {1,0,0,0},QOIUtils.unsignedIntToByteArray(1, ByteOrder.LITTLE_ENDIAN));

		assertArrayEquals(new byte[] {1,0,0,0},QOIUtils.unsignedIntToByteArray(16777216, ByteOrder.BIG_ENDIAN));
		assertArrayEquals(new byte[] {0,0,0,1},QOIUtils.unsignedIntToByteArray(16777216, ByteOrder.LITTLE_ENDIAN));

		assertArrayEquals(new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255},QOIUtils.unsignedIntToByteArray(4294967295L, ByteOrder.BIG_ENDIAN));
		assertArrayEquals(new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255},QOIUtils.unsignedIntToByteArray(4294967295L, ByteOrder.LITTLE_ENDIAN));
	}

	@Test
	void byteToUint8() {
		assertEquals(0, QOIUtils.byteToUint8((byte) 0));
		assertEquals(128, QOIUtils.byteToUint8((byte) 0b10000000));
		assertEquals(255, QOIUtils.byteToUint8((byte) 0b11111111));
	}

	@Test
	void wrapByteValue() {
		assertEquals(0, QOIUtils.wrapByteValue(0));
		assertEquals(1, QOIUtils.wrapByteValue(1));
		assertEquals(0, QOIUtils.wrapByteValue(256));
		assertEquals(1, QOIUtils.wrapByteValue(257));
	}

	@Test
	void colorHash() {
		assertEquals(53,QOIUtils.colorHash(Color.BLACK));
		assertEquals(53,QOIUtils.colorHash(Color.GRAY));
		assertEquals(38,QOIUtils.colorHash(Color.WHITE));

		assertEquals(50,QOIUtils.colorHash(Color.RED));
		assertEquals(48,QOIUtils.colorHash(Color.GREEN));
		assertEquals(46,QOIUtils.colorHash(Color.BLUE));

		assertEquals(41,QOIUtils.colorHash(Color.CYAN));
		assertEquals(43,QOIUtils.colorHash(Color.MAGENTA));
		assertEquals(45,QOIUtils.colorHash(Color.YELLOW));
	}

	@Test
	void createRunChunk() {
		assertEquals((byte) 0b11000000, QOIUtils.createRunChunk(1));
		assertEquals((byte) 0b11000001, QOIUtils.createRunChunk(2));
		assertEquals((byte) 0b11100000, QOIUtils.createRunChunk(33));
		assertEquals((byte) 0b11111101, QOIUtils.createRunChunk(62));
	}

	@Test
	void createIndexChunk() {
		assertEquals((byte) 0b00000000, QOIUtils.createIndexChunk(0));
		assertEquals((byte) 0b00000001, QOIUtils.createIndexChunk(1));
		assertEquals((byte) 0b00100000, QOIUtils.createIndexChunk(32));
		assertEquals((byte) 0b00111111, QOIUtils.createIndexChunk(63));
	}

	@Test
	void createDiffChunk() {
		assertEquals((byte) 0b01000000, QOIUtils.createDiffChunk(-2, -2, -2));
		assertEquals((byte) 0b01101010, QOIUtils.createDiffChunk(0, 0, 0));
		assertEquals((byte) 0b01111111, QOIUtils.createDiffChunk(1, 1, 1));

		assertEquals((byte) 0b01001111, QOIUtils.createDiffChunk(-2, 1, 1));
		assertEquals((byte) 0b01110011, QOIUtils.createDiffChunk(1, -2, 1));
		assertEquals((byte) 0b01111100, QOIUtils.createDiffChunk(1, 1, -2));
	}

	@Test
	void createLumaChunk() {
		assertArrayEquals(new byte[] {(byte) 0b10100000, (byte) 0b10001000}, QOIUtils.createLumaChunk(0 , 0, 0));
		assertArrayEquals(new byte[] {(byte) 0b10000000, (byte) 0b00000000}, QOIUtils.createLumaChunk(-8, -32, -8));
		assertArrayEquals(new byte[] {(byte) 0b10111111, (byte) 0b11111111}, QOIUtils.createLumaChunk(7, 31, 7));
		assertArrayEquals(new byte[] {(byte) 0b10111111, (byte) 0b10001111}, QOIUtils.createLumaChunk(0, 31, 7));
		assertArrayEquals(new byte[] {(byte) 0b10111111, (byte) 0b11111000}, QOIUtils.createLumaChunk(7, 31, 0));
	}

	@Test
	void createRGBChunk() {
		assertArrayEquals(new byte[] {(byte) 0b11111110, (byte) 0, (byte) 0, (byte) 0}, QOIUtils.createRGBChunk(Color.BLACK));
		assertArrayEquals(new byte[] {(byte) 0b11111110, (byte) 128, (byte) 128, (byte) 128}, QOIUtils.createRGBChunk(Color.GRAY));
		assertArrayEquals(new byte[] {(byte) 0b11111110, (byte) 255, (byte) 255, (byte) 255}, QOIUtils.createRGBChunk(Color.WHITE));
		assertArrayEquals(new byte[] {(byte) 0b11111110, (byte) 255, (byte) 0, (byte) 0}, QOIUtils.createRGBChunk(Color.RED));
		assertArrayEquals(new byte[] {(byte) 0b11111110, (byte) 0, (byte) 255, (byte) 0}, QOIUtils.createRGBChunk(Color.GREEN));
		assertArrayEquals(new byte[] {(byte) 0b11111110, (byte) 0, (byte) 0, (byte) 255}, QOIUtils.createRGBChunk(Color.BLUE));
	}

	@Test
	void createRGBAChunk() {
		assertArrayEquals(new byte[] {(byte) 0b11111111, (byte) 0, (byte) 0, (byte) 0, (byte) 255}, QOIUtils.createRGBAChunk(Color.BLACK));
		assertArrayEquals(new byte[] {(byte) 0b11111111, (byte) 128, (byte) 128, (byte) 128, (byte) 255}, QOIUtils.createRGBAChunk(Color.GRAY));
		assertArrayEquals(new byte[] {(byte) 0b11111111, (byte) 255, (byte) 255, (byte) 255, (byte) 255}, QOIUtils.createRGBAChunk(Color.WHITE));
		assertArrayEquals(new byte[] {(byte) 0b11111111, (byte) 255, (byte) 0, (byte) 0, (byte) 255}, QOIUtils.createRGBAChunk(Color.RED));
		assertArrayEquals(new byte[] {(byte) 0b11111111, (byte) 0, (byte) 255, (byte) 0, (byte) 255}, QOIUtils.createRGBAChunk(Color.GREEN));
		assertArrayEquals(new byte[] {(byte) 0b11111111, (byte) 0, (byte) 0, (byte) 255, (byte) 255}, QOIUtils.createRGBAChunk(Color.BLUE));
		assertArrayEquals(new byte[] {(byte) 0b11111111, (byte) 255, (byte) 255, (byte) 255, (byte) 0}, QOIUtils.createRGBAChunk(new Color(255,255,255,0)));
	}
}
