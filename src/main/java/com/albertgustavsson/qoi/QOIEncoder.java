package com.albertgustavsson.qoi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class QOIEncoder {
	private final static Logger logger = LogManager.getLogger(QOIEncoder.class);
	public static final byte[] END_MARKER = {0, 0, 0, 0, 0, 0, 0, 1};

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			logger.error("Required parameters: <input> <output>");
		}
		String inputFileName = args[0];
		String outputFileName = args[1];

		BufferedImage image = ImageIO.read(new File(inputFileName));
		//QOIUtils.showImage(image);

		QOIEncoder.encode(image, outputFileName);

		logger.debug("Image has been encoded.");
	}

	public static void encode(BufferedImage image, String fileName) throws IOException {
		Color previousPixelColor = new Color(0,0,0,255);
		final Color[] colorArray = new Color[64];
		Arrays.setAll(colorArray, value -> new Color(0,0,0,0));
		int runLengthCounter = 0;
		try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
			// Write header to file
			int numComponents = image.getColorModel().getNumComponents();
			if (numComponents != 3 && numComponents != 4) {
				throw new IOException("Unsupported number of components in image: " + numComponents);
			}
			byte channels = (byte) numComponents;
			byte colorSpace = image.getColorModel().getColorSpace().isCS_sRGB()?(byte)0:1;

			fileOutputStream.write(getHeaderBytes(image.getWidth(), image.getHeight(), channels, colorSpace));

			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					Color color = new Color(image.getRGB(x,y), channels==4);
					int colorIndex = QOIUtils.colorHash(color);

					if (color.equals(previousPixelColor)) {
						// Continue run
						runLengthCounter++;
						if (runLengthCounter == 62 || (x == image.getWidth()-1 && y == image.getHeight()-1)) {
							fileOutputStream.write(QOIUtils.createRunChunk(runLengthCounter));
							runLengthCounter = 0;
						}
					} else {
						// This pixel's color is different from the previous
						if (runLengthCounter > 0) {
							// Run ends.
							fileOutputStream.write(QOIUtils.createRunChunk(runLengthCounter));
							runLengthCounter = 0;
						}

						if (colorArray[colorIndex].equals(color)) {
							fileOutputStream.write(QOIUtils.createIndexChunk(colorIndex));
						} else {
							colorArray[colorIndex] = color;
							if (color.getAlpha() == previousPixelColor.getAlpha()) {
								byte diffRed = (byte) (color.getRed() - previousPixelColor.getRed());
								byte diffGreen = (byte) (color.getGreen() - previousPixelColor.getGreen());
								byte diffBlue = (byte) (color.getBlue() - previousPixelColor.getBlue());
								byte ddiffRed = (byte) (diffRed - diffGreen);
								byte ddiffBlue = (byte) (diffBlue - diffGreen);

								if (diffRed >= -2 && diffRed <= 1 &&
										diffGreen >= -2 && diffGreen <= 1 &&
										diffBlue >= -2 && diffBlue <= 1) {
									fileOutputStream.write(QOIUtils.createDiffChunk(diffRed, diffGreen, diffBlue));
								} else if (ddiffRed >= -8 && ddiffRed <= 7 &&
										diffGreen >= -32 && diffGreen <= 31 &&
										ddiffBlue >= -8 && ddiffBlue <= 7) {
									fileOutputStream.write(QOIUtils.createLumaChunk(ddiffRed, diffGreen, ddiffBlue));
								} else {
									fileOutputStream.write(QOIUtils.createRGBChunk(color));
								}
							} else {
								fileOutputStream.write(QOIUtils.createRGBAChunk(color));
							}
						}
					}
					previousPixelColor = color;
				}
			}
			fileOutputStream.write(END_MARKER);
		}
	}

	private static byte[] getHeaderBytes(int width, int height, byte channels, byte colorSpace) {
		ByteBuffer buffer = ByteBuffer.allocate(14);
		buffer.put("qoif".getBytes());
		buffer.put(QOIUtils.unsignedIntToByteArray(width, ByteOrder.BIG_ENDIAN));
		buffer.put(QOIUtils.unsignedIntToByteArray(height, ByteOrder.BIG_ENDIAN));
		buffer.put(channels);
		buffer.put(colorSpace);
		return buffer.array();
	}
}
