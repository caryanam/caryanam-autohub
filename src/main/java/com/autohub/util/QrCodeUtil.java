package com.autohub.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for generating QR code PNG images from any text content.
 * Uses Google ZXing library with UTF-8 encoding and medium error correction.
 */
public class QrCodeUtil {

    private QrCodeUtil() {
        // Utility class — prevent instantiation
    }

    /**
     * Generate a QR code as a PNG byte array.
     *
     * @param content the text content to encode (URL, structured text, etc.)
     * @param width   QR image width in pixels
     * @param height  QR image height in pixels
     * @return PNG image bytes
     * @throws WriterException if the content cannot be encoded
     * @throws IOException     if the PNG stream fails
     */

    public static byte[] generateQrCodePng(String content, int width, int height)
            throws WriterException, IOException {

        QRCodeWriter writer = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);

        return outputStream.toByteArray();
    }
}
