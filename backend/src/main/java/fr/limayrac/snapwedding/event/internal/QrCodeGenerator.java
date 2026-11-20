package fr.limayrac.snapwedding.event.internal;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * O4 : fabrique le QR code d'un événement, en PNG, avec la bibliothèque ZXing.
 * <p>
 * Le QR code contient l'URL de la page invité du front : {public-base-url}/e/{publicCode}.
 * Jamais l'id de l'événement : /events/42 se devine, /e/Xk9f...Qz non.
 */
@Component
public class QrCodeGenerator {

    private static final int SIZE_PX = 512;

    private final String publicBaseUrl;

    public QrCodeGenerator(@Value("${snapwedding.public-base-url}") String publicBaseUrl) {
        // "http://localhost:5173/" et "http://localhost:5173" donnent la même URL
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    /** L'URL sur laquelle arrive un invité qui scanne le QR code. */
    public String guestUrl(String publicCode) {
        return publicBaseUrl + "/e/" + publicCode;
    }

    public byte[] generatePng(String publicCode) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    guestUrl(publicCode), BarcodeFormat.QR_CODE, SIZE_PX, SIZE_PX,
                    Map.of(
                            EncodeHintType.MARGIN, 2,   // marge blanche autour, nécessaire à la lecture
                            // niveau M : le QR reste lisible même abîmé à ~15 % (table de mariage...)
                            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M
                    ));
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", png);
            return png.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("QR code generation failed", e);
        }
    }
}
