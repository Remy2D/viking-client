package haven.purus;

import haven.*;
import haven.Button;
import haven.Config;
import haven.purus.pbot.api.PBotGob;
import haven.purus.pbot.api.PBotSession;
import haven.render.Render;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class MultiSession {
    public static final ArrayList<UI> sessions = new ArrayList<>();
    public static UI activeSession;

    public static KeyBinding kb_nextSession = KeyBinding.get("next_session", KeyMatch.forcode(KeyEvent.VK_PAGE_DOWN, 0));
    public static KeyBinding kb_prevSession = KeyBinding.get("previous_session", KeyMatch.forcode(KeyEvent.VK_PAGE_UP, 0));

    public static class MultiSessionWindow extends BetterWindow {
        public final List<FightEventAware> fightEventAwareList = new ArrayList<>();

        public boolean fightExtensionsEnabled;
        public boolean doubleTapEnabled;
        public boolean locked = false;
        boolean update = true;
        Gob multisessionGob = null;

        public void toggleFightExtensionsEnabled() {
            this.fightExtensionsEnabled ^= true;
            System.out.println("Toggled fight extensions " + fightExtensionsEnabled);
        }

        public void toggleDoubleTapEnabled() {
            this.doubleTapEnabled ^= true;
            System.out.println("Toggled double tap " + doubleTapEnabled);
        }

        public MultiSessionWindow() {
            super(UI.scale(0, 0), "Sessions");
            this.visible = false;
        }

        public void update() {
            update = true;
        }

        public void doClick(Coord c, int button) {
            if (locked) {
                if (button == 1) {
                    startFightSequence(c, button);
                    ensureFightOff();
                } else if (button == 3) {
                    multisessionRightClick();
                }
            }
        }

        private void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void ensureNotInFight() {
            for (int i = 0; i < 20; i++) {
                ensureFightOff();
                if (!isSessionInFight()) {
                    break;
                }
            }
        }

        public void triggerAttackIcon() {
            if (ui.fightGobCache.getLastAggroedEvent() != null) {
                runKeyCommandThis(ui.fightGobCache.getLastAggroedEvent());
            }
        }

        public void startFightSequence(Coord c, int button) {
            ui.gui.map.mousedown(c, button, Optional.of(this::multisessionGobCallback));
        }

        private void multisessionGobCallback(Gob gob, Integer button) {
            this.multisessionGob = gob;
            switch (button) {
                case 1:
                    leftClickGob();
                    new Thread(new HourgrassMonitor(ui.gui, 2000, this::secondCallback)).start();
                    break;
                case 3:
                    multisessionRightClick();
                    break;
            }
        }

        private void secondCallback() {
            if (doubleTapEnabled) {
                doubleTap();
            }
            ensureFightOff();
            multisessionRightClick();

            fightEventAwareList.forEach(FightEventAware::fightStarted);
        }

        private void doubleTap() {
            System.out.println("Each session double tap");
            runForEverySession(ui -> {
                PBotSession pSession = new PBotSession(ui.gui);

                if (!isSessionInFight()) {
                    clickGob(pSession);
                }
            });
        }

        private void ensureFightOff() {
            System.out.println("Each session fight off");
            runForEverySession(ui -> {
                if (isNotWaitingForCancel()) {
                    System.out.println("Session fight off click");
                    ui.gui.fv.current.give.wdgmsg("click", 1);
                }
            });
        }

        private boolean isNotWaitingForCancel() {
            return ui.gui.fv.curgive != null && ui.gui.fv.curgive.state != 1;
        }

        public boolean isSessionAggroed() {
            return ui.gui.fv.curgive != null &&
                    (ui.gui.fv.curgive.state == 0 || ui.gui.fv.curgive.state == 1);
        }

        private boolean isSessionInFight() {
            return ui.gui.fv.curgive != null && ui.gui.fv.curgive.state != -1;
        }

        public void leftClickGob() {
            if (multisessionGob != null) {
                System.out.println("Each session click: " + multisessionGob.id);
                runForEverySession(ui -> {
                    PBotSession pSession = new PBotSession(ui.gui);
                    System.out.println("Left");
                    clickGob(pSession);
                });
            }
        }

        private void clickGob(PBotSession pSession) {
            PBotGob pGob = new PBotGob(multisessionGob, pSession);

            pGob.doClick(1, 0, -1, 0);
        }

        private void rightClick(UI ui) {
            System.out.println("Single session cancel " + ui.sess.username);
            PBotSession pSession = new PBotSession(ui.gui);
            pSession.PBotCharacterAPI().cancelAct();
            ui.mousegrab.clear();
        }

        private void multisessionRightClick() {
            System.out.println("Each session cancel");
            runForEverySession(this::rightClick);
            locked = false;
        }

        public void runKeyCommand(KeyEvent ev) {
            if (fightExtensionsEnabled) {
                runForOtherSessions(ui -> ui.keydown(ev));
                ui.fightGobCache.setLastAggroedEvent(ev);
                locked = true;
            }
        }

        public void runKeyCommandThis(KeyEvent ev) {
            if (fightExtensionsEnabled) {
                runForEverySession(ui -> ui.keydown(ev));
                locked = true;
            }
        }

        private void runForOtherSessions(Consumer<? super UI> c) {
            synchronized (sessions) {
                sessions.stream()
                        .filter(ui -> ui != activeSession)
                        .forEach(c);
            }
        }

        private void runForEverySession(Consumer<? super UI> c) {
            synchronized (sessions) {
                sessions.stream()
                        .forEach(c);
            }
        }

        @Override
        public void wdgmsg(Widget sender, String msg, Object... args) {
            if (sender == cbtn) {
                reqdestroy();
                return;
            }
            super.wdgmsg(sender, msg, args);
        }

        @Override
        public void gtick(Render out) {
            if (update) {
                if (haven.purus.Config.disableSessWnd.val) {
                    hide();
                    super.gtick(out);
                    return;
                }
                show();
                for (Widget w : this.children(Button.class)) {
                    w.destroy();
                }
                int ofsY = -UI.scale(5);
                synchronized (sessions) {
                    for (UI session : sessions) {
                        Button btn = add(new Button(UI.scale(200), (session.sess != null ? session.sess.username : "???")), 10, ofsY + UI.scale(5));
                        btn.action = () -> {
                            MultiSession.setActiveSession(session);
                        };
                        if (session == activeSession)
                            btn.change(btn.text.text, Color.ORANGE);
                        ofsY += btn.sz.y + UI.scale(5);
                    }
                    if (sessions.stream().noneMatch((ses) -> (ses.sess == null))) {
                        Button btn = add(new Button(UI.scale(200), "New Session", () -> {
                            MainFrame.mf.sessionCreate();
                        }) {
                            @Override
                            public void click() {
                                super.click();
                                this.destroy();
                            }
                        }, 10, ofsY + UI.scale(5));
                        ofsY += btn.sz.y + UI.scale(5);
                    }
                }
                this.resize(UI.scale(220), ofsY + UI.scale(5));
                update = false;
            }
            super.gtick(out);
        }
    }

    public static void addSession(UI ui) {
        synchronized (sessions) {
            sessions.add(ui);
        }
        ui.audio.amb.setVolumeNoSave(0);
        ui.audio.pos.setVolumeNoSave(0);
        if (activeSession != null && activeSession.root != null && activeSession.root.multiSessionWindow != null)
            activeSession.root.multiSessionWindow.update();
    }

    public static void closeSession(UI ui) {
        synchronized (sessions) {
            if (ui == activeSession)
                nextSession(1);
            sessions.remove(ui);
        }
        synchronized (ui) {
            if (ui.sess != null)
                ui.sess.close();
            else
                ui.destroy();
        }
        if (activeSession != null && activeSession.root != null && activeSession.root.multiSessionWindow != null)
            activeSession.root.multiSessionWindow.update();
    }

    public static void setActiveSession(UI ui) {
        if (ui.sess != null)
            MainFrame.mf.setTitle("Viking Client " + Config.version + " \u2013 " + ui.sess.username);
        else
            MainFrame.mf.setTitle("Viking Client " + Config.version);
        if (activeSession != null) {
            activeSession.audio.amb.setVolumeNoSave(0);
            activeSession.audio.pos.setVolumeNoSave(0);
        }
        synchronized (sessions) {
            activeSession = ui;
        }
        activeSession.audio.amb.setVolumeNoSave(Double.parseDouble(Utils.getpref("sfxvol-" + ui.audio.amb.name, "1.0")));
        activeSession.audio.pos.setVolumeNoSave(Double.parseDouble(Utils.getpref("sfxvol-" + ui.audio.pos.name, "1.0")));
        if (activeSession.root != null && activeSession.root.multiSessionWindow != null)
            activeSession.root.multiSessionWindow.update();
    }

    public static void nextSession(int ofs) {
        synchronized (sessions) {
            if (activeSession == null) {
                if (!sessions.isEmpty())
                    activeSession = sessions.get(sessions.size() - 1);
            } else {
                int currentIdx = sessions.indexOf(activeSession);
                currentIdx += ofs + sessions.size();
                currentIdx %= sessions.size();
                setActiveSession(sessions.get(currentIdx));
            }
        }
    }
}
