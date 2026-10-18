package pl.faze.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Rysowanie w swiecie: Box ESP, Target ESP (pierscien), Target Highlight, Damage Indicator. */
public final class WorldVisuals {
    private WorldVisuals() {}

    public static void init() {
        WorldRenderEvents.LAST.register(WorldVisuals::render);
    }

    private static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        MatrixStack ms = ctx.matrixStack();
        if (ms == null) return;
        Config c = Config.get();
        Camera cam = ctx.camera();
        Vec3d cp = cam.getPos();
        float delta = mc.getRenderTickCounter().getTickDelta(true);
        PlayerEntity target = TargetTracker.getTarget();

        boolean needLines = c.boxEsp || (c.targetEsp && target != null) || (c.highlight && target != null);
        if (needLines) {
            ms.push();
            ms.translate(-cp.x, -cp.y, -cp.z);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();

            // wypelnienie celu
            if (c.highlight && target != null && target != mc.player) {
                RenderSystem.depthMask(false);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                filledBox(b, ms.peek().getPositionMatrix(), lerpedBox(target, delta).expand(0.02), c.highlightColor, c.highlightAlpha);
                BufferRenderer.drawWithGlobalProgram(b.end());
                RenderSystem.depthMask(true);
            }

            // linie
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            RenderSystem.lineWidth(c.boxThickness);
            BufferBuilder lb = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
            MatrixStack.Entry entry = ms.peek();
            boolean any = false;

            if (c.boxEsp) {
                for (PlayerEntity p : mc.world.getPlayers()) {
                    if (p == mc.player || p.isInvisible() || p.isSpectator()) continue;
                    if (p == target && c.highlight) continue; // cel ma wlasne wyroznienie
                    outline(lb, entry, lerpedBox(p, delta), c.boxColor, 1f);
                    any = true;
                }
            }
            if (target != null && target != mc.player) {
                if (c.highlight) {
                    outline(lb, entry, lerpedBox(target, delta).expand(0.02), c.highlightColor, 1f);
                    any = true;
                }
                if (c.targetEsp) {
                    ring(lb, entry, target, delta, c.targetEspColor);
                    any = true;
                }
            }
            if (any) {
                BufferRenderer.drawWithGlobalProgram(lb.end());
            }
            RenderSystem.lineWidth(1f);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            ms.pop();
        }

        floatingTexts(ms, cam, mc, c);
    }

    private static Box lerpedBox(Entity e, float delta) {
        Vec3d lp = e.getLerpedPos(delta);
        return e.getBoundingBox().offset(lp.x - e.getX(), lp.y - e.getY(), lp.z - e.getZ());
    }

    private static void line(BufferBuilder b, MatrixStack.Entry en, double x1, double y1, double z1,
                             double x2, double y2, double z2, int rgb, float a) {
        float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) return;
        dx /= len; dy /= len; dz /= len;
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, bl = (rgb & 255) / 255f;
        Matrix4f m = en.getPositionMatrix();
        b.vertex(m, (float) x1, (float) y1, (float) z1).color(r, g, bl, a).normal(en, dx, dy, dz);
        b.vertex(m, (float) x2, (float) y2, (float) z2).color(r, g, bl, a).normal(en, dx, dy, dz);
    }

    private static void outline(BufferBuilder b, MatrixStack.Entry en, Box bx, int rgb, float a) {
        double x1 = bx.minX, y1 = bx.minY, z1 = bx.minZ, x2 = bx.maxX, y2 = bx.maxY, z2 = bx.maxZ;
        line(b, en, x1, y1, z1, x2, y1, z1, rgb, a);
        line(b, en, x2, y1, z1, x2, y1, z2, rgb, a);
        line(b, en, x2, y1, z2, x1, y1, z2, rgb, a);
        line(b, en, x1, y1, z2, x1, y1, z1, rgb, a);
        line(b, en, x1, y2, z1, x2, y2, z1, rgb, a);
        line(b, en, x2, y2, z1, x2, y2, z2, rgb, a);
        line(b, en, x2, y2, z2, x1, y2, z2, rgb, a);
        line(b, en, x1, y2, z2, x1, y2, z1, rgb, a);
        line(b, en, x1, y1, z1, x1, y2, z1, rgb, a);
        line(b, en, x2, y1, z1, x2, y2, z1, rgb, a);
        line(b, en, x2, y1, z2, x2, y2, z2, rgb, a);
        line(b, en, x1, y1, z2, x1, y2, z2, rgb, a);
    }

    /** Target ESP: pierscien krazacy w gore i w dol wokol celu. */
    private static void ring(BufferBuilder b, MatrixStack.Entry en, Entity e, float delta, int rgb) {
        Vec3d lp = e.getLerpedPos(delta);
        double t = (System.currentTimeMillis() % 1800L) / 1800.0;
        double h = e.getHeight();
        double y = lp.y + h * (0.5 + 0.5 * Math.sin(t * Math.PI * 2));
        double r = e.getWidth() * 0.9 + 0.1;
        int seg = 40;
        double px = lp.x + r, pz = lp.z;
        for (int i = 1; i <= seg; i++) {
            double a = i * (Math.PI * 2 / seg);
            double nx = lp.x + Math.cos(a) * r, nz = lp.z + Math.sin(a) * r;
            line(b, en, px, y, pz, nx, y, nz, rgb, 1f);
            px = nx;
            pz = nz;
        }
    }

    private static void filledBox(BufferBuilder b, Matrix4f m, Box bx, int rgb, float a) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, bl = (rgb & 255) / 255f;
        float x1 = (float) bx.minX, y1 = (float) bx.minY, z1 = (float) bx.minZ;
        float x2 = (float) bx.maxX, y2 = (float) bx.maxY, z2 = (float) bx.maxZ;
        quad(b, m, r, g, bl, a, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2); // dol
        quad(b, m, r, g, bl, a, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1); // gora
        quad(b, m, r, g, bl, a, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1); // polnoc
        quad(b, m, r, g, bl, a, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2); // poludnie
        quad(b, m, r, g, bl, a, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1); // zachod
        quad(b, m, r, g, bl, a, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2); // wschod
    }

    private static void quad(BufferBuilder b, Matrix4f m, float r, float g, float bl, float a,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        b.vertex(m, ax, ay, az).color(r, g, bl, a);
        b.vertex(m, bx, by, bz).color(r, g, bl, a);
        b.vertex(m, cx, cy, cz).color(r, g, bl, a);
        b.vertex(m, dx, dy, dz).color(r, g, bl, a);
    }

    private static void floatingTexts(MatrixStack ms, Camera cam, MinecraftClient mc, Config c) {
        if (TargetTracker.texts.isEmpty()) return;
        TextRenderer tr = mc.textRenderer;
        VertexConsumerProvider.Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();
        Vec3d cp = cam.getPos();
        long now = System.currentTimeMillis();
        for (TargetTracker.FloatingText t : TargetTracker.texts) {
            float age = (now - t.created) / 1000f;
            if (age < 0f || age > 1f) continue;
            int alpha = Math.max(6, (int) (255 * (1f - age * age)));
            int color = (alpha << 24) | (t.color & 0xFFFFFF);
            ms.push();
            ms.translate(t.x - cp.x, t.y + age * 0.6 - cp.y, t.z - cp.z);
            ms.multiply(cam.getRotation());
            float s = 0.03f;
            ms.scale(-s, -s, s);
            Matrix4f m = ms.peek().getPositionMatrix();
            Text text = Text.literal(t.text);
            float w = tr.getWidth(text);
            tr.draw(text, -w / 2f, 0f, color, false, m, imm, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
            ms.pop();
        }
        imm.draw();
    }
}
