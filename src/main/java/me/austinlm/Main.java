package me.austinlm;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;

public class Main {

    private static int failCount = 0;

    public static void main(String[] args) {
        String basePath = "/Users/austinmayes/Desktop/raw";
        // Loop through each file in the directory
        File folder = new File(basePath);
        File[] listOfFiles = folder.listFiles();
        System.out.println("Preparing to process " + listOfFiles.length + " files");
        for (File file : listOfFiles) {
            String nameWithoutExtension = file.getName().substring(0, file.getName().lastIndexOf('.'));
            fixImage(file.getAbsolutePath(), "/Users/austinmayes/Desktop/processed/" + nameWithoutExtension + ".png");
        }
        System.out.println("Processed " + (listOfFiles.length - failCount) + " files");
    }

    private static void fixImage(String sourceImagePath, String outputImagePath) {
        int outputWidth = 1920;
        int outputHeight = 1080;

        try {
            // Read the source image
            BufferedImage sourceImage = ImageIO.read(new File(sourceImagePath));

            // Read the image metadata
            Metadata metadata = ImageMetadataReader.readMetadata(new File(sourceImagePath));
            int orientation = getOrientation(metadata);

            // Rotate the image if necessary
            AffineTransform transform = transform(orientation, sourceImage.getWidth(), sourceImage.getHeight());
            AffineTransformOp affineTransformOp = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
            BufferedImage newSourceImage = affineTransformOp.createCompatibleDestImage(sourceImage, (sourceImage.getType() == BufferedImage.TYPE_BYTE_GRAY) ? sourceImage.getColorModel() : null);
            newSourceImage = affineTransformOp.filter(sourceImage, newSourceImage);

            // Determine the scale factor to fit the source image within the output dimensions + a 15% margin
            double scale = Math.min((double) outputWidth / newSourceImage.getWidth(),
                    (double) outputHeight / newSourceImage.getHeight()) * 0.85;

            // Calculate the scaled dimensions
            int scaledWidth = (int) (newSourceImage.getWidth() * scale);
            int scaledHeight = (int) (newSourceImage.getHeight() * scale);

            // Create a blank output image
            BufferedImage outputImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);

            // Calculate the position to center the scaled source image on the output image
            int x = (outputWidth - scaledWidth) / 2;
            int y = (outputHeight - scaledHeight) / 2;

            // Scale the source image to fit the output image
            Graphics2D g2d = outputImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            // Add a drop shadow effect to the output image
            g2d.setColor(Color.BLACK);
            g2d.setComposite(AlphaComposite.SrcOver.derive(0.5f));
            g2d.fillRect(x + 1, y + 1, scaledWidth, scaledHeight);
            g2d.setComposite(AlphaComposite.SrcOver);

            // Add the image to the output image
            g2d.drawImage(newSourceImage, x, y, scaledWidth, scaledHeight, null);

            g2d.dispose();

            // Write the output image to a PNG file
            ImageIO.write(outputImage, "png", new File(outputImagePath));

            System.out.println("Image scaling complete for " + sourceImagePath);
        } catch (Exception e) {
            System.out.println("Image scaling failed for " + sourceImagePath);
            e.printStackTrace();
            failCount++;
        }
    }

    private static int getOrientation(Metadata metadata) throws Exception {
        ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
            return directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        }
        return 0;
    }

    private static AffineTransform transform(int orientation, int width, int height) throws Exception {
        AffineTransform affineTransform = new AffineTransform();

        switch (orientation) {
            case 1:
                break;
            case 2: // Flip X
                affineTransform.scale(-1.0, 1.0);
                affineTransform.translate(-width, 0);
                break;
            case 3: // PI rotation
                affineTransform.translate(width, width);
                affineTransform.rotate(Math.PI);
                break;
            case 4: // Flip Y
                affineTransform.scale(1.0, -1.0);
                affineTransform.translate(0, -width);
                break;
            case 5: // - PI/2 and Flip X
                affineTransform.rotate(-Math.PI / 2);
                affineTransform.scale(-1.0, 1.0);
                break;
            case 6: // -PI/2 and -width
                affineTransform.translate(width, 0);
                affineTransform.rotate(Math.PI / 2);
                break;
            case 7: // PI/2 and Flip
                affineTransform.scale(-1.0, 1.0);
                affineTransform.translate(-width, 0);
                affineTransform.translate(0, width);
                affineTransform.rotate(3 * Math.PI / 2);
                break;
            case 8: // PI / 2
                affineTransform.translate(0, width);
                affineTransform.rotate(3 * Math.PI / 2);
                break;
            default:
                break;
        }
        return affineTransform;
    }
}
