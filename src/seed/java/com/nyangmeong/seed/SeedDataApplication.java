package com.nyangmeong.seed;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nyangmeong.seed.core.AutoFill;
import com.nyangmeong.seed.core.RowStore;
import com.nyangmeong.seed.core.Schema;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.SeedImages;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.SeedSettings;
import com.nyangmeong.seed.core.SeedValidator;
import com.nyangmeong.seed.core.SeedWarnings;
import com.nyangmeong.seed.core.World;
import com.nyangmeong.seed.domain.AiSeeder;
import com.nyangmeong.seed.domain.CharacterSeeder;
import com.nyangmeong.seed.domain.CommunitySeeder;
import com.nyangmeong.seed.domain.ExpenseSeeder;
import com.nyangmeong.seed.domain.HealthSeeder;
import com.nyangmeong.seed.domain.MasterSeeder;
import com.nyangmeong.seed.domain.MemberSeeder;
import com.nyangmeong.seed.domain.NotificationSeeder;
import com.nyangmeong.seed.domain.PetSeeder;
import com.nyangmeong.seed.domain.PlaceSeeder;
import com.nyangmeong.seed.domain.SupportSeeder;

/**
 * [시드 데이터 생성기]
 * - data : 빈 DB에 시드 데이터를 넣는다. 한 트랜잭션, 커밋 전 검사 실패 시 롤백
 * - files : 이미 들어간 시드 데이터의 이미지 파일만 이 PC에 다시 만든다
 * - clean : 시드 대상 테이블과 기록 테이블을 전부 비운다(팀원이 만든 행도 지워진다). 한 트랜잭션
 * - 실행은 build.gradle의 seedData·seedFiles·seedClean 태스크(README "시드 데이터")
 */
public final class SeedDataApplication {

    /** 난수 시드. 바꾸면 모든 팀원의 데이터가 달라진다 */
    private static final long RANDOM_SEED = 20261023L;

    private SeedDataApplication() {
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        try {
            SeedOptions options = SeedOptions.parse(args);
            switch (options.command()) {
            case FILES -> writeFiles(options);
            case CLEAN -> clean(options);
            default -> seed(options);
            }
        } catch (SeedException e) {
            System.err.println();
            System.err.println("[시드] 중단: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println();
            System.err.println("[시드] 예상하지 못한 오류로 중단했습니다. DB는 바뀌지 않았습니다.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void seed(SeedOptions options) throws Exception {
        SeedSettings settings = SeedSettings.load(options.target());
        System.out.println("[시드] 대상: " + options.target().label() + " " + settings.describeDatabase());
        System.out.println("[시드] 배율: " + options.scale());
        // 설정 확인은 -Preset으로 비우기 전에 끝낸다
        settings.requiredSeedPassword();
        checkTarget(options, settings);

        try (Connection conn = DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password())) {
            Schema schema = Schema.load(conn);
            if (options.reset()) {
                RowStore.truncate(conn, schema, SeedTables.targets(schema), SeedTables.RUNTIME);
                SeedImages.deleteFiles(settings.fileRootDir());
                System.out.println("[시드] -Preset: 시드 대상 테이블과 seed 이미지 폴더를 비웠습니다");
            }
            requireEmpty(conn, schema);

            long started = System.currentTimeMillis();
            Seed seed = Seed.create(conn, RANDOM_SEED, options.scale(), settings);
            World world = new World();
            List<Seeder> seeders = List.of(new MasterSeeder(), new MemberSeeder(), new PetSeeder(), new PlaceSeeder(),
                    new ExpenseSeeder(), new HealthSeeder(), new CharacterSeeder(), new CommunitySeeder(),
                    new SupportSeeder(), new NotificationSeeder(), new AiSeeder());
            for (Seeder seeder : seeders) {
                seeder.run(seed, world);
            }
            // 테이블 구조가 Seeder와 달라도 돌게 맞추고, 모르는 새 테이블은 자동으로 채운다
            AutoFill auto = new AutoFill(seed, schema);
            auto.reconcile();
            List<String> autoTables = auto.generateUnknownTables(SeedTables.known());

            seed.store().checkColumns(schema);
            conn.setAutoCommit(false);
            try {
                seed.store().flush(conn, schema, autoTables);
                new SeedValidator(conn, schema).validate(new ArrayList<>(seed.store().tableNames()));
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
            int files = SeedImages.writeAll(seed.store(), settings.fileRootDir());
            System.out.println();
            seed.store().counts().forEach((table, count) -> System.out.printf("  %-26s %,8d%n", table, count));
            System.out.printf("[시드] 완료: %,d행, 이미지 %,d개, %.1f초%n", seed.store().totalRows(), files,
                    (System.currentTimeMillis() - started) / 1000.0);
            SeedWarnings.print();
            System.out.println("[시드] 고정 계정과 확인 방법은 README \"시드 데이터\"에 있습니다");
        }
    }

    /**
     * 시드 대상·기록 테이블 비우기
     * - 공용·로컬 모두 DB 이름을 입력해야 지운다. COM_CD·NOTI_TMPLT는 남긴다
     * - DELETE를 한 트랜잭션으로 해서 중간에 실패하면 아무것도 지워지지 않는다. 이 PC의 seed 이미지 폴더도 지운다
     */
    private static void clean(SeedOptions options) throws Exception {
        SeedSettings settings = SeedSettings.load(options.target());
        System.out.println("[시드] 비우기 대상: " + options.target().label() + " " + settings.describeDatabase());
        if (options.target() == SeedOptions.Target.LOCAL && !settings.isLocalHost()) {
            throw new SeedException("-Ptarget=local인데 접속 주소가 " + settings.host() + "입니다");
        }
        System.out.println("[시드] " + String.join("·", SeedTables.KEPT)
                + "를 뺀 모든 테이블의 행을 지웁니다. 팀원이 만든 테스트 데이터도 함께 지워집니다");
        confirmDatabaseName(options, settings, "지우려면");

        try (Connection conn = DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password())) {
            Schema schema = Schema.load(conn);
            conn.setAutoCommit(false);
            Map<String, Long> deleted;
            try {
                deleted = RowStore.deleteAll(conn, schema, SeedTables.targets(schema), SeedTables.RUNTIME);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
            int files = SeedImages.deleteFiles(settings.fileRootDir());
            long total = deleted.values().stream().mapToLong(Long::longValue).sum();
            System.out.printf("[시드] 비우기 완료: %d개 테이블 %,d행, 이 PC의 이미지 %,d개%n", deleted.size(), total, files);
            System.out.println("[시드] 다른 PC의 파일 저장 루트 아래 seed·public/seed 폴더는 그대로 남습니다. 다시 넣으면 seedFiles가 덮어씁니다");
        }
    }

    private static void writeFiles(SeedOptions options) throws Exception {
        SeedSettings settings = SeedSettings.load(options.target());
        try (Connection conn = DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password())) {
            int files = SeedImages.rewriteFromDatabase(conn, settings.fileRootDir());
            System.out.printf("[시드] 이미지 %,d개를 %s에 만들었습니다%n", files, settings.fileRootDir().toAbsolutePath());
        }
    }

    /**
     * 대상 DB 확인
     * - 공용 : DB 이름 입력 확인, reset 금지
     * - 로컬 : localhost·127.0.0.1만
     */
    private static void checkTarget(SeedOptions options, SeedSettings settings) throws Exception {
        if (options.target() == SeedOptions.Target.LOCAL) {
            if (!settings.isLocalHost()) {
                throw new SeedException("-Ptarget=local인데 접속 주소가 " + settings.host()
                        + "입니다. application-local.properties의 spring.datasource.url을 확인해 주세요");
            }
            return;
        }
        if (options.reset()) {
            throw new SeedException("공용 DB에는 -Preset을 쓸 수 없습니다. 비우려면 팀에 알린 뒤 seedClean을 씁니다");
        }
        confirmDatabaseName(options, settings, "넣으려면");
    }

    /**
     * DB 이름 입력 확인(-Pconfirm 또는 표준 입력)
     */
    private static void confirmDatabaseName(SeedOptions options, SeedSettings settings, String action) throws Exception {
        String typed = options.confirm();
        if (typed == null) {
            System.out.print("[시드] " + action + " DB 이름(" + settings.database() + ")을 입력하세요: ");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            typed = in.readLine();
        }
        if (typed == null || !typed.trim().equals(settings.database())) {
            throw new SeedException("DB 이름이 맞지 않아 DB를 바꾸지 않았습니다");
        }
    }

    private static void requireEmpty(Connection conn, Schema schema) throws Exception {
        Map<String, Long> filled = RowStore.nonEmpty(conn, schema, SeedTables.targets(schema));
        if (!filled.isEmpty()) {
            StringBuilder message = new StringBuilder("시드 대상 테이블 " + filled.size() + "개에 이미 데이터가 있습니다(섞어 넣지 않습니다):");
            filled.entrySet().stream().limit(5)
                    .forEach(e -> message.append(' ').append(e.getKey()).append('=').append(e.getValue()));
            if (filled.size() > 5) {
                message.append(" 외 ").append(filled.size() - 5).append("개");
            }
            throw new SeedException(message.toString());
        }
    }
}
