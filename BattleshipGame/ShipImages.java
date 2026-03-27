import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * PNG names end in 1…N = segment order along the hull: first cell is 1, then 2, …
 * Vertical on the grid = top→bottom. Horizontal = left→right after rotating vertical art.
 */
final class ShipImages {

    private static final Map<String, BufferedImage> rawCache = new HashMap<>();
    private static final Map<String, BufferedImage> orientedCache = new HashMap<>();

    private ShipImages() {
    }

    static void paintSegment(Graphics g, Model model, int row, int col, int cellW, int cellH) {
        Model.ShipType t = model.yourCell(row, col);
        if (t == Model.ShipType.EMPTY) {
            return;
        }
        Ship ship = model.getShipCovering(row, col);
        if (ship == null) {
            return;
        }
        int seg = model.segmentIndexAt(row, col);
        int len = ship.xy_coords.size();
        if (seg < 0 || seg >= len) {
            seg = 0;
        }
        // segmentIndexAt: 0 = first cell of the ship (min col if horizontal, min row if vertical).
        // That matches filename order: Carrier1.png, Carrier2.png, … along the hull.
        int pieceNumber = seg + 1;
        BufferedImage img = getOrientedPiece(t, pieceNumber, ship.horizontal);
        if (img == null) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(2, 2, cellW - 4, cellH - 4);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Reference ShipSquare: drawImage at +3 with size cell-5 on each axis (fits grid cell).
        int inset = 3;
        int dw = Math.max(1, cellW - 5);
        int dh = Math.max(1, cellH - 5);
        g2.drawImage(img, inset, inset, inset + dw, inset + dh, 0, 0, img.getWidth(), img.getHeight(), null);
        g2.dispose();
    }

    private static BufferedImage getOrientedPiece(Model.ShipType type, int pieceNumber, boolean horizontal) {
        String key = type.name() + pieceNumber + horizontal;
        BufferedImage cached = orientedCache.get(key);
        if (cached != null) {
            return cached;
        }
        BufferedImage raw = loadRaw(type, pieceNumber);
        if (raw == null) {
            return null;
        }
        // PNGs are drawn for a vertical hull. Vertical on the grid = no spin. Horizontal = −90°
        // (same direction as the old reference used to lay horizontal strips vertically).
        BufferedImage use = horizontal ? rotateMinus90(raw) : raw;
        orientedCache.put(key, use);
        return use;
    }

    /** Same −90° pivot as Battleship/Model.rotateImage — pairs with vertically authored tiles. */
    private static BufferedImage rotateMinus90(BufferedImage originalImage) {
        double rotationRequired = Math.toRadians(-90);
        double locationX = originalImage.getWidth() / 2;
        double locationY = originalImage.getHeight() / 2;
        AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        return op.filter(originalImage, null);
    }

    private static BufferedImage loadRaw(Model.ShipType type, int pieceNumber) {
        String base = filePrefix(type);
        if (base == null) {
            return null;
        }
        String filename = base + pieceNumber + ".png";
        BufferedImage cached = rawCache.get(filename);
        if (cached != null) {
            return cached;
        }
        // Classpath (works from any cwd when BattleshipGame is on the class path and images/ is beside .class files)
        String resourcePath = "images/" + filename;
        try (InputStream in = openImageStream(resourcePath)) {
            if (in != null) {
                BufferedImage img = ImageIO.read(in);
                if (img != null) {
                    rawCache.put(filename, img);
                    return img;
                }
            }
        } catch (IOException ignored) {
            // try file paths
        }

        BufferedImage fromCodeBase = loadFromClassLocation(filename);
        if (fromCodeBase != null) {
            rawCache.put(filename, fromCodeBase);
            return fromCodeBase;
        }

        for (String dir : new String[] {
                "images",
                "BattleshipGame/images",
                "Battleship/images",
                "../BattleshipGame/images",
                "../Battleship/images"
        }) {
            File f = new File(dir, filename);
            if (f.isFile()) {
                try {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) {
                        rawCache.put(filename, img);
                        return img;
                    }
                } catch (IOException ignored) {
                    // try next path
                }
            }
        }
        return null;
    }

    private static InputStream openImageStream(String resourcePath) {
        InputStream in = ShipImages.class.getResourceAsStream(resourcePath);
        if (in != null) {
            return in;
        }
        ClassLoader cl = ShipImages.class.getClassLoader();
        if (cl != null) {
            in = cl.getResourceAsStream(resourcePath);
            if (in != null) {
                return in;
            }
        }
        in = ClassLoader.getSystemResourceAsStream(resourcePath);
        if (in != null) {
            return in;
        }
        return Thread.currentThread().getContextClassLoader() != null
                ? Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)
                : null;
    }

    /** When classes run from an exploded directory (IDE / javac), images often sit in ./images next to .class files. */
    private static BufferedImage loadFromClassLocation(String filename) {
        try {
            java.security.ProtectionDomain pd = ShipImages.class.getProtectionDomain();
            if (pd == null || pd.getCodeSource() == null) {
                return null;
            }
            URL loc = pd.getCodeSource().getLocation();
            if (loc == null) {
                return null;
            }
            File base = new File(loc.toURI());
            if (!base.isDirectory()) {
                return null;
            }
            File f = new File(base, "images/" + filename);
            if (f.isFile()) {
                return ImageIO.read(f);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String filePrefix(Model.ShipType type) {
        switch (type) {
            case CARRIER:
                return "Carrier";
            case BATTLESHIP:
                return "Battleship";
            case CRUISER:
                return "Cruiser";
            case SUBMARINE:
                return "Submarine";
            case DESTROYER:
                return "Destroyer";
            default:
                return null;
        }
    }
}
