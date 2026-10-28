package com.nyangmeong.seed.core;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.seed.SeedException;

/**
 * [시드 이미지]
 * - ATCH_FILE 행과 파일을 함께 만든다. 내용은 (종류, 번호)로 정해져 다른 PC에서도 같은 파일이 나온다
 * - 사진은 src/seed/resources/images의 실제 사진(목록·출처는 그 폴더의 SOURCES.md), 픽셀 캐릭터·아이템·문서는 그린다
 * - 경로 seed/{종류}/{ATCH_FILE_SN}.{확장자}, 원본 파일명 {종류}_{ATCH_FILE_SN}.{확장자} (seedFiles가 이 이름으로 다시 만든다)
 */
public final class SeedImages {

    /** 이미지 종류. 이름이 파일 경로에 들어간다 */
    public enum Kind {
        /** 강아지·고양이 사진(캐릭터 원본처럼 종이 정해진 자리) */
        DOG, CAT,
        /** 반려동물 사진 아무거나 */
        PROFILE, POST, CHAT, NEWS, OBSERVE, INQUIRY, CARE,
        /** 그리는 것 */
        CHARACTER, ITEM, RECEIPT, DOCUMENT, SCAN
    }

    private record Asset(byte[] bytes, String extension) {
    }

    private static final String SEED_DIR = "seed";
    private static final String SOURCES = "/images/SOURCES.md";
    private static final Pattern SOURCE_ROW = Pattern.compile("^\\| `((dog|cat)/[^`]+)` \\|");

    private static List<String> dogPhotos;
    private static List<String> catPhotos;

    private final RowStore store;
    private final Seed seed;

    SeedImages(RowStore store, Seed seed) {
        this.store = store;
        this.seed = seed;
    }

    /**
     * 이미지 한 장 등록
     * @return ATCH_FILE_SN
     */
    public long add(Kind kind) {
        long sn = store.rows("ATCH_FILE").size() + 1L;
        // 파일은 커밋 뒤 writeAll이 같은 내용으로 다시 만들어 쓴다. 여기서는 크기만 쓴다
        Asset asset = render(kind, sn);
        String extension = seed.codes().of("FILG01", asset.extension());
        Row row = store.add("ATCH_FILE", r -> r
                .set("FILE_EXTN_CD", extension)
                .set("ORGNL_FILE_NM", fileName(kind, sn, asset.extension()))
                .set("PHYS_STRG_PATH", seedPath(kind) + "/" + sn + "." + asset.extension())
                .set("FILE_SZ", asset.bytes().length));
        if (row.sn() != sn) {
            throw new IllegalStateException("ATCH_FILE 번호가 어긋났습니다");
        }
        return sn;
    }

    /**
     * 반려동물 종에 맞는 사진
     */
    public long addPetPhoto(boolean dog) {
        return add(dog ? Kind.DOG : Kind.CAT);
    }

    /**
     * 커밋 뒤 파일 쓰기
     */
    public static int writeAll(RowStore store, Path rootDir) {
        int count = 0;
        for (Row row : store.rows("ATCH_FILE")) {
            write(rootDir, row.string("PHYS_STRG_PATH"), render(kindOf(row.string("ORGNL_FILE_NM")), row.sn()).bytes());
            count++;
        }
        return count;
    }

    /**
     * DB의 시드 ATCH_FILE을 보고 파일만 다시 만들기(seedFiles)
     */
    public static int rewriteFromDatabase(Connection conn, Path rootDir) throws SQLException {
        String sql = "SELECT ATCH_FILE_SN, ORGNL_FILE_NM, PHYS_STRG_PATH FROM ATCH_FILE WHERE RGTR_ID = 'SEED'";
        int count = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                write(rootDir, rs.getString(3), render(kindOf(rs.getString(2)), rs.getLong(1)).bytes());
                count++;
            }
        }
        if (count == 0) {
            throw new SeedException("DB에 시드 이미지(ATCH_FILE.RGTR_ID = 'SEED')가 없습니다. seedData를 먼저 실행해 주세요");
        }
        return count;
    }

    /**
     * 이 PC의 시드 이미지 폴더 삭제(seedClean, -Preset)
     * @return 지운 파일 수
     */
    public static int deleteFiles(Path rootDir) throws IOException {
        return deleteDir(rootDir.resolve(SEED_DIR)) + deleteDir(rootDir.resolve(FileStore.PUBLIC_PREFIX + SEED_DIR));
    }

    private static int deleteDir(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        int count = 0;
        try (var paths = Files.walk(dir)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (Files.isRegularFile(p)) {
                    count++;
                }
                Files.delete(p);
            }
        }
        return count;
    }

    /** 서버가 공개로 저장하는 종류(게시글 사진·캐릭터 이미지)는 public/ 아래에 둔다 */
    private static String seedPath(Kind kind) {
        boolean publicKind = kind == Kind.POST || kind == Kind.CHARACTER;
        return (publicKind ? FileStore.PUBLIC_PREFIX : "") + SEED_DIR + "/" + kind.name().toLowerCase();
    }

    private static String fileName(Kind kind, long sn, String extension) {
        return kind.name().toLowerCase() + "_" + sn + "." + extension;
    }

    private static Kind kindOf(String fileName) {
        return Kind.valueOf(fileName.substring(0, fileName.indexOf('_')).toUpperCase());
    }

    private static void write(Path rootDir, String physStrgPath, byte[] bytes) {
        Path path = rootDir.resolve(physStrgPath);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(path + " 쓰기 실패", e);
        }
    }

    // ---- 만들기. 같은 (종류, 번호)면 같은 바이트가 나온다

    private static Asset render(Kind kind, long sn) {
        Random r = new Random(kind.ordinal() * 1_000_003L + sn);
        return switch (kind) {
        case DOG -> photo(pick(dogPhotos(), r));
        case CAT -> photo(pick(catPhotos(), r));
        case CHARACTER -> png(pixelPet(r));
        case ITEM -> png(pixelItem(r));
        case RECEIPT -> png(receipt(r));
        case DOCUMENT -> png(document(r));
        case SCAN -> png(scan(r));
        default -> photo(pick(r.nextBoolean() ? dogPhotos() : catPhotos(), r));
        };
    }

    private static String pick(List<String> photos, Random r) {
        return photos.get(r.nextInt(photos.size()));
    }

    private static Asset photo(String resource) {
        try (InputStream in = SeedImages.class.getResourceAsStream("/images/" + resource)) {
            if (in == null) {
                throw new SeedException("시드 사진 " + resource + "이 없습니다. src/seed/resources/images와 SOURCES.md를 맞춰 주세요");
            }
            return new Asset(in.readAllBytes(), "jpg");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Asset png(BufferedImage img) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return new Asset(out.toByteArray(), "png");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static synchronized List<String> dogPhotos() {
        if (dogPhotos == null) {
            loadPhotos();
        }
        return dogPhotos;
    }

    private static synchronized List<String> catPhotos() {
        if (catPhotos == null) {
            loadPhotos();
        }
        return catPhotos;
    }

    /**
     * 사진 목록은 SOURCES.md의 표에서 읽는다. 출처를 적지 않은 사진은 쓰지 않는다
     */
    private static void loadPhotos() {
        List<String> dogs = new ArrayList<>();
        List<String> cats = new ArrayList<>();
        try (InputStream in = SeedImages.class.getResourceAsStream(SOURCES)) {
            if (in == null) {
                throw new SeedException("src/seed/resources" + SOURCES + "가 없습니다");
            }
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                Matcher m = SOURCE_ROW.matcher(line);
                if (m.find()) {
                    ("dog".equals(m.group(2)) ? dogs : cats).add(m.group(1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (dogs.isEmpty() || cats.isEmpty()) {
            throw new SeedException("SOURCES.md에 강아지·고양이 사진이 모두 있어야 합니다");
        }
        dogPhotos = List.copyOf(dogs);
        catPhotos = List.copyOf(cats);
    }

    private static final Color[] FUR = { new Color(0x3b2f2f), new Color(0xf5f5f0), new Color(0x9c6b3c),
            new Color(0xe8d3a9), new Color(0x8a8a8a), new Color(0xd98c3f) };

    /** 픽셀 반려동물 얼굴(16x16 칸을 16배로) */
    private static BufferedImage pixelPet(Random r) {
        int cell = 16;
        BufferedImage img = new BufferedImage(16 * cell, 16 * cell, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color fur = FUR[r.nextInt(FUR.length)];
        Color dark = fur.darker().darker();
        boolean cat = r.nextBoolean();
        int[][] ears = cat ? new int[][] { { 3, 2 }, { 4, 3 }, { 12, 2 }, { 11, 3 } }
                : new int[][] { { 2, 4 }, { 2, 5 }, { 2, 6 }, { 13, 4 }, { 13, 5 }, { 13, 6 } };
        g.setColor(cat ? fur : dark);
        for (int[] e : ears) {
            g.fillRect(e[0] * cell, e[1] * cell, cell, cell);
        }
        g.setColor(fur);
        g.fillRect(3 * cell, 4 * cell, 10 * cell, 9 * cell);
        g.fillRect(4 * cell, 3 * cell, 8 * cell, cell);
        g.setColor(Color.BLACK);
        g.fillRect(5 * cell, 7 * cell, cell, 2 * cell);
        g.fillRect(10 * cell, 7 * cell, cell, 2 * cell);
        g.setColor(new Color(0xe07a8a));
        g.fillRect(7 * cell, 10 * cell, 2 * cell, cell);
        if (r.nextBoolean()) {
            g.setColor(dark);
            g.fillRect(3 * cell, 4 * cell, 3 * cell, 3 * cell);
        }
        g.dispose();
        return img;
    }

    /** 픽셀 아이템(모자·안경 모양 단순화) */
    private static BufferedImage pixelItem(Random r) {
        int cell = 8;
        BufferedImage img = new BufferedImage(16 * cell, 16 * cell, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.getHSBColor(r.nextFloat(), 0.6f, 0.9f));
        int shape = r.nextInt(3);
        if (shape == 0) {
            g.fillRect(4 * cell, 5 * cell, 8 * cell, 5 * cell);
            g.fillRect(2 * cell, 10 * cell, 12 * cell, 2 * cell);
        } else if (shape == 1) {
            g.fillRect(2 * cell, 7 * cell, 5 * cell, 3 * cell);
            g.fillRect(9 * cell, 7 * cell, 5 * cell, 3 * cell);
            g.fillRect(7 * cell, 8 * cell, 2 * cell, cell);
        } else {
            g.fillRect(3 * cell, 6 * cell, 10 * cell, 2 * cell);
            g.fillRect(7 * cell, 8 * cell, 2 * cell, 4 * cell);
        }
        g.dispose();
        return img;
    }

    private static final String[] HOSPITALS = { "튼튼동물병원", "행복동물메디컬센터", "우리동물병원", "24시하나동물병원", "사랑동물병원" };
    private static final String[][] RECEIPT_ITEMS = { { "진찰료", "15000" }, { "혈액검사", "45000" }, { "피부검사", "30000" },
            { "주사", "12000" }, { "처치료", "10000" }, { "내복약(7일)", "21000" }, { "예방접종", "35000" }, { "귀세정", "8000" } };
    private static final String[][] BLOOD = { { "WBC", "6.0-17.0", "K/μL" }, { "RBC", "5.5-8.5", "M/μL" },
            { "HGB", "12.0-18.0", "g/dL" }, { "BUN", "7-27", "mg/dL" }, { "CREA", "0.5-1.8", "mg/dL" },
            { "ALT", "10-125", "U/L" }, { "GLU", "70-143", "mg/dL" } };

    /** 동물병원 영수증 */
    private static BufferedImage receipt(Random r) {
        int width = 300;
        int height = 460;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = paper(img);
        String hospital = HOSPITALS[r.nextInt(HOSPITALS.length)];
        g.setFont(font(Font.BOLD, 20));
        center(g, hospital, width, 40);
        g.setFont(font(Font.PLAIN, 12));
        center(g, "사업자번호 " + (100 + r.nextInt(800)) + "-" + (10 + r.nextInt(89)) + "-" + (10000 + r.nextInt(89999)), width, 62);
        center(g, String.format("2026-%02d-%02d %02d:%02d", 1 + r.nextInt(10), 1 + r.nextInt(28), 9 + r.nextInt(9), r.nextInt(60)),
                width, 80);
        dashed(g, 20, width - 20, 96);
        g.setFont(font(Font.PLAIN, 14));
        int y = 124;
        int total = 0;
        int n = 3 + r.nextInt(3);
        boolean[] used = new boolean[RECEIPT_ITEMS.length];
        for (int i = 0; i < n; i++) {
            int k = r.nextInt(RECEIPT_ITEMS.length);
            if (used[k]) {
                continue;
            }
            used[k] = true;
            int amount = Integer.parseInt(RECEIPT_ITEMS[k][1]);
            total += amount;
            g.drawString(RECEIPT_ITEMS[k][0], 24, y);
            right(g, String.format("%,d", amount), width - 24, y);
            y += 28;
        }
        dashed(g, 20, width - 20, y - 8);
        g.setFont(font(Font.BOLD, 16));
        g.drawString("합계", 24, y + 20);
        right(g, String.format("%,d원", total), width - 24, y + 20);
        g.setFont(font(Font.PLAIN, 12));
        center(g, "카드 승인 " + (10000000 + r.nextInt(89999999)), width, height - 40);
        center(g, "감사합니다", width, height - 20);
        g.dispose();
        return img;
    }

    /** 혈액검사 결과지 */
    private static BufferedImage document(Random r) {
        int width = 420;
        int height = 540;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = paper(img);
        g.setFont(font(Font.BOLD, 22));
        center(g, "혈액검사 결과지", width, 44);
        g.setFont(font(Font.PLAIN, 13));
        g.drawString(HOSPITALS[r.nextInt(HOSPITALS.length)], 28, 76);
        right(g, String.format("검사일 2026-%02d-%02d", 1 + r.nextInt(10), 1 + r.nextInt(28)), width - 28, 76);
        g.drawLine(24, 90, width - 24, 90);
        g.setFont(font(Font.BOLD, 13));
        g.drawString("항목", 32, 112);
        g.drawString("결과", 150, 112);
        g.drawString("참고범위", 240, 112);
        g.drawString("단위", 340, 112);
        g.drawLine(24, 122, width - 24, 122);
        g.setFont(font(Font.PLAIN, 13));
        int y = 148;
        for (String[] row : BLOOD) {
            String[] range = row[1].split("-");
            double low = Double.parseDouble(range[0]);
            double high = Double.parseDouble(range[1]);
            double value = low + (high - low) * (r.nextDouble() * 1.2 - 0.1);
            boolean out = value < low || value > high;
            g.setColor(out ? new Color(0xc62828) : Color.BLACK);
            g.drawString(row[0], 32, y);
            g.drawString(String.format(high >= 100 ? "%.0f" : "%.1f", value) + (out ? (value > high ? " H" : " L") : ""), 150, y);
            g.setColor(Color.BLACK);
            g.drawString(row[1], 240, y);
            g.drawString(row[2], 340, y);
            y += 32;
        }
        g.drawLine(24, y - 12, width - 24, y - 12);
        g.drawString("담당 수의사 " + new String[] { "김", "이", "박", "최" }[r.nextInt(4)] + "OO", 28, height - 36);
        g.dispose();
        return img;
    }

    /** 영상 사진 자리(어두운 배경에 밝은 형태) */
    private static BufferedImage scan(Random r) {
        BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0x101418));
        g.fillRect(0, 0, 256, 256);
        g.setColor(new Color(0xc8d0d8));
        for (int i = 0; i < 3; i++) {
            g.fillOval(40 + r.nextInt(120), 40 + r.nextInt(120), 30 + r.nextInt(70), 20 + r.nextInt(60));
        }
        g.dispose();
        return img;
    }

    private static Graphics2D paper(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0xfcfcf8));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setColor(Color.BLACK);
        return g;
    }

    /** 한글이 나오는 글꼴(Windows 맑은 고딕, 없으면 Java 기본 글꼴) */
    private static Font font(int style, int size) {
        Font font = new Font("Malgun Gothic", style, size);
        return font.canDisplay('가') ? font : new Font(Font.SANS_SERIF, style, size);
    }

    private static void center(Graphics2D g, String text, int width, int y) {
        g.drawString(text, (width - g.getFontMetrics().stringWidth(text)) / 2, y);
    }

    private static void right(Graphics2D g, String text, int x, int y) {
        g.drawString(text, x - g.getFontMetrics().stringWidth(text), y);
    }

    private static void dashed(Graphics2D g, int x1, int x2, int y) {
        for (int x = x1; x < x2; x += 8) {
            g.drawLine(x, y, Math.min(x + 4, x2), y);
        }
    }
}
