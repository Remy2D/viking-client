package haven.purus;

import haven.*;

import java.awt.event.KeyEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BreakNotify {

	private static ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
	private static Runnable run;


	public static void start() { run = new Runnable() {
			@Override
			public void run() {
				BreakWnd bw = new BreakWnd();
				HavenPanel.lui.root.add(bw);
				bw.show();
				bw.raise();
			}
		};
		ses.schedule(run,30, TimeUnit.MINUTES);
	}

	private static class BreakWnd extends Window {
		public BreakWnd() {
			super(Coord.z, "Why not take a break? ");
			add(new RichTextBox(new Coord(200, 125), "Sitting for long periods greatly increases the risk to get some serious, fatal diseases.\n" +
					"Purus Pasta recommends you to stand up, move your arms and legs for a small workout. Even if just for a moment. As it is better than only sitting."));
			add(new Button(60, "Yes, I will") {
				@Override
				public void click() {
					ses.schedule(run,30, TimeUnit.MINUTES);
					parent.reqdestroy();
				}
			}, new Coord(629 / 2 - 60, 50));

			add(new Button(60, "No") {
				@Override
				public void click() {
					parent.reqdestroy();
				}
			}, new Coord(629 / 2 - 60, 80));
			pack();
			this.c = new Coord(HavenPanel.w / 2 - sz.x / 2, HavenPanel.h / 2 - sz.y / 2);
		}

		@Override
		public void wdgmsg (Widget sender, String msg, Object...args){
			if(sender == cbtn) {
				ui.destroy(this);
				ses.schedule(run,30, TimeUnit.MINUTES);
			} else {
				super.wdgmsg(sender, msg, args);
			}
		}

		@Override
		public boolean keydown ( KeyEvent ev){
			if(ev.getKeyChar() == KeyEvent.VK_ESCAPE) {
				wdgmsg(cbtn, "click");
				return (true);
			}
			return super.keydown(ev);
		}
	}
}
