package com.aaindia.prodocscanner.utils.pdf;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.IOException;

public class PDFRendererWhiteBG extends PDFRenderer {
    /**
     * Creates a new PDFRenderer.
     *
     * @param document the document to render
     */
    public PDFRendererWhiteBG(PDDocument document) {
        super(document);
    }


    @Override
    public Bitmap renderImage(int pageIndex, float scale, Bitmap.Config config) throws IOException {
        PDPage page = document.getPage(pageIndex);

        PDRectangle cropbBox = page.getCropBox();
        float widthPt = cropbBox.getWidth();
        float heightPt = cropbBox.getHeight();
        int widthPx = Math.round(widthPt * scale);
        int heightPx = Math.round(heightPt * scale);
        int rotationAngle = page.getRotation();

        // swap width and height
        Bitmap image;
        if (rotationAngle == 90 || rotationAngle == 270) {
            image = Bitmap.createBitmap(heightPx, widthPx, config);
        } else {
            image = Bitmap.createBitmap(widthPx, heightPx, config);
        }

        // use a transparent background if the imageType supports alpha
        Paint paint = new Paint();
        Canvas canvas = new Canvas(image);


        //paint.setColor(Color.WHITE);
        //paint.setStyle(Paint.Style.FILL);
        //canvas.drawRect(0, 0, image.getWidth(), image.getHeight(), paint);
        //paint.reset();


        renderPage(page, paint, canvas, image.getWidth(), image.getHeight(), scale, scale);

        return image;
    }
}
