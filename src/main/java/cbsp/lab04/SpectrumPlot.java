package cbsp.lab04;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Prepared PNG plotting using only the JDK; works without a display or audio device. */
public final class SpectrumPlot {
    private static final Color INPUT = new Color(35, 99, 173);
    private static final Color OUTPUT = new Color(207, 83, 23);
    private static final int WIDTH = 1600, HEIGHT = 1000;
    private static final double MIN_DB = -120;
    private SpectrumPlot() { }

    /** Plot full bandwidth and a frequency detail using the same amplitude scale. */
    public static void write(Path path, String title, Spectrum before, String beforeLabel,
            Spectrum after, String afterLabel, double detailMinHz, double detailMaxHz) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(Color.DARK_GRAY);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            g.drawString(title, 100, 40);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            g.drawString("One-sided peak amplitude spectrum (raw signal, before listening attenuation)", 100, 68);
            legend(g, 100, beforeLabel, INPUT, false);
            legend(g, 700, afterLabel, OUTPUT, true);
            double maxDb = 0;
            for (Spectrum s : new Spectrum[]{before, after})
                for (double a : s.amplitude()) maxDb = Math.max(maxDb, Spectrum.db(a));
            maxDb = Math.ceil(maxDb/20)*20;
            double nyquist = before.frequencyHz()[before.frequencyHz().length-1];
            panel(g, before, after, 165, 320, 0, nyquist, maxDb, "Full bandwidth");
            panel(g, before, after, 585, 320, detailMinHz, Math.min(detailMaxHz, nyquist), maxDb,
                    "Frequency detail");
            g.setColor(Color.DARK_GRAY);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.drawString("Display floor: -120 dBFS. CSV retains values down to -240 dBFS. Dashed orange overlays solid blue.",
                    100, 980);
        } finally {
            g.dispose();
        }
        Files.createDirectories(path.toAbsolutePath().getParent());
        if (!ImageIO.write(image, "png", path.toFile())) throw new IOException("PNG writer unavailable.");
    }
    private static void legend(Graphics2D g, int x, String label, Color color, boolean dashed) {
        g.setColor(color);
        g.setStroke(stroke(dashed));
        g.drawLine(x, 105, x+50, 105);
        g.setColor(Color.DARK_GRAY);
        g.drawString(label, x+62, 111);
    }
    private static BasicStroke stroke(boolean dashed) {
        return dashed ? new BasicStroke(1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                10, new float[]{8, 5}, 0) : new BasicStroke(2.8f);
    }
    private static void panel(Graphics2D g, Spectrum before, Spectrum after, int top, int height,
            double minHz, double maxHz, double maxDb, String title) {
        int left = 100, width = 1440;
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g.setColor(Color.DARK_GRAY);
        g.drawString(title, left, top-16);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g.setStroke(new BasicStroke(1));
        for (int i = 0; i <= 8; i++) {
            int x = left+i*width/8;
            g.setColor(new Color(225, 230, 235));
            g.drawLine(x, top, x, top+height);
            g.setColor(Color.DARK_GRAY);
            String label = String.format(Locale.ROOT, "%.0f", minHz+(maxHz-minHz)*i/8);
            g.drawString(label, x-g.getFontMetrics().stringWidth(label)/2, top+height+24);
        }
        for (double db = MIN_DB; db <= maxDb; db += 20) {
            int y = top+(int)Math.round((maxDb-db)/(maxDb-MIN_DB)*height);
            g.setColor(new Color(225, 230, 235));
            g.drawLine(left, y, left+width, y);
            g.setColor(Color.DARK_GRAY);
            g.drawString(String.format(Locale.ROOT, "%.0f", db), left-45, y+5);
        }
        g.drawString("Frequency (Hz)", left+width/2-50, top+height+52);
        g.rotate(-Math.PI/2);
        g.drawString("Amplitude (dBFS)", -(top+height/2+55), 28);
        g.rotate(Math.PI/2);
        java.awt.Shape previousClip = g.getClip();
        g.clipRect(left, top, width, height);
        Spectrum[] spectra = {before, after};
        for (int series = 0; series < spectra.length; series++) {
            Spectrum s = spectra[series];
            Path2D line = new Path2D.Double();
            boolean started = false;
            for (int k = 0; k < s.frequencyHz().length; k++) {
                double hz = s.frequencyHz()[k];
                if (hz < minHz || hz > maxHz) continue;
                double x = left+(hz-minHz)/(maxHz-minHz)*width;
                double db = Math.max(MIN_DB, Spectrum.db(s.amplitude()[k]));
                double y = top+(maxDb-db)/(maxDb-MIN_DB)*height;
                if (!started) { line.moveTo(x, y); started = true; }
                else line.lineTo(x, y);
            }
            g.setColor(series == 0 ? INPUT : OUTPUT);
            g.setStroke(stroke(series == 1));
            g.draw(line);
        }
        g.setClip(previousClip);
        g.setColor(Color.GRAY);
        g.setStroke(new BasicStroke(1));
        g.drawRect(left, top, width, height);
    }
}
