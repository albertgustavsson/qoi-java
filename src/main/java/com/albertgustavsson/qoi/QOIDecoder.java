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

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			logger.error("Required parameters: <filename>");
		}
		String filename = args[0];

		BufferedImage image = QOIDecoder.decode(filename);

		logger.debug("Image has been decoded.");

		float scale;
		if (image.getWidth() > image.getHeight()) {
			scale = (float) 1600 / image.getWidth();
		} else {
			scale = (float) 900 / image.getHeight();
		}
		QOIUtils.showImage(image, scale);
	}

	public static BufferedImage decode(String inputFileName) throws IOException {
		BufferedImage image;
		long pixelIndex = 0;
		Color color = new Color(0,0,0,255);
		final Color[] pixelArray = new Color[64];
		Arrays.setAll(pixelArray, value -> new Color(0,0,0,0));
		try (FileInputStream fileInputStream = new FileInputStream(inputFileName)) {
			byte[] magicBytes = fileInputStream.readNBytes(4);
			assert new String(magicBytes).equals("qoif");

			long width = QOIUtils.byteArrayToUnsignedInt(fileInputStream.readNBytes(4), ByteOrder.BIG_ENDIAN);
			long height = QOIUtils.byteArrayToUnsignedInt(fileInputStream.readNBytes(4), ByteOrder.BIG_ENDIAN);
			if (width > Integer.MAX_VALUE || height > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("Image dimensions exceed implementation limits. The maximum supported resolution is " + Integer.MAX_VALUE + " x " + Integer.MAX_VALUE);
			}

			byte channels = fileInputStream.readNBytes(1)[0];
			byte colorSpace = fileInputStream.readNBytes(1)[0];

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

			int runLengthCounter = 0;
			while (pixelIndex < totalPixels) {
				if (runLengthCounter > 0) {
					runLengthCounter--;
				} else {
					int chunkByte = fileInputStream.read();
					if (chunkByte < 0) {
						throw new IOException("File ended prematurely");
					}
					if (chunkByte == 255) {
						// Chunk is QOI_OP_RGBA
						byte[] chunkData = fileInputStream.readNBytes(4);
						color = QOIUtils.bytesToColorRGBA(chunkData);
						logger.debug("QOI_OP_RGBA (color: {})", QOIUtils.colorString(color));
					} else if (chunkByte == 254) {
						// Chunk is QOI_OP_RGB
						byte[] chunkData = fileInputStream.readNBytes(3);
						color = QOIUtils.bytesToColorRGB(chunkData, color.getAlpha());
						logger.debug("QOI_OP_RGB (color: {})", QOIUtils.colorString(color));
					} else if (chunkByte >= 192) {
						// Chunk is QOI_OP_RUN
						int runLength = chunkByte - 192 + 1; // How many times to repeat previous pixel
						logger.debug("QOI_OP_RUN (length: {})", runLength);
						runLengthCounter = runLength-1;
					} else if (chunkByte >= 128) {
						// Chunk is QOI_OP_LUMA
						int diffByte = fileInputStream.read();
						int diffGreen = chunkByte - 128 - 32;
						int diffRed = ((diffByte >> 4) & 0x0F) - 8 + diffGreen;
						int diffBlue = (diffByte & 0x0F) - 8 + diffGreen;
						Color previousColor = color;
						color = QOIUtils.colorApplyDiff(color, diffRed, diffGreen, diffBlue);
						logger.debug("QOI_OP_LUMA (diff: [{}, {}, {}], previous: {}, color: {})", diffRed, diffGreen, diffBlue, QOIUtils.colorString(previousColor), QOIUtils.colorString(color));
					} else if (chunkByte >= 64) {
						// Chunk is QOI_OP_DIFF
						int diffRed = ((chunkByte >> 4) & 0b11) - 2;
						int diffGreen = ((chunkByte >> 2) & 0b11) - 2;
						int diffBlue = (chunkByte & 0b11) - 2;
						Color previousColor = color;
						color = QOIUtils.colorApplyDiff(color, diffRed, diffGreen, diffBlue);
						logger.debug("QOI_OP_DIFF (diff: [{}, {}, {}], previous: {}, color: {})", diffRed, diffGreen, diffBlue, QOIUtils.colorString(previousColor), QOIUtils.colorString(color));
					} else {
						// Chunk is QOI_OP_INDEX
						int index = chunkByte & 0x3F;
						color = pixelArray[index];
						logger.debug("QOI_OP_INDEX (index: {}, color: {})", index, QOIUtils.colorString(color));
					}
				}

				int pixelX = (int) (pixelIndex % image.getWidth());
				int pixelY = (int) (pixelIndex / image.getWidth());
				if (pixelY >= image.getHeight()) {
					throw new IndexOutOfBoundsException("Pixel index is out of bounds for the given image");
				}
				image.setRGB(pixelX, pixelY, color.getRGB());
				pixelArray[QOIUtils.colorHash(color)] = color;
				pixelIndex++;
			}
			return image;
		}
	}
}
