public final class ClassAltered {

    public static void main(String[] args) {
        int k = 8;
        for (int j = k; j < 20; j++) {
        }
        method1();
        method2(15);
    }

    private static void method1() {
        for (int j = 4; j > 15; j++) {
        }
    }

    private static void method2(int n) {
        for (int i = n; i <= 18; i++) {
        }
        for (int i = 4; i >= 0; i--) {
        }
    }
}
