package com.albertgustavsson.qoi;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QOIDecoder {
	private final static Logger logger = LogManager.getLogger(QOIDecoder.class);

	private BufferedImage image;
	private long pixelIndex = 0;

	private Color previousPixel = new Color(0,0,0,255);
	private final Color[] pixelArray = new Color[64];

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			logger.error("Required parameters: <filename>");
		}
		String filename = args[0];

		QOIDecoder decoder = new QOIDecoder();
		BufferedImage image = decoder.decode(filename);

		logger.debug("Image has been decoded.");

		float scale;
		if (image.getWidth() > image.getHeight()) {
			scale = (float) 2500 /image.getWidth();
		} else {
			scale = (float) 1400 /image.getHeight();
		}
		QOIUtils.showImage(image, scale);
	}

	public QOIDecoder() {
		Arrays.setAll(pixelArray, value -> new Color(0,0,0,0));
	}

	public BufferedImage decode(String inputFileName) throws IOException {
		try (FileInputStream fileInputStream = new FileInputStream(inputFileName)) {
			byte[] header = fileInputStream.readNBytes(14);
			byte[] magicBytes = Arrays.copyOfRange(header, 0, 4);
			assert new String(magicBytes).equals("qoif");

			long width = QOIUtils.byteArrayToUnsignedInt(Arrays.copyOfRange(header, 4, 8), ByteOrder.BIG_ENDIAN);
			long height = QOIUtils.byteArrayToUnsignedInt(Arrays.copyOfRange(header, 8, 12), ByteOrder.BIG_ENDIAN);
			if (width > Integer.MAX_VALUE || height > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("Image dimensions exceed implementation limits. The maximum supported resolution is " + Integer.MAX_VALUE + " x " + Integer.MAX_VALUE);
			}

			byte channels = header[12];
			byte colorSpace = header[13];

			int imageType;
			if (channels == 3) {
				imageType = BufferedImage.TYPE_INT_RGB;
			} else if (channels == 4) {
				imageType = BufferedImage.TYPE_INT_ARGB;
			} else {
				logger.error("Unsupported channel count in header: {}", channels);
				throw new IOException("Unsupported channel count: " + channels);
			}

			image = new BufferedImage((int) width, (int) height, imageType);

			long totalPixels = width * height;
			while (pixelIndex < totalPixels) {
				int chunkByte = fileInputStream.read();
				if (chunkByte < 0) {
					throw new IOException("File ended prematurely");
				}
				if (chunkByte == 255) {
					// Chunk is QOI_OP_RGBA
					handleChunkRGBA(fileInputStream);
				} else if (chunkByte == 254) {
					// Chunk is QOI_OP_RGB
					handleChunkRGB(fileInputStream);
				} else if (chunkByte >= 192) {
					// Chunk is QOI_OP_RUN
					handleChunkRun(chunkByte);
				} else if (chunkByte >= 128) {
					// Chunk is QOI_OP_LUMA
					handleChunkLuma(fileInputStream, chunkByte);
				} else if (chunkByte >= 64) {
					// Chunk is QOI_OP_DIFF
					handleChunkDiff(chunkByte);
				} else {
					// Chunk is QOI_OP_INDEX
					handleChunkIndex(chunkByte);
				}
			}
			return image;
		}
	}

	private void handleChunkIndex(int chunkByte) {
		int index = chunkByte & 0x3F;
		Color color = pixelArray[index];
		logger.debug("QOI_OP_INDEX (index: {}, color: {})", index, QOIUtils.colorString(color));
		setNextPixel(color);
	}

	private void handleChunkDiff(int chunkByte) {
		int diffRed = ((chunkByte >> 4) & 0b11) - 2;
		int diffGreen = ((chunkByte >> 2) & 0b11) - 2;
		int diffBlue = (chunkByte & 0b11) - 2;

		int red = QOIUtils.wrapByteValue(previousPixel.getRed() + diffRed);
		int green = QOIUtils.wrapByteValue(previousPixel.getGreen() + diffGreen);
		int blue = QOIUtils.wrapByteValue(previousPixel.getBlue() + diffBlue);
		int alpha = previousPixel.getAlpha();
		Color color = new Color(red, green, blue, alpha);
		logger.debug("QOI_OP_DIFF (diff: [{}, {}, {}], previous: {}, color: {})", diffRed, diffGreen, diffBlue, QOIUtils.colorString(previousPixel), QOIUtils.colorString(color));
		setNextPixel(color);
	}

	private void handleChunkLuma(FileInputStream fileInputStream, int chunkByte) throws IOException {
		int diffByte = fileInputStream.read();
		if (diffByte < 0) {
			throw new IOException("File ended");
		}
		int diffGreen = chunkByte - 128 - 32;
		int diffRed = ((diffByte >> 4) & 0x0F) - 8 + diffGreen;
		int diffBlue = (diffByte & 0x0F) - 8 + diffGreen;

		int red = QOIUtils.wrapByteValue(previousPixel.getRed() + diffRed);
		int green = QOIUtils.wrapByteValue(previousPixel.getGreen() + diffGreen);
		int blue = QOIUtils.wrapByteValue(previousPixel.getBlue() + diffBlue);
		int alpha = previousPixel.getAlpha();
		Color color = new Color(red, green, blue, alpha);
		logger.debug("QOI_OP_LUMA (diff: [{}, {}, {}], previous: {}, color: {})", diffRed, diffGreen, diffBlue, QOIUtils.colorString(previousPixel), QOIUtils.colorString(color));
		setNextPixel(color);
	}

	private void handleChunkRun(int chunkByte) {
		int runLength = chunkByte - 192 + 1; // How many times to repeat previous pixel
		logger.debug("QOI_OP_RUN (length: {})", runLength);
		for (int i = 0; i < runLength; i++) {
			setNextPixel(previousPixel);
		}
	}

	private void handleChunkRGB(FileInputStream fileInputStream) throws IOException {
		byte[] chunkData = fileInputStream.readNBytes(3);
		if (chunkData.length != 3) {
			throw new IOException("File ended");
		}

		int red = QOIUtils.byteToUint8(chunkData[0]);
		int green = QOIUtils.byteToUint8(chunkData[1]);
		int blue = QOIUtils.byteToUint8(chunkData[2]);
		int alpha = previousPixel.getAlpha();
		Color color = new Color(red, green, blue, alpha);
		logger.debug("QOI_OP_RGB (color: {})", QOIUtils.colorString(color));
		setNextPixel(color);
	}

	private void handleChunkRGBA(FileInputStream fileInputStream) throws IOException {
		byte[] chunkData = fileInputStream.readNBytes(4);
		if (chunkData.length != 4) {
			throw new IOException("File ended");
		}

		int red = QOIUtils.byteToUint8(chunkData[0]);
		int green = QOIUtils.byteToUint8(chunkData[1]);
		int blue = QOIUtils.byteToUint8(chunkData[2]);
		int alpha = QOIUtils.byteToUint8(chunkData[3]);
		Color color = new Color(red, green, blue, alpha);
		logger.debug("QOI_OP_RGBA (color: {})", QOIUtils.colorString(color));
		setNextPixel(color);
	}

	private void setNextPixel(Color color) {
		int pixelX = (int) (pixelIndex % image.getWidth());
		int pixelY = (int) (pixelIndex / image.getWidth());
		if (pixelY >= image.getHeight()) {
			throw new IndexOutOfBoundsException("Pixel index is out of bounds for the given image");
		}
		image.setRGB(pixelX, pixelY, color.getRGB());

		pixelArray[QOIUtils.colorHash(color)] = color;
		previousPixel = color;
		pixelIndex++;
	}
}
