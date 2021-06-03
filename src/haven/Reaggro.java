package haven;

import java.util.concurrent.atomic.AtomicInteger;

import haven.purus.MultiSession;

public class Reaggro implements Runnable, FightEventAware {

    private final MultiSession.MultiSessionWindow multiSession;
    private final AtomicInteger counter = new AtomicInteger(0);
    private static final Integer LIMIT = 20;

    Reaggro(UI ui) {
        this.multiSession = ui.root.multiSessionWindow;
        this.multiSession.fightEventAwareList.add(this);
    }

    @Override
    public void run() {
        makeReaggroGreatAgain();
    }

    public void makeReaggroGreatAgain() {
        if (counter.incrementAndGet() >= LIMIT) {
            return;
        }

        clickAttackOnGob();
    }

    @Override
    public void fightStarted() {
        if (ensureSessionAggroed()) {
            counter.set(0);
            this.multiSession.fightEventAwareList.remove(this);
        } else {
            multiSession.ensureNotInFight();
            makeReaggroGreatAgain();
            sleep(5L);
        }
    }

    private boolean ensureSessionAggroed() {
        boolean aggro = false;

        for (int j = 0; j < 20; j++) {
            aggro = multiSession.isSessionAggroed();
            if (aggro) {
                break;
            }
            sleep(5);
        }
        return aggro;
    }

    private void clickAttackOnGob() {
        multiSession.triggerAttackIcon(); // no callback
        sleep(5L);
        multiSession.leftClickGob();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
