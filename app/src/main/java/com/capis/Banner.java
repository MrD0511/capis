package com.capis;

public class Banner {
    private static final String RESET = "\u001B[0m";
    private static final boolean COLOR = System.getenv("NO_COLOR") == null;

    private static String c(String code, String text) {
        return COLOR ? "\u001B[" + code + "m" + text + RESET : text;
    }

    private static void row(String label, String value, String valueColor) {
        String left = String.format("%-16s", label);          // 16 wide: leaves a gap after the longest label
        int pad = Math.max(0, 28 - value.length());           // 2 + 2 + 16 + 28 + 2 = 50 inner width
        System.out.println("  " + c("90", "│") + "  " + c("92", "●") + " " + c("90", left)
                + c(valueColor, value) + " ".repeat(pad) + "  " + c("90", "│"));
    }

    public static void print(int port, String evictionPolicy, int capacity, int purgeLimit) {
        String[] logo = {
            "   ██████╗ █████╗ ██████╗ ██╗███████╗",
            "  ██╔════╝██╔══██╗██╔══██╗██║██╔════╝",
            "  ██║     ███████║██████╔╝██║███████╗",
            "  ██║     ██╔══██║██╔═══╝ ██║╚════██║",
            "  ╚██████╗██║  ██║██║     ██║███████║",
            "   ╚═════╝╚═╝  ╚═╝╚═╝     ╚═╝╚══════╝"
        };
        // cyan -> blue gradient (256-color codes)
        String[] gradient = {"38;5;51", "38;5;45", "38;5;39", "38;5;33", "38;5;27", "38;5;21"};

        System.out.println();
        for (int i = 0; i < logo.length; i++) {
            System.out.println(c(gradient[i], logo[i]));
        }

        System.out.println();
        System.out.println("  " + c("1;97", "CAPIS") + " " + c("96", "v0.1.0")
                + c("90", "  ·  ") + c("37", "Redis-compatible in-memory key-value store"));
        System.out.println();

        String line = "─".repeat(50);
        System.out.println("  " + c("90", "╭" + line + "╮"));
        row("Status",          "RUNNING",            "1;92");
        row("Port",            String.valueOf(port), "1;97");
        row("Eviction policy", evictionPolicy,       "1;97");
        row("Capacity",        String.valueOf(capacity), "1;97");
        row("Purge limit",     String.valueOf(purgeLimit), "1;97");
        System.out.println("  " + c("90", "╰" + line + "╯"));

        System.out.println();
        System.out.println("  " + c("92", "✔") + " Ready to accept connections");
        System.out.println("  " + c("90", "→ try: ") + c("96", "redis-cli -p " + port));
        System.out.println();
    }
}
