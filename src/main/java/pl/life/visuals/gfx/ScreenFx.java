package pl.life.visuals.gfx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import pl.life.visuals.LifeVisualsClient;
import pl.life.visuals.module.Modules;

/**
 * Jedno przejscie post-processingu na obrazie swiata: Motion Blur (mieszanie z poprzednia klatka),
 * Color Saturation / kontrast, odcien (Atmosphere) i winieta. Dziala na wlasnym programie GLSL.
 * Gdy cos pojdzie nie tak (blad shadera), efekt wylacza sie sam i jest to zapisane w logu.
 */
public final class ScreenFx {
    private ScreenFx() {}

    private static boolean failed;
    private static int prog, vao;
    private static int uCur, uPrev, uBlur, uSat, uContrast, uTint, uTintAmt, uVig;
    private static int curTex, curFbo, prevTex, prevFbo, w, h;
    private static boolean prevValid;

    private static final String VERT = "#version 150\nout vec2 uv;\nvoid main(){\n"
            + " vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));\n"
            + " uv = p; gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);\n}\n";

    private static final String FRAG = "#version 150\nuniform sampler2D cur;\nuniform sampler2D prev;\n"
            + "uniform float blur;\nuniform float sat;\nuniform float contrast;\nuniform vec3 tint;\nuniform float tintAmt;\nuniform float vig;\n"
            + "in vec2 uv;\nout vec4 fragColor;\nvoid main(){\n"
            + " vec3 c = texture(cur, uv).rgb;\n"
            + " if (blur > 0.0) c = mix(c, texture(prev, uv).rgb, blur);\n"
            + " float l = dot(c, vec3(0.299, 0.587, 0.114));\n"
            + " c = mix(vec3(l), c, sat);\n"
            + " c = (c - 0.5) * contrast + 0.5;\n"
            + " c = mix(c, tint, tintAmt);\n"
            + " float d = distance(uv, vec2(0.5));\n"
            + " c *= 1.0 - vig * smoothstep(0.35, 0.85, d);\n"
            + " fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);\n}\n";

    private static int compile(int type, String src) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, src);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == 0) {
            throw new IllegalStateException("shader: " + GL20.glGetShaderInfoLog(s, 2048));
        }
        return s;
    }

    private static void init() {
        int vs = compile(GL20.GL_VERTEX_SHADER, VERT);
        int fs = compile(GL20.GL_FRAGMENT_SHADER, FRAG);
        prog = GL20.glCreateProgram();
        GL20.glAttachShader(prog, vs);
        GL20.glAttachShader(prog, fs);
        GL20.glLinkProgram(prog);
        if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == 0) {
            throw new IllegalStateException("link: " + GL20.glGetProgramInfoLog(prog, 2048));
        }
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        uCur = GL20.glGetUniformLocation(prog, "cur");
        uPrev = GL20.glGetUniformLocation(prog, "prev");
        uBlur = GL20.glGetUniformLocation(prog, "blur");
        uSat = GL20.glGetUniformLocation(prog, "sat");
        uContrast = GL20.glGetUniformLocation(prog, "contrast");
        uTint = GL20.glGetUniformLocation(prog, "tint");
        uTintAmt = GL20.glGetUniformLocation(prog, "tintAmt");
        uVig = GL20.glGetUniformLocation(prog, "vig");
        vao = GL30.glGenVertexArrays();
    }

    private static int makeTex(int width, int height) {
        int t = GL11.glGenTextures();
        GlStateManager._bindTexture(t);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
        return t;
    }

    private static int makeFbo(int tex) {
        int f = GL30.glGenFramebuffers();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, f);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, tex, 0);
        return f;
    }

    private static void ensureTargets(int width, int height) {
        if (width == w && height == h && curTex != 0) return;
        if (curTex != 0) {
            GL11.glDeleteTextures(curTex);
            GL11.glDeleteTextures(prevTex);
            GL30.glDeleteFramebuffers(curFbo);
            GL30.glDeleteFramebuffers(prevFbo);
        }
        w = width;
        h = height;
        curTex = makeTex(w, h);
        curFbo = makeFbo(curTex);
        prevTex = makeTex(w, h);
        prevFbo = makeFbo(prevTex);
        prevValid = false;
    }

    /** Wywolywane po wyrenderowaniu swiata (przed HUD i rysowaniem naszych efektow). */
    public static void apply(MinecraftClient mc) {
        boolean blurOn = Modules.MOTION_BLUR.enabled;
        boolean satOn = Modules.SATURATION.enabled;
        boolean tintOn = Modules.ATMOSPHERE.enabled && (Modules.AT_TINT_AMT.num() > 0.001f || Modules.AT_VIG.num() > 0.001f);
        if (failed || (!blurOn && !satOn && !tintOn)) {
            prevValid = false;
            return;
        }
        try {
            if (prog == 0) init();
            Framebuffer main = mc.getFramebuffer();
            ensureTargets(main.textureWidth, main.textureHeight);

            // kopia obrazu swiata
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, curFbo);
            GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

            main.beginWrite(true);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();

            float blur = blurOn && prevValid ? Modules.MB_STRENGTH.num() : 0f;
            float sat = satOn ? Modules.CS_SAT.num() : 1f;
            float contrast = satOn ? Modules.CS_CONTRAST.num() : 1f;
            int tc = Modules.AT_TINT.rgb();
            float tintAmt = tintOn ? Modules.AT_TINT_AMT.num() : 0f;
            float vig = tintOn ? Modules.AT_VIG.num() : 0f;

            GlStateManager._glUseProgram(prog);
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(curTex);
            GlStateManager._activeTexture(GL13.GL_TEXTURE1);
            GlStateManager._bindTexture(prevTex);
            GL20.glUniform1i(uCur, 0);
            GL20.glUniform1i(uPrev, 1);
            GL20.glUniform1f(uBlur, blur);
            GL20.glUniform1f(uSat, sat);
            GL20.glUniform1f(uContrast, contrast);
            GL20.glUniform3f(uTint, ((tc >> 16) & 255) / 255f, ((tc >> 8) & 255) / 255f, (tc & 255) / 255f);
            GL20.glUniform1f(uTintAmt, tintAmt);
            GL20.glUniform1f(uVig, vig);
            GlStateManager._glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
            GlStateManager._glBindVertexArray(0);
            GlStateManager._glUseProgram(0);
            GlStateManager._activeTexture(GL13.GL_TEXTURE1);
            GlStateManager._bindTexture(0);
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);

            // wynik zostaje jako "poprzednia klatka" dla rozmycia ruchu
            if (blurOn) {
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevFbo);
                GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
                prevValid = true;
            } else {
                prevValid = false;
            }
            main.beginWrite(true);
            BufferRenderer.reset();
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
        } catch (Throwable t) {
            failed = true;
            LifeVisualsClient.LOGGER.error("Screen effects disabled because of an error", t);
            try {
                GlStateManager._glUseProgram(0);
                GlStateManager._glBindVertexArray(0);
                MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
            } catch (Throwable ignored) {
            }
        }
    }
}
