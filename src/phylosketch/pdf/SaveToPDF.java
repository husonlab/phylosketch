/*
 * SaveToPDF.java Copyright (C) 2023. Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package phylosketch.pdf;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import jloda.fx.util.BasicFX;
import jloda.fx.util.GeometryUtilsFX;
import jloda.thirdparty.PngEncoderFX;
import jloda.util.Basic;
import jloda.util.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import static org.apache.pdfbox.pdmodel.common.PDRectangle.A4;

/**
 * save pane to PDF, trying to draw as objects
 * Daniel Huson, 6.2023
 */
public class SaveToPDF {
	public static void apply(Pane pane, File file) throws IOException {
		file.delete();

		var document = new PDDocument();
		var page = new PDPage(A4);
		document.addPage(page);

		var pdfMinX = page.getCropBox().getLowerLeftX();
		var pdfMaxX = page.getCropBox().getUpperRightX();
		var pdfWidth = page.getCropBox().getWidth();

		var pdfMinY = page.getCropBox().getLowerLeftY();
		var pdfMaxY = page.getCropBox().getUpperRightY();
		var pdfHeight = page.getCropBox().getHeight();

		var paneWidth = pane.getBoundsInLocal().getWidth();
		var paneHeight = pane.getBoundsInLocal().getHeight();

		var factor = Math.min(pdfWidth / paneWidth, pdfHeight / paneHeight);

		Function<Double, Float> ps = s -> (float) (s * factor);
		Function<Double, Float> px = x -> (float) (x * factor + pdfMinX);
		Function<Double, Float> py = y -> (float) (pdfMaxY - y * factor);

		var contentStream = new PDPageContentStream(document, page);

		for (var n : BasicFX.getAllRecursively(pane, n -> true)) {
			//System.err.println("n: "+n.getClass().getSimpleName());
			try {
				if (n instanceof Line line) {
					contentStream.setLineWidth(ps.apply(line.getStrokeWidth()));
					var x1 = px.apply(line.localToScene(line.getStartX(), line.getStartY()).getX());
					var y1 = py.apply(line.localToScene(line.getStartX(), line.getStartY()).getY());
					var x2 = px.apply(line.localToScene(line.getEndX(), line.getEndY()).getX());
					var y2 = py.apply(line.localToScene(line.getEndX(), line.getEndY()).getY());
					contentStream.moveTo(x1, y1);
					contentStream.lineTo(x2, y2);
					doFillStroke(contentStream, line.getStroke(), line.getFill());
				} else if (n instanceof Circle circle) {
					contentStream.setLineWidth(ps.apply(circle.getStrokeWidth()));
					var bounds = circle.localToScene(circle.getBoundsInLocal());
					var r = ps.apply(0.5 * bounds.getHeight());
					var x = px.apply(bounds.getCenterX());
					var y = py.apply(bounds.getCenterY());
					addCircle(contentStream, x, y, r);
					doFillStroke(contentStream, circle.getStroke(), circle.getFill());
				} else if (n instanceof QuadCurve || n instanceof CubicCurve) {
					var curve = (n instanceof QuadCurve ? convertQuadCurveToCubicCurve((QuadCurve) n) : (CubicCurve) n);
					contentStream.setLineWidth(ps.apply(curve.getStrokeWidth()));
					var sX = px.apply(curve.localToScene(curve.getStartX(), curve.getStartY()).getX());
					var sY = py.apply(curve.localToScene(curve.getStartX(), curve.getStartY()).getY());
					var c1X = px.apply(curve.localToScene(curve.getControlX1(), curve.getControlY1()).getX());
					var c1Y = py.apply(curve.localToScene(curve.getControlX1(), curve.getControlY1()).getY());
					var c2X = px.apply(curve.localToScene(curve.getControlX2(), curve.getControlY2()).getX());
					var c2Y = py.apply(curve.localToScene(curve.getControlX2(), curve.getControlY2()).getY());
					var tX = px.apply(curve.localToScene(curve.getEndX(), curve.getEndY()).getX());
					var tY = py.apply(curve.localToScene(curve.getEndX(), curve.getEndY()).getY());
					contentStream.moveTo(sX, sY);
					contentStream.curveTo(c1X, c1Y, c2X, c2Y, tX, tY);
					doFillStroke(contentStream, curve.getStroke(), curve.getFill());
				} else if (n instanceof Path path) {
					contentStream.setLineWidth(ps.apply(path.getStrokeWidth()));
					var local = new Point2D(0, 0);
					for (var element : path.getElements()) {
						if (element instanceof MoveTo moveTo) {
							local = new Point2D(moveTo.getX(), moveTo.getY());
							var t = path.localToScene(local.getX(), local.getY());
							contentStream.moveTo(px.apply(t.getX()), py.apply(t.getY()));
						} else if (element instanceof LineTo lineTo) {
							local = new Point2D(lineTo.getX(), lineTo.getY());
							var t = path.localToScene(local.getX(), local.getY());
							contentStream.lineTo(px.apply(t.getX()), py.apply(t.getY()));
						} else if (element instanceof HLineTo lineTo) {
							local = new Point2D(lineTo.getX(), local.getY());
							var t = path.localToScene(local.getX(), local.getY());
							contentStream.lineTo(px.apply(t.getX()), py.apply(t.getY()));
						} else if (element instanceof VLineTo lineTo) {
							local = new Point2D(local.getX(), lineTo.getY());
							var t = path.localToScene(local.getX(), local.getY());
							contentStream.lineTo(px.apply(t.getX()), py.apply(t.getY()));
						} else if (element instanceof ArcTo arcTo) {
							local = new Point2D(arcTo.getX(), arcTo.getY());
							System.err.println("arcTo: not implemented");
						} else if (element instanceof QuadCurveTo || element instanceof CubicCurveTo) {
							var curveTo = (element instanceof QuadCurveTo ? convertQuadToCubicCurveTo(local.getX(), local.getY(), (QuadCurveTo) element) : (CubicCurveTo) element);
							var t = path.localToScene(curveTo.getX(), curveTo.getY());
							var c1 = path.localToScene(curveTo.getControlX1(), curveTo.getControlY1());
							var c2 = path.localToScene(curveTo.getControlX2(), curveTo.getControlY2());
							contentStream.curveTo(px.apply(c1.getX()), py.apply(c1.getY()), px.apply(c2.getX()), py.apply(c2.getY()), px.apply(t.getX()), py.apply(t.getY()));
						}
					}
				} else if (n instanceof Polygon polygon) {
					contentStream.setLineWidth(ps.apply(polygon.getStrokeWidth()));
					var points = polygon.getPoints();
					if (points.size() > 0) {
						var sX = px.apply(polygon.localToScene(polygon.getPoints().get(0), polygon.getPoints().get(1)).getX());
						var sY = py.apply(polygon.localToScene(polygon.getPoints().get(0), polygon.getPoints().get(1)).getY());

						contentStream.moveTo(sX, sY);
						for (var i = 2; i < points.size(); i += 2) {
							var x = px.apply(polygon.localToScene(polygon.getPoints().get(i), polygon.getPoints().get(i + 1)).getX());
							var y = py.apply(polygon.localToScene(polygon.getPoints().get(i), polygon.getPoints().get(i + 1)).getY());
							contentStream.lineTo(x, y);
						}
						contentStream.closePath();
						doFillStroke(contentStream, polygon.getStroke(), polygon.getFill());
					}
				} else if (n instanceof Text text) {
					if (!text.getText().isBlank()) {
						double screenAngle = 360 - getAngleOnScreen(text); // because y axis points upward in PDF
						var localBounds = text.getBoundsInLocal();
						var origX = localBounds.getMinX();
						var origY = localBounds.getMinY() + 0.87f * localBounds.getHeight();
						var rotateAnchorX = text.localToScene(origX, origY).getX();
						var rotateAnchorY = text.localToScene(origX, origY).getY();
						contentStream.beginText();
						if (isMirrored(text)) // todo: this is untested:
							screenAngle = 360 - screenAngle;
						contentStream.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(screenAngle), px.apply(rotateAnchorX), py.apply(rotateAnchorY)));
						contentStream.setNonStrokingColor(pdfColor(text.getFill()));
						var fontHeight = ps.apply(0.87f * localBounds.getHeight());
						setFont(contentStream, text, fontHeight);
						contentStream.showText(text.getText());
						contentStream.endText();
					}
				} else if (n instanceof ImageView imageView) {
					var encoder = new PngEncoderFX(imageView.getImage());
					var image = PDImageXObject.createFromByteArray(document, encoder.pngEncode(false), "image/png");
					var bounds = imageView.localToScene(imageView.getBoundsInLocal());
					var x = px.apply(bounds.getMinX());
					var width = ps.apply(bounds.getWidth());
					var y = py.apply(bounds.getMaxY());
					var height = ps.apply(bounds.getHeight());
					contentStream.drawImage(image, x, y, width, height);
				} else if (n instanceof Shape3D shape) { // untested
					var snapShot = shape.snapshot(null, null);
					var encoder = new PngEncoderFX(snapShot);
					var image = PDImageXObject.createFromByteArray(document, encoder.pngEncode(false), "image/png");
					var bounds = shape.localToScene(shape.getBoundsInLocal());
					var x = px.apply(bounds.getMinX());
					var width = ps.apply(bounds.getWidth());
					var y = ps.apply(bounds.getMaxY());
					var height = ps.apply(bounds.getHeight());
					contentStream.drawImage(image, x, y, width, height);
				}
			} catch (IOException ex) {
				Basic.caught(ex);
			}
		}
		contentStream.close();
		document.save(file);
		document.close();
	}

	private static void setFont(PDPageContentStream contentStream, Text text, float size) throws IOException {
		contentStream.setFont(convertToPDFBoxFont(text.getFont()), size);
	}

	public static PDType1Font convertToPDFBoxFont(Font javafxFont) {
		var pdfboxFontFamily = "";
		{
			var fontFamily = javafxFont.getFamily().toLowerCase();
			if (fontFamily.startsWith("times") || fontFamily.startsWith("arial"))
				pdfboxFontFamily = Standard14Fonts.FontName.TIMES_ROMAN.getName();
			else if (fontFamily.startsWith("courier"))
				pdfboxFontFamily = Standard14Fonts.FontName.COURIER.getName();
			else if (fontFamily.startsWith("symbol"))
				pdfboxFontFamily = Standard14Fonts.FontName.SYMBOL.getName();
			else if (fontFamily.startsWith("zapf_dingbats"))
				pdfboxFontFamily = Standard14Fonts.FontName.ZAPF_DINGBATS.getName();
			else // if(fontFamily.startsWith("helvetica") || fontFamily.startsWith("system"))
				pdfboxFontFamily = Standard14Fonts.FontName.HELVETICA.getName();
		}

		// Map JavaFX font weight and style to PDFBox font
		var bold = javafxFont.getName().contains(" Bold");
		var italic = javafxFont.getName().contains(" Italic");
		var pdfboxFontStyle = "";
		if (bold) {
			if (italic)
				pdfboxFontStyle = "_BoldItalic";
			else
				pdfboxFontStyle = "_Bold";
		} else if (italic)
			pdfboxFontStyle = "_Italic";
		var pdfboxFontFullName = pdfboxFontFamily + pdfboxFontStyle;
		var font = StringUtils.valueOfIgnoreCase(Standard14Fonts.FontName.class, pdfboxFontFullName);
		if (font == null) {
			pdfboxFontFullName = pdfboxFontFullName.replaceAll("Italic", "Oblique");
			font = StringUtils.valueOfIgnoreCase(Standard14Fonts.FontName.class, pdfboxFontFullName);
		}
		if (font == null) {
			font = StringUtils.valueOfIgnoreCase(Standard14Fonts.FontName.class, pdfboxFontFamily);
		}
		if (font == null)
			font = Standard14Fonts.FontName.HELVETICA;
		return new PDType1Font(font);
	}

	private static void doFillStroke(PDPageContentStream contentStream, Paint stroke, Paint fill) throws IOException {
		var pdfStroke = pdfColor(stroke);
		var pdfFill = pdfColor(fill);
		if (pdfStroke != null && pdfFill != null) {
			contentStream.setStrokingColor(pdfStroke);
			contentStream.setNonStrokingColor(pdfFill);
			contentStream.fillAndStroke();
		} else if (pdfStroke != null) {
			contentStream.setStrokingColor(pdfStroke);
			contentStream.stroke();
		} else if (pdfFill != null) {
			contentStream.setNonStrokingColor(pdfFill);
			contentStream.fill();
		}
	}

	private static void addCircle(PDPageContentStream contentStream, float cx, float cy, float r) throws IOException {
		final float k = 0.552284749831f;
		//System.err.println("Circle at: " + cx + "," + cy);
		contentStream.moveTo(cx - r, cy);
		contentStream.curveTo(cx - r, cy + k * r, cx - k * r, cy + r, cx, cy + r);
		contentStream.curveTo(cx + k * r, cy + r, cx + r, cy + k * r, cx + r, cy);
		contentStream.curveTo(cx + r, cy - k * r, cx + k * r, cy - r, cx, cy - r);
		contentStream.curveTo(cx - k * r, cy - r, cx - r, cy - k * r, cx - r, cy);
	}

	private static PDColor pdfColor(Paint paint) {
		var color = (Color) paint;
		if (color == null || color.equals(Color.TRANSPARENT))
			return null;
		return new PDColor(new float[]{(float) color.getRed(), (float) color.getGreen(), (float) color.getBlue()}, PDDeviceRGB.INSTANCE);
	}

	/**
	 * gets the angle of a node on screen
	 *
	 * @param node the node
	 * @return angle in degrees
	 */
	private static double getAngleOnScreen(Node node) {
		var localOrig = new Point2D(0, 0);
		var localX1000 = new Point2D(1000, 0);
		var orig = node.localToScreen(localOrig);
		if (orig != null) {
			var x1000 = node.localToScreen(localX1000).subtract(orig);
			if (x1000 != null) {
				return GeometryUtilsFX.computeAngle(x1000);
			}
		}
		return 0.0;
	}

	/**
	 * does this pane appear as a mirrored image on the screen?
	 *
	 * @param node the pane
	 * @return true, if mirror image, false if direct image
	 */
	private static boolean isMirrored(Node node) {
		var orig = node.localToScreen(0, 0);
		if (orig != null) {
			var x1000 = node.localToScreen(1000, 0);
			var y1000 = node.localToScreen(0, 1000);
			var p1 = x1000.subtract(orig);
			var p2 = y1000.subtract(orig);
			var determinant = p1.getX() * p2.getY() - p1.getY() * p2.getX();
			return (determinant < 0);
		} else
			return false;
	}

	private static CubicCurve convertQuadCurveToCubicCurve(QuadCurve quadCurve) {
		var cubicCurve = new CubicCurve();
		cubicCurve.setStartX(quadCurve.getStartX());
		cubicCurve.setStartY(quadCurve.getStartY());
		cubicCurve.setEndX(quadCurve.getEndX());
		cubicCurve.setEndY(quadCurve.getEndY());
		var c1x = (2.0 / 3.0) * quadCurve.getControlX() + (1.0 / 3.0) * quadCurve.getStartX();
		var c1y = (2.0 / 3.0) * quadCurve.getControlY() + (1.0 / 3.0) * quadCurve.getStartY();
		var c2x = (2.0 / 3.0) * quadCurve.getControlX() + (1.0 / 3.0) * quadCurve.getEndX();
		var c2y = (2.0 / 3.0) * quadCurve.getControlY() + (1.0 / 3.0) * quadCurve.getEndY();
		cubicCurve.setControlX1(c1x);
		cubicCurve.setControlY1(c1y);
		cubicCurve.setControlX2(c2x);
		cubicCurve.setControlY2(c2y);
		return cubicCurve;
	}

	private static CubicCurveTo convertQuadToCubicCurveTo(double startX, double startY, QuadCurveTo quadCurveTo) {
		var cubicCurveTo = new CubicCurveTo();
		cubicCurveTo.setX(quadCurveTo.getX());
		cubicCurveTo.setY(quadCurveTo.getY());
		var c1x = (2.0 / 3.0) * quadCurveTo.getControlX() + (1.0 / 3.0) * startX;
		var c1y = (2.0 / 3.0) * quadCurveTo.getControlY() + (1.0 / 3.0) * startY;
		var c2x = (2.0 / 3.0) * quadCurveTo.getControlX() + (1.0 / 3.0) * quadCurveTo.getX();
		var c2y = (2.0 / 3.0) * quadCurveTo.getControlY() + (1.0 / 3.0) * quadCurveTo.getY();
		cubicCurveTo.setControlX1(c1x);
		cubicCurveTo.setControlY1(c1y);
		cubicCurveTo.setControlX2(c2x);
		cubicCurveTo.setControlY2(c2y);
		return cubicCurveTo;
	}
}
