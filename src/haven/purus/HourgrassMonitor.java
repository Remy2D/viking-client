package haven.purus;

import java.util.function.Function;

import haven.GameUI;

public class HourgrassMonitor implements Runnable {

    private static final Integer STEP_MS = 5;
    private final GameUI gui;
    private final Integer timeout;
    private final Runnable callback;

    public HourgrassMonitor(GameUI gui, Integer timeout, Runnable callback) {
        this.gui = gui;
        this.timeout = timeout;
        this.callback = callback;
    }

    @Override
    public void run() {
        waitForProgressStart();
        waitForProgressEnd();

        callback.run();
    }

    private void waitForProgressStart() {
        waitOnCondition(prog -> prog <= 0.0);
    }

    private void waitForProgressEnd() {
        waitOnCondition(prog -> prog >= 0.0);
    }

    private void waitOnCondition(Function<Double, Boolean> f) {
        int retries = 0;
        while (f.apply(gui.prog)) {
            if (retries * STEP_MS > timeout) {
                System.out.println("Retrun timedout " + retries * STEP_MS);
                return;
            }
            retries++;
            sleep();
        }
        System.out.println("Retrun after " + retries * STEP_MS + " with " + gui.prog);
    }

    private void sleep() {
        try {
            Thread.sleep(STEP_MS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

