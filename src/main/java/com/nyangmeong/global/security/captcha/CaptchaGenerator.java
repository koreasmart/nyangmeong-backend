package com.nyangmeong.global.security.captcha;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [보안코드(캡챠) 생성기]
 * - 영문 대문자·숫자가 각각 1개 이상인 6글자, PNG data URI 로 그린다
 * - 헷갈리는 글자(O/0, I/1)도 명세가 허용하는 A~Z, 0~9 를 그대로 쓴다
 */
@Component
public class CaptchaGenerator {

    /** 만들어 낸 코드와 그 이미지 */
    public record Captcha(String code, String imageDataUri) {
    }

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final int CODE_LENGTH = 6;
    private static final int WIDTH = 168;
    private static final int HEIGHT = 56;
    private static final int NOISE_LINES = 5;

    private final SecureRandom random = new SecureRandom();

    /**
     * 새 보안코드 생성
     */
    public Captcha generate() {
        String code = newCode();
        return new Captcha(code, "data:image/png;base64," + Base64.getEncoder().encodeToString(draw(code)));
    }

    /** 영문·숫자를 각각 1개 이상 넣고 섞는다 */
    String newCode() {
        char[] chars = new char[CODE_LENGTH];
        chars[0] = LETTERS.charAt(random.nextInt(LETTERS.length()));
        chars[1] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        String all = LETTERS + DIGITS;
        for (int i = 2; i < CODE_LENGTH; i++) {
            chars[i] = all.charAt(random.nextInt(all.length()));
        }
        for (int i = CODE_LENGTH - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char swap = chars[i];
            chars[i] = chars[j];
            chars[j] = swap;
        }
        return new String(chars);
    }

    private byte[] draw(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setStroke(new BasicStroke(1.4f));
            for (int i = 0; i < NOISE_LINES; i++) {
                g.setColor(randomColor(150, 210));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH),
                        random.nextInt(HEIGHT));
            }

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
            int cell = WIDTH / CODE_LENGTH;
            for (int i = 0; i < CODE_LENGTH; i++) {
                AffineTransform saved = g.getTransform();
                double angle = (random.nextDouble() - 0.5) * 0.6;
                int x = i * cell + 6;
                int y = HEIGHT / 2 + 12 + random.nextInt(5) - 2;
                g.rotate(angle, x + cell / 2.0, HEIGHT / 2.0);
                g.setColor(randomColor(30, 120));
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.setTransform(saved);
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Color randomColor(int min, int max) {
        return new Color(min + random.nextInt(max - min), min + random.nextInt(max - min),
                min + random.nextInt(max - min));
    }
}
