package com.nyangmeong.seed;

/**
 * [실행 옵션]
 * - 첫 인자 data·files·clean, 나머지 --이름=값 (build.gradle이 -P 값을 옮긴다)
 */
public record SeedOptions(Command command, Target target, int scale, String confirm, boolean reset) {

    private static final int MAX_SCALE = 20;

    public enum Command {
        DATA, FILES, CLEAN
    }

    public enum Target {
        SHARED("공용 DB"), LOCAL("로컬 DB");

        private final String label;

        Target(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public static SeedOptions parse(String[] args) {
        if (args.length == 0) {
            throw new SeedException("첫 인자는 data·files·clean 중 하나입니다");
        }
        Command command = switch (args[0]) {
        case "data" -> Command.DATA;
        case "files" -> Command.FILES;
        case "clean" -> Command.CLEAN;
        default -> throw new SeedException("알 수 없는 명령: " + args[0]);
        };
        Target target = Target.SHARED;
        int scale = 1;
        String confirm = null;
        boolean reset = false;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            int eq = arg.indexOf('=');
            String name = eq < 0 ? arg : arg.substring(0, eq);
            String value = eq < 0 ? "" : arg.substring(eq + 1);
            switch (name) {
            case "--target" -> target = switch (value) {
            case "shared" -> Target.SHARED;
            case "local" -> Target.LOCAL;
            default -> throw new SeedException("-Ptarget은 shared 또는 local입니다: " + value);
            };
            case "--scale" -> scale = parseScale(value);
            case "--confirm" -> confirm = value;
            // -Preset 처럼 값 없이 주면 Gradle이 빈 문자열을 넘긴다
            case "--reset" -> reset = !"false".equalsIgnoreCase(value);
            default -> throw new SeedException("알 수 없는 옵션: " + arg);
            }
        }
        return new SeedOptions(command, target, scale, confirm, reset);
    }

    private static int parseScale(String value) {
        try {
            int scale = Integer.parseInt(value);
            if (scale < 1 || scale > MAX_SCALE) {
                throw new SeedException("-Pscale은 1~" + MAX_SCALE + "입니다: " + value);
            }
            return scale;
        } catch (NumberFormatException e) {
            throw new SeedException("-Pscale은 숫자입니다: " + value);
        }
    }
}
