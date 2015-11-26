package cn.lambdalib.util.client.font;

import cn.lambdalib.util.helper.Color;

/**
 * A generic font interface.
 */
public interface IFont {

	enum FontAlign { LEFT, CENTER, RIGHT }

	class Extent {
		public int linesDrawn;
		public double width;
		public double height;

		public Extent(int _lines, double _width, double _height) {
			linesDrawn = _lines;
			width = _width;
			height = _height;
		}
	}

	class FontOption {

		public boolean bold, stroke, italic;
		public double fontSize = 10;
		public FontAlign align;
		public Color color;

		public FontOption() {
			this(10);
		}

		public FontOption(double _fontsz) {
			this(_fontsz, FontAlign.LEFT);
		}

		public FontOption(double _fontsz, Color _color) {
			this(_fontsz, FontAlign.LEFT, _color);
		}

		public FontOption(double _fontsz, FontAlign _align) {
			this(_fontsz, _align, Color.WHITE());
		}

		public FontOption(double _fontsz, FontAlign _align, Color _color) {
			fontSize = _fontsz;
			align = _align;
			color = _color;
		}

		@Override
		public FontOption clone() {
			FontOption ret = new FontOption();
			ret.bold = bold;
			ret.stroke = stroke;
			ret.fontSize = fontSize;
			ret.align = align;
			return ret;
		}

	}

	/**
	 * Draws the string at the given position with given font option in one line. <br>
	 *
	 * The string is assumed to not include line-seperate characters. (\n or \r). Violating this yields undefined
	 * 	behaviour.
	 */
	void draw(String str, double x, double y, FontOption option);

	/**
	 * Get the width of given character when drawed with given FontOption.
	 */
	void getCharWidth(char chr, FontOption option);

	/**
	 * Get the text width that will be drawn if calls the {@link IFont#draw}.
	 * @param str
	 * @param option
	 * @return
	 */
	double getTextWidth(String str, FontOption option);

	/**
	 * Draws a line-seperated string at the given position.
	 */
	default void drawSeperated(String str, double x, double y, double limit, FontOption option) {
		// TODO
	}

	/**
	 * Simulates the {@link IFont#drawSeperated} and return the extent drawn.
	 * @return A {@link Extent} describing the drawn area
	 */
	default Extent drawSeperated_Sim(String str, double x, double y, double limit, FontOption option) {
		// TODO
		return null;
	}

}
