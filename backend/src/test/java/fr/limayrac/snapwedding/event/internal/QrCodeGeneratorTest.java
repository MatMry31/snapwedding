package fr.limayrac.snapwedding.event.internal;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeGeneratorTest {

    private final QrCodeGenerator generator = new QrCodeGenerator("http://localhost:5173/");

    @Test
    void theQrCodeIsAPngThatPointsToTheGuestPageWithThePublicCode() throws Exception {
        byte[] png = generator.generatePng("Xk9fQz3vT0aBcDeFgHiJkL");

        // On RELIT le QR code comme le ferait un téléphone, et on vérifie l'URL qu'il contient
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        String content = new MultiFormatReader()
                .decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image))))
                .getText();

        assertThat(content).isEqualTo("http://localhost:5173/e/Xk9fQz3vT0aBcDeFgHiJkL");
    }
}
