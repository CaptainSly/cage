package com.github.cage;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Random;

/**
 * This class does most of the captcha drawing. This class is thread safe.
 * 
 * @author akiraly
 * 
 */
public class Painter {
	private static final long serialVersionUID = 5691787632585996912L;

	/**
	 * Enumeration for different image quality levels.
	 */
	public static enum Quality {
		MIN, DEFAULT, MAX
	}

	public static final int DEFAULT_WIDTH = 200;
	public static final int DEFAULT_HEIGHT = 70;

	private final int width;
	private final int height;
	private final Color background;
	private final Quality quality;
	private final boolean rippleEnabled;
	private final boolean blurEnabled;
	private final boolean outlineEnabled;
	private final boolean rotateEnabled;
	private final Random rnd;

	/**
	 * Default constructor calls
	 * {@link Painter#Painter(int, int, Color, Quality, boolean, boolean, boolean, boolean, Random)
	 * )}
	 */
	public Painter() {
		this(DEFAULT_WIDTH, DEFAULT_HEIGHT, null, null, true, true, false,
				true, null);
	}

	/**
	 * Constructor
	 * 
	 * @param rnd
	 *            random generator to be used, can be null
	 */
	public Painter(Random rnd) {
		this(DEFAULT_WIDTH, DEFAULT_HEIGHT, null, null, true, true, false,
				true, rnd);
	}

	/**
	 * Constructor
	 * 
	 * @param width
	 *            captcha image width, default {@link #DEFAULT_WIDTH}
	 * @param height
	 *            captcha image height, default {@link #DEFAULT_HEIGHT}
	 * @param bGround
	 *            background color of captcha image, default white, can be null
	 * @param quality
	 *            captcha image quality, default {@link Quality#MAX}, should use
	 *            max it does not have measurable speed penalty on modern jvm-s
	 *            (1.6u23), can be null
	 * @param ripple
	 *            waving effect should be used, default true, disabling this
	 *            helps performance
	 * @param blur
	 *            should the image be blurred, default true, disabling this
	 *            helps performance
	 * @param outline
	 *            should a shifted, font colored outline be drawn behind the
	 *            characters, default false, disabling this helps performance
	 *            slightly
	 * @param rotate
	 *            should the letters be rotated independently, default true,
	 *            disabling this helps performance slightly
	 * @param rnd
	 *            random generator to be used, can be null
	 */
	public Painter(int width, int height, Color bGround, Quality quality,
			boolean ripple, boolean blur, boolean outline, boolean rotate,
			Random rnd) {
		super();
		this.width = width;
		this.height = height;
		this.background = bGround != null ? bGround : Color.WHITE;
		this.quality = quality != null ? quality : Quality.MAX;
		this.rippleEnabled = ripple;
		this.blurEnabled = blur;
		this.outlineEnabled = outline;
		this.rotateEnabled = rotate;
		this.rnd = rnd != null ? rnd : new Random();
	}

	/**
	 * Generates a new captcha image.
	 * 
	 * @param font
	 *            will be used for text, not null
	 * @param fGround
	 *            will be used for text, not null
	 * @param text
	 *            this will be rendered on the image, not null, not 0 length
	 * @return the generated image
	 */
	public BufferedImage draw(Font font, Color fGround, String text) {
		if (font == null)
			throw new IllegalArgumentException("Font can not be null.");
		if (fGround == null)
			throw new IllegalArgumentException(
					"Foreground color can not be null.");
		if (text == null || text.length() < 1)
			throw new IllegalArgumentException("No text given.");

		BufferedImage img = createImage();

		Graphics g = img.getGraphics();
		try {
			if (!(g instanceof Graphics2D))
				throw new IllegalStateException("Image (" + img
						+ ") has a graphics (" + g
						+ ") that is not an instance of Graphics2D.");
			Graphics2D g2 = (Graphics2D) g;
			configureGraphics(g2, font, fGround);

			draw(g2, text);

			img = postProcess(img);
		} finally {
			g.dispose();
		}

		return img;
	}

	/**
	 * Creates a new image to draw upon.
	 * 
	 * @return new image
	 */
	protected BufferedImage createImage() {
		return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
	}

	/**
	 * Configures graphics object before drawing text on it.
	 * 
	 * @param g2
	 *            to be configured
	 * @param font
	 *            to be used for the text
	 * @param fGround
	 *            to be used for the text
	 */
	protected void configureGraphics(Graphics2D g2, Font font, Color fGround) {
		configureGraphicsQuality(g2);

		g2.setColor(fGround);
		g2.setBackground(background);
		g2.setFont(font);

		g2.clearRect(0, 0, width, height);
	}

	/**
	 * Sets quality related hints based on the quality field of this object.
	 * 
	 * @param g2
	 *            to be configured
	 */
	protected void configureGraphicsQuality(Graphics2D g2) {
		switch (quality) {
		case MAX:
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
					RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
					RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_DITHERING,
					RenderingHints.VALUE_DITHER_ENABLE);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
					RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_QUALITY);
			break;
		case MIN:
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
					RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_OFF);
			g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
					RenderingHints.VALUE_COLOR_RENDER_SPEED);
			g2.setRenderingHint(RenderingHints.KEY_DITHERING,
					RenderingHints.VALUE_DITHER_DISABLE);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
					RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_SPEED);
			break;
		}
	}

	/**
	 * Does some of the text transformation (like rotation, scaling) and draws
	 * the result.
	 * 
	 * @param g
	 *            to be drawn upon
	 * @param text
	 *            to be drawn
	 */
	protected void draw(Graphics2D g, String text) {
		GlyphVector vector = g.getFont().createGlyphVector(
				g.getFontRenderContext(), text);

		transform(g, text, vector);

		Rectangle bounds = vector.getPixelBounds(null, 0, height);
		float bw = (float) bounds.getWidth();
		float bh = (float) bounds.getHeight();

		// transform + scale text to better fit the image
		float wr = width / bw * (rnd.nextFloat() / 2.5f + 0.5f);
		float hr = height / bh
				* (rnd.nextFloat() / 4 + (outlineEnabled ? 0.45f : 0.55f));
		g.translate((width - bw * wr) / 2, (height - bh * hr) / 2);
		g.scale(wr, hr);

		float bx = (float) bounds.getX();
		float by = (float) bounds.getY();
		// draw outline if needed
		if (outlineEnabled)
			g.draw(vector.getOutline(Math.signum(rnd.nextFloat() - 0.5f) * 1
					* width / 200 - bx, Math.signum(rnd.nextFloat() - 0.5f) * 1
					* height / 70 + height - by));
		g.drawGlyphVector(vector, -bx, height - by);
	}

	/**
	 * Does some of the text transformation.
	 * 
	 * @param g
	 *            to be drawn upon
	 * @param text
	 *            to be drawn
	 * @param v
	 *            graphical representation of text, to be transformed
	 */
	protected void transform(Graphics2D g, String text, GlyphVector v) {
		int glyphNum = v.getNumGlyphs();

		Point2D prePos = null;
		Rectangle2D preBounds = null;

		double rotateCur = (rnd.nextDouble() - 0.5) * Math.PI / 8;
		double rotateStep = Math.signum(rotateCur)
				* (rnd.nextDouble() * Math.PI / 2 / glyphNum);

		for (int fi = 0; fi < glyphNum; fi++) {
			if (rotateEnabled) {
				AffineTransform tr = AffineTransform
						.getRotateInstance(rotateCur);
				if (rnd.nextDouble() < 0.25)
					rotateStep *= -1;
				rotateCur += rotateStep;
				v.setGlyphTransform(fi, tr);
			}
			Point2D pos = v.getGlyphPosition(fi);
			Rectangle2D bounds = v.getGlyphVisualBounds(fi).getBounds2D();
			Point2D newPos;
			if (prePos == null)
				newPos = new Point2D.Double(pos.getX() - bounds.getX(),
						pos.getY());
			else
				newPos = new Point2D.Double(preBounds.getMaxX()
						+ pos.getX()
						- bounds.getX()
						- Math.min(preBounds.getWidth(), bounds.getWidth())
						* (rnd.nextDouble() * 0.15 + (rotateEnabled ? 0.20
								: 0.15)), pos.getY());
			v.setGlyphPosition(fi, newPos);
			prePos = newPos;
			preBounds = v.getGlyphVisualBounds(fi).getBounds2D();
		}
	}

	/**
	 * Does some post processing on the generated image if needed. Like rippling
	 * (waving) and blurring.
	 * 
	 * @param img
	 *            to be post prosessed.
	 * @return the finished image, maybe the same as the input
	 */
	protected BufferedImage postProcess(BufferedImage img) {
		if (rippleEnabled) {
			Rippler op = new Rippler(rnd.nextDouble() * 2 * Math.PI,
					(1 + rnd.nextDouble() * 3) * Math.PI,
					img.getHeight() / 10.0);
			img = op.filter(img, createImage());
		}
		if (blurEnabled) {
			float[] blurArray = new float[9];
			fillBlurArray(blurArray);
			ConvolveOp op = new ConvolveOp(new Kernel(3, 3, blurArray),
					ConvolveOp.EDGE_NO_OP, null);
			img = op.filter(img, createImage());
		}
		return img;
	}

	/**
	 * Generates a random probability distribution. Used by blurring.
	 * 
	 * @param array
	 *            filled with random values. The values in array sum up to 1.
	 */
	protected void fillBlurArray(float[] array) {
		float sum = 0;
		for (int fi = 0; fi < array.length; fi++)
			sum += array[fi] = rnd.nextFloat();
		for (int fi = 0; fi < array.length; fi++)
			array[fi] /= sum;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Color getBackground() {
		return background;
	}

	public Quality getQuality() {
		return quality;
	}

	public boolean isRippleEnabled() {
		return rippleEnabled;
	}

	public boolean isBlurEnabled() {
		return blurEnabled;
	}

	public boolean isOutlineEnabled() {
		return outlineEnabled;
	}

	public boolean isRotateEnabled() {
		return rotateEnabled;
	}
}