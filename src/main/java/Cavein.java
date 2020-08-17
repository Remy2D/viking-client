import haven.Config;
import haven.Coord3f;
import haven.GOut;

import java.awt.*;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Cavein extends haven.Sprite implements haven.Gob.Overlay.CDel {
	private static final Random rnd = new Random();;
	public class Boll {
		Coord3f position;
		Coord3f velocity;
		Coord3f normal;
		float sz;
		float t;

		Boll(Coord3f pos, float delta) {
			this.position = new Coord3f(pos.x, pos.y, pos.z);
			this.velocity = new Coord3f(0.0F, 0.0F, 0.0F);
			this.normal = new Coord3f(Cavein.this.rnd.nextFloat() - 0.5F, Cavein.this.rnd.nextFloat() - 0.5F, Cavein.this.rnd.nextFloat() - 0.5F).norm();
			this.sz = delta;
			this.t = -1.0F;
		}

		boolean tick(float dt) {
			this.velocity.z -= dt;
			this.velocity.z = Math.min(0.0F, this.velocity.z + dt * 5.0F * this.velocity.z * this.velocity.z / this.sz);
			this.velocity.x += dt * (float) Cavein.this.rnd.nextGaussian() * 0.1F;
			this.velocity.y += dt * (float) Cavein.this.rnd.nextGaussian() * 0.1F;
			this.position.x += this.velocity.x;
			this.position.y += this.velocity.y;
			this.position.z += this.velocity.z;
			if (this.position.z < 0.0F) {
				this.position.z = 0.0F;
				this.velocity.z *= -0.7F;
				this.velocity.x = (this.velocity.z * (Cavein.this.rnd.nextFloat() - 0.5F));
				this.velocity.y = (this.velocity.z * (Cavein.this.rnd.nextFloat() - 0.5F));
				if (this.t < 0.0F) {
					this.t = 0.0F;
				}
			}
			if (this.t >= 0.0F) {
				this.t += dt;
			}
			return this.t > 1.5F;
		}
	}

	private final haven.GLState mat;
	List<Boll> bollar = new LinkedList<>();
	boolean spawn = true;
	FloatBuffer posb = null;
	FloatBuffer nrmb = null;
	float de = 0.0F;
	float str;
	float life;
	Coord3f off;
	haven.Coord sz;

	public Cavein(haven.Sprite.Owner paramOwner, haven.Resource paramResource, haven.Message paramMessage) {
		super(paramOwner, paramResource);
		str = paramMessage.uint8();
		sz = new haven.Coord(paramMessage.uint8(), paramMessage.uint8());
		off = new Coord3f(-sz.x / 2.0F, -sz.y / 2.0F, paramMessage.uint8());
		paramMessage.uint8(); //Ignore server "life"
		life = 60 * 30; //dust lasts 30 minutes

		if (Config.colorfulCavein) {
			mat = new haven.Material.Colors(new Color(Color.HSBtoRGB(rnd.nextFloat(), 1.0f, 1.0f)),
					new Color(Color.HSBtoRGB(rnd.nextFloat(), 1.0f, 1.0f)),
					new Color(Color.HSBtoRGB(rnd.nextFloat(), 1.0f, 1.0f)),
					new Color(0, 0, 0), 1.0F);
		} else {
			mat = new haven.Material.Colors(new Color(255, 255, 255),
					new Color(255, 255, 255),
					new Color(128, 128, 128),
					new Color(0, 0, 0), 1.0F);
		}
	}

	public void draw(GOut g) {
		updpos(g);
		if (posb == null)
			return;
		g.apply();
		posb.rewind();
		nrmb.rewind();
		g.gl.glPointSize(1.1F);
		g.gl.glEnableClientState(32884);
		g.gl.glVertexPointer(3, 5126, 0, posb);
		g.gl.glEnableClientState(32885);
		g.gl.glNormalPointer(5126, 0, nrmb);
		g.gl.glDrawArrays(0, 0, bollar.size());
		g.gl.glDisableClientState(32884);
		g.gl.glDisableClientState(32885);
	}

	void updpos(GOut g) {
		if (bollar.size() < 1) {
			posb = null;
			nrmb = null;
			return;
		}
		if ((posb == null) || (posb.capacity() < bollar.size() * 3)) {
			int i = posb == null ? 512 : posb.capacity() / 3;
			posb = haven.Utils.mkfbuf(i * 2 * 3);
			nrmb = haven.Utils.mkfbuf(i * 2 * 3);
		}
		FloatBuffer pos = haven.Utils.wfbuf(3 * bollar.size());
		FloatBuffer norm = haven.Utils.wfbuf(3 * bollar.size());
		for (Cavein.Boll boll : bollar) {
			pos.put(boll.position.x).put(boll.position.y).put(boll.position.z);
			norm.put(boll.normal.x).put(boll.normal.y).put(boll.normal.z);
		}
		g.gl.bglCopyBufferf(posb, 0, pos, 0, pos.capacity());
		g.gl.bglCopyBufferf(nrmb, 0, norm, 0, pos.capacity());
	}

	public boolean tick(int dt) {
		float f = dt / 1000.0F;
		de += f * str;
		if ((spawn) && (de > 1.0F)) {
			de -= 1.0F;
			bollar.add(new Cavein.Boll(off.add(rnd.nextFloat() * sz.x, rnd.nextFloat() * sz.y, 0.0F),
					0.5F + rnd.nextFloat() * 1.5F));
		}
		//Remove dead dust
		bollar.removeIf(boll -> boll.tick(f));

		if ((life > 0.0F) && ((this.life -= f) <= 0.0F))
			spawn = false;
		return (!spawn) && (bollar.isEmpty());
	}

	public boolean setup(haven.RenderList rl) {
		rl.prepo(haven.Light.deflight);
		rl.prepo(mat);
		return true;
	}

	public void delete() {
		spawn = false;
	}
}