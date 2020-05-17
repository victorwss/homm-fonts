import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;

/**
 * @author Victor Williams Stafusa da Silva
 */
public class FontWorker {

    private static final int TOTAL_CHARS = 256;
    private static final int TOTAL_CHARS_ROOT = 16;
    private static final int TOTAL_LINES_OFFSETS_FILE = 17;
    private static final int HEADER1_BYTES = 5;
    private static final int HEADER2_START = 9;
    private static final int INT_SIZE = 4;
    private static final int NUMBER_CHAR_ATTRIBUTES = 3;
    private static final int ATTRIBUTES_OFFSET = 32;
    private static final int OFFSETS_OFFSET = ATTRIBUTES_OFFSET + (NUMBER_CHAR_ATTRIBUTES * INT_SIZE * TOTAL_CHARS);
    private static final int IMAGE_OFFSET = OFFSETS_OFFSET + (INT_SIZE * TOTAL_CHARS);

    public static void main(String[] args) {
        if (args == null) throw new IllegalArgumentException();
        try {
            var parameters = parseParameters(args);
            var colors = new ColorSet(
                    Color.forName(parameters.get("bg")),
                    Color.forName(parameters.get("fg")),
                    Color.forName(parameters.get("shadow")),
                    Color.forName(parameters.get("outline")));
            var operation = parameters.get("op");
            var font = parameters.get("font");
            var image = parameters.get("image");
            var offsets = parameters.get("offsets");
            if (operation == null) throw new MalformedCommandLineParameterException("The operation was not specified.");
            operation = operation.toUpperCase(Locale.ROOT);
            if ("EXPORT".equals(operation)) {
                Font.exportFont(colors, font, image, offsets);
            } else if ("IMPORT".equals(operation)) {
                Font.importFont(colors, font, image, offsets);
            } else {
                throw new MalformedCommandLineParameterException("Invalid value for the op parameter.");
            }
        } catch (MalformedCommandLineParameterException e) {
            System.out.println(e.getMessage());
            System.out.print(USAGE);
        } catch (BadColorException | BadInputDataException | BadImageDataException | FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private static Map<String, String> parseParameters(String[] args) throws MalformedCommandLineParameterException {
        if (args == null) throw new IllegalArgumentException();
        var keys = List.of("op", "fg", "bg", "shadow", "outline", "font", "image", "offsets");
        Map<String, String> params = new HashMap<>(args.length);
        for (var arg : args) {
            var eq = arg.indexOf('=');
            if (eq == -1) throw new MalformedCommandLineParameterException("Malformed parameter " + arg + ".");
            var key = arg.substring(0, eq).toLowerCase(Locale.ROOT);
            if (key.isEmpty()) throw new MalformedCommandLineParameterException("Invalid nameless parameter.");
            if (!keys.contains(key)) throw new MalformedCommandLineParameterException("Unknown parameter " + key + ".");
            if (params.containsKey(key)) throw new MalformedCommandLineParameterException("Duplicate parameter " + key + ".");
            var value = arg.substring(eq + 1);
            if (value.isEmpty()) throw new MalformedCommandLineParameterException("No value for " + key + ".");
            params.put(key, value);
        }
        params.putIfAbsent("fg", "red");
        params.putIfAbsent("bg", "white");
        params.putIfAbsent("shadow", "black");
        params.putIfAbsent("outline", "blue");
        return params;
    }

    private static String listSuffixes() {
        var s = ImageIO.getWriterFileSuffixes();
        if (s.length == 0) return "<NONE>";
        if (s.length == 1) return s[0];
        var sb = new StringBuilder(64);
        sb.append(s[0]);
        for (var i = 1; i < s.length - 1; i++) {
            sb.append(", ");
            sb.append(s[i]);
        }
        sb.append(" and ");
        sb.append(s[s.length - 1]);
        return sb.toString();
    }

    public static final String USAGE = "\n"
            + "This program is a command line tool used for editing FNT files for \n"
            + "  Heroes of Might and Magic games.\n"
            + "\n"
            + "It might be used to either export a FNT file to an image file or to import them\n"
            + "  back to the FNT file.\n"
            + "\n"
            + "BASIC USAGE:\n"
            + "The usage format is in this form:\n"
            + "FontWorker param1=value1 param2=value2 param3=value3 ...\n"
            + "\n"
            + "The most common usage are those:\n"
            + "FontWorker op=export \"font=<path/to/file.fnt>\"\n"
            + "FontWorker op=import \"image=<path/to/file.png>\"\n"
            + "\n"
            + "An example of advanced usage would be:\n"
            + "FontWorker op=export font=file.fnt image=save.png offsets=data.txt bg=cyan\n"
            + "\n"
            + "PARAMETERS:\n"
            + "You might pass additional parameters in the form of name=value. The 'font'\n"
            + "  parameter is mandatory for the export command and the 'image' for the import\n"
            + "  command. The 'op' parameter is always mandatory and might be either 'import'\n"
            + "  or 'export'. If the parameter features spaces in its text, wrap it in double\n"
            + "  quotes like this:\n"
            + "  \"image=C:\\My Images\\image.png\"\n"
            + "  instead of that:\n"
            + "  image=C:\\My Images\\image.png\n"
            + "\n"
            + "The parameters are:\n"
            + "op: The operation. Should be either import or export. Mandatory.\n"
            + "font: Font file. Defaults to the image file name with FNT extension.\n"
            + "      Mandatory for the export operation.\n"
            + "image: Image file. Defaults to the font file name with PNG extension.\n"
            + "       Mandatory for the import operation.\n"
            + "offsets: Offsets file. Defaults to the image file name with TXT extension.\n"
            + "fg: Foreground color. Defaults to red.\n"
            + "bg: Background color. Defaults to white.\n"
            + "shadow: Text shadow color. Defaults to black.\n"
            + "outline: Glyph outline color. Defaults to blue.\n"
            + "\n"
            + "COLORS:\n"
            + "The recognized vlues for the color parameters are those:\n"
            + "  black, red, blue, yellow, green, magenta, white or cyan\n"
            + "You might also give for any of them a set of 6 hexadecimal digits representing\n"
            + "  a color in the RGB format.\n"
            + "\n"
            + "IMAGE FORMATS:\n"
            + "The default image format for export is PNG.\n"
            + "However, those are the supported formats:\n"
            + "  " + listSuffixes() + ".\n"
            + "\n"
            + "OFFSETS FILE FORMAT:\n"
            + "Each image file must have a companion offsets file. Its content must be " + TOTAL_LINES_OFFSETS_FILE + " text\n"
            + "  lines. The first line must contain the 5 bytes values of the file header data,\n"
            + "  written in decimal separated by spaces. The other " + TOTAL_CHARS_ROOT + " lines containing data for\n"
            + "  " + TOTAL_CHARS_ROOT + " glyphs each line (in the same disposition as the image), separated by a tab\n"
            + "  character. Each glyph's data has two numeric values separated by a single\n"
            + "  space followed by a description of the glyph. Those values are, respectively,\n"
            + "  the space before the glyph and the space after the glyph, where each glyph\n"
            + "  offset is represented by a single line.\n";

    private FontWorker() {
    }

    public static class MalformedCommandLineParameterException extends Exception {
        private static final long serialVersionUID = 1L;

        public MalformedCommandLineParameterException(String message) {
            super(message);
        }
    }

    public static class BadImageDataException extends Exception {
        private static final long serialVersionUID = 1L;

        public BadImageDataException(String message) {
            super(message);
        }
    }

    public static class BadInputDataException extends Exception {
        private static final long serialVersionUID = 1L;

        public BadInputDataException(String message) {
            super(message);
        }
    }

    public static class BadColorException extends Exception {
        private static final long serialVersionUID = 1L;

        public BadColorException(String message) {
            super(message);
        }
    }

    public static final class Font {
        private final Glyph[] glyphs;
        private final int glyphHeight;
        private final ImageOffsets offsets;

        private Font(Glyph[] glyphs, int glyphHeight, ImageOffsets offsets) {
            this.glyphHeight = glyphHeight;
            this.glyphs = glyphs;
            this.offsets = offsets;
        }

        public static Font read(ColorSet colors, BufferedImage in, String offsetData) throws BadImageDataException {
            if (colors == null || in == null || offsetData == null) throw new IllegalArgumentException();
            var w = in.getWidth();
            var h = in.getHeight();
            if (w % TOTAL_CHARS_ROOT != 1) {
                throw new BadImageDataException("The image has an incorrect width (" + w + " % " + TOTAL_CHARS_ROOT + " != 1).");
            }
            if (h % TOTAL_CHARS_ROOT != 1) {
                throw new BadImageDataException("The image has an incorrect height (" + h + " % " + TOTAL_CHARS_ROOT + " != 1).");
            }
            var offsets = ImageOffsets.parseOffsets(offsetData);
            var gw = (w - 1) / TOTAL_CHARS_ROOT;
            var gh = (h - 1) / TOTAL_CHARS_ROOT;
            var out = colors.getOutOfBounds().getRGB();
            for (var i = 0; i < (TOTAL_CHARS_ROOT + 1); i++) {
                for (var py = 0; py < h; py++) {
                    var px = i * gw;
                    if (in.getRGB(px, py) != out) {
                        throw new BadImageDataException("The image features unexpected data over outlines (see [" + px + ", " + py + "]).");
                    }
                }
                for (var px = 0; px < w; px++) {
                    var py = i * gh;
                    if (in.getRGB(px, py) != out) {
                        throw new BadImageDataException("The image features unexpected data over outlines (see [" + px + ", " + py + "]).");
                    }
                }
            }
            var glyphs = new Glyph[TOTAL_CHARS];
            for (int y = 0; y < TOTAL_CHARS_ROOT; y++) {
                for (var x = 0; x < TOTAL_CHARS_ROOT; x++) {
                    var idx = y * TOTAL_CHARS_ROOT + x;
                    var before = offsets.getSpacesBefore(idx);
                    var after = offsets.getSpacesAfter(idx);
                    glyphs[idx] = Glyph.read(colors, in, x * gw + 1, y * gh + 1, gw, gh - 1, before, after);
                }
            }
            return new Font(glyphs, gh - 1, offsets);
        }

        private static byte[] readBytes(int howMany, InputStream in, int position) throws BadInputDataException, IOException {
            if (in == null || howMany < 0) throw new IllegalArgumentException();
            var b = new byte[howMany];
            for (var i = 0; i < howMany; i++) {
                var a = in.read();
                checkPrematureEnd(a, position + i);
                b[i] = (byte) a;
            }
            return b;
        }

        private static void checkPrematureEnd(int read, int position) throws BadInputDataException {
            if (read == -1) throw new BadInputDataException("Premature end of stream at position " + position + ".");
        }

        private static int readInt(int position, InputStream in) throws BadInputDataException, IOException {
            if (in == null) throw new IllegalArgumentException();
            var a = in.read();
            checkPrematureEnd(position, a);
            var b = in.read();
            checkPrematureEnd(position + 1, b);
            var c = in.read();
            checkPrematureEnd(position + 2, c);
            var d = in.read();
            checkPrematureEnd(position + 3, c);
            return a | (b << 8) | (c << 16) | (d << 24);
        }

        private static void writeInt(int toWrite, OutputStream out) throws IOException {
            if (out == null) throw new IllegalArgumentException();
            out.write((byte) (toWrite & 0xFF));
            out.write((byte) ((toWrite >>> 8) & 0xFF));
            out.write((byte) ((toWrite >>> 16) & 0xFF));
            out.write((byte) ((toWrite >>> 24) & 0xFF));
        }

        public static Font read(String fileName) throws BadInputDataException, FileNotFoundException, IOException {
            if (fileName == null) throw new IllegalArgumentException();
            try (InputStream is = new FileInputStream(fileName)) {
                return Font.read(is);
            } catch (FileNotFoundException | NoSuchFileException e) {
                throw new FileNotFoundException("The file " + fileName + " was not found.");
            }
        }

        public static Font read(InputStream in) throws BadInputDataException, IOException {
            if (in == null) throw new IllegalArgumentException();
            var spaceBefore = new int[TOTAL_CHARS];
            var width = new int[TOTAL_CHARS];
            var spaceAfter = new int[TOTAL_CHARS];
            var offsets = new int[TOTAL_CHARS];
            var firstFive = new int[HEADER1_BYTES];
            for (var i = 0; i < HEADER1_BYTES; i++) {
                var a = in.read();
                checkPrematureEnd(a, i);
                firstFive[i] = a;
            }
            var h = readInt(HEADER1_BYTES, in);
            for (var i = HEADER2_START; i < ATTRIBUTES_OFFSET; i++) {
                var x = in.read();
                checkPrematureEnd(x, i);
                if (x != 0) throw new BadInputDataException("Bad stream header (byte " + i + " is " + x + ").");
            }
            for (var i = 0; i < TOTAL_CHARS; i++) {
                spaceBefore[i] = readInt(i * NUMBER_CHAR_ATTRIBUTES * INT_SIZE + ATTRIBUTES_OFFSET, in);
                width[i] = readInt(i * NUMBER_CHAR_ATTRIBUTES * INT_SIZE + ATTRIBUTES_OFFSET + INT_SIZE, in);
                spaceAfter[i] = readInt(i * NUMBER_CHAR_ATTRIBUTES * INT_SIZE + ATTRIBUTES_OFFSET + (2 * INT_SIZE), in);
            }
            for (var i = 0; i < TOTAL_CHARS; i++) {
                offsets[i] = readInt(OFFSETS_OFFSET + i * INT_SIZE, in);
            }
            var io = new ImageOffsets(firstFive, spaceBefore, spaceAfter);
            var currentOffset = 0;
            var glyphs = new Glyph[TOTAL_CHARS];
            for (var i = 0; i < TOTAL_CHARS; i++) {
                if (currentOffset != offsets[i]) throw new BadInputDataException("Bad offset data for glyph " + i + ".");
                var size = width[i] * h;
                var bytes = readBytes(size, in, currentOffset + IMAGE_OFFSET);
                glyphs[i] = new Glyph(bytes, width[i], spaceBefore[i], spaceAfter[i]);
                currentOffset += size;
            }
            if (in.read() != -1) throw new BadInputDataException("Unexpected data left at the stream beyond its expected end.");
            return new Font(glyphs, h, io);
        }

        public Glyph at(byte b) {
            return glyphs[b & 0xFF];
        }

        public int getGlyphHeight() {
            return glyphHeight;
        }

        public BufferedImage draw(ColorSet colors) {
            if (colors == null) throw new IllegalArgumentException();
            var maxW = 0;
            for (var g : glyphs) {
                maxW = Integer.max(g.getWidth(), maxW);
            }
            var out = colors.getOutOfBounds().getRGB();
            var iw = TOTAL_CHARS_ROOT * (maxW + 1) + 1;
            var gh = getGlyphHeight();
            var ih = TOTAL_CHARS_ROOT * (gh + 1) + 1;
            var image = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
            for (var y = 0; y < ih; y++) {
                for (var x = 0; x < iw; x++) {
                    image.setRGB(x, y, out);
                }
            }
            for (var gy = 0; gy < TOTAL_CHARS_ROOT; gy++) {
                for (var gx = 0; gx < TOTAL_CHARS_ROOT; gx++) {
                    glyphs[gy * TOTAL_CHARS_ROOT + gx].draw(colors, image, gx * (maxW + 1) + 1, gy * (gh + 1) + 1);
                }
            }
            return image;
        }

        public ImageOffsets getOffsets() {
            return this.offsets;
        }

        public void write(File out) throws IOException {
            try (var os = new FileOutputStream(out)) {
                write(os);
            }
        }

        public void write(OutputStream out) throws IOException {
            if (out == null) throw new IllegalArgumentException();
            for (var i = 0; i < HEADER1_BYTES; i++) {
                out.write(offsets.getHeaderByte(i));
            }
            writeInt(getGlyphHeight(), out);
            for (var i = HEADER2_START; i < ATTRIBUTES_OFFSET; i++) {
                out.write(0);
            }
            for (var i = 0; i < TOTAL_CHARS; i++) {
                Glyph g = glyphs[i];
                writeInt(g.getSpaceBefore(), out);
                writeInt(g.getWidth(), out);
                writeInt(g.getSpaceAfter(), out);
            }
            var offset = 0;
            for (var i = 0; i < TOTAL_CHARS; i++) {
                Glyph g = glyphs[i];
                writeInt(offset, out);
                offset += g.getSize();
            }
            for (var i = 0; i < TOTAL_CHARS; i++) {
                glyphs[i].writePixels(out);
            }
        }

        public static void exportFont(ColorSet colors, String fontFile, String imageFile, String offsetsFile)
                throws FileNotFoundException, IOException, BadInputDataException
        {
            if (colors == null) throw new IllegalArgumentException();
            if (fontFile == null) throw new FileNotFoundException("The font file name is mandatory for the export operation.");
            var imageFileOk = imageFile;
            if (imageFile == null) {
                var dot1 = fontFile.lastIndexOf('.');
                imageFileOk = (dot1 == -1 ? fontFile : fontFile.substring(0, dot1)) + ".png";
            }
            var dot = imageFileOk.lastIndexOf('.');
            if (dot == -1) throw new BadInputDataException("Unrecognized image type. The image file name has no extension.");
            var offsetsFileOk = offsetsFile;
            if (offsetsFile == null) {
                var dot2 = imageFileOk.lastIndexOf('.');
                offsetsFileOk = (dot2 == -1 ? imageFileOk : imageFileOk.substring(0, dot2)) + ".txt";
            }
            var f = Font.read(fontFile);
            var image = f.draw(colors);
            var type = imageFileOk.substring(dot + 1);
            if (!ImageIO.write(image, type, new File(imageFileOk))) {
                throw new BadInputDataException("Unrecognized image type " + type + ".");
            }
            Files.writeString(new File(offsetsFileOk).toPath(), f.getOffsets().toString(), StandardCharsets.ISO_8859_1);
        }

        public static void importFont(ColorSet colors, String fontFile, String imageFile, String offsetsFile)
                throws FileNotFoundException, IOException, BadImageDataException
        {
            if (colors == null) throw new IllegalArgumentException();
            if (imageFile == null) throw new FileNotFoundException("The image file name is mandatory for the import operation.");
            var fontFileOk = fontFile;
            if (fontFileOk == null) {
                var dot = imageFile.lastIndexOf('.');
                fontFileOk = (dot == -1 ? imageFile : imageFile.substring(0, dot)) + ".fnt";
            }
            var offsetsFileOk = offsetsFile;
            if (offsetsFileOk == null) {
                var dot = imageFile.lastIndexOf('.');
                offsetsFileOk = (dot == -1 ? imageFile : imageFile.substring(0, dot)) + ".txt";
            }
            BufferedImage image;
            try {
                image = ImageIO.read(new File(imageFile));
            } catch (IIOException e) {
                if ("Can't read input file!".equals(e.getMessage())) {
                    throw new FileNotFoundException("The file " + imageFile + " was not found.");
                }
                throw e;
            } catch (FileNotFoundException | NoSuchFileException e) {
                throw new FileNotFoundException("The file " + imageFile + " was not found.");
            }
            String offsets;
            try {
                offsets = Files.readString(new File(offsetsFileOk).toPath(), StandardCharsets.ISO_8859_1);
            } catch (FileNotFoundException | NoSuchFileException e) {
                throw new FileNotFoundException("The file " + offsetsFileOk + " was not found.");
            }
            var f = Font.read(colors, image, offsets);
            f.write(new File(fontFileOk));
        }
    }

    public static final class ImageOffsets {

        private static final String[] CHAR_NAMES = new String[TOTAL_CHARS];
        static {
            String hex = "0123456789ABCDEF";
            for (var i = 0; i < 32; i++) {
                CHAR_NAMES[i] = "H-" + (hex.charAt(i / 16)) + (hex.charAt(i % 16));
            }
            for (var i = 32; i < TOTAL_CHARS; i++) {
                CHAR_NAMES[i] = new String(new byte[] {(byte) i}, StandardCharsets.ISO_8859_1);
            }
            for (var i = 127; i < TOTAL_CHARS; i++) {
                CHAR_NAMES[i] = "H-" + (hex.charAt(i / 16)) + (hex.charAt(i % 16)) + " (" + CHAR_NAMES[i] + ")";
            }
            CHAR_NAMES[0] = "H-00 NULL";
            CHAR_NAMES[7] = "H-07 BELL";
            CHAR_NAMES[8] = "H-08 BACKSPACE";
            CHAR_NAMES[9] = "H-0A TAB";
            CHAR_NAMES[10] = "H-0A LINE FEED";
            CHAR_NAMES[11] = "H-0B VERTICAL TAB";
            CHAR_NAMES[12] = "H-0C FORM FEED";
            CHAR_NAMES[13] = "H-0D CARRIAGE RETURN";
            CHAR_NAMES[27] = "H-1B ESCAPE";
            CHAR_NAMES[32] = "SPACE";
            CHAR_NAMES[160] = "H-A0 NBSP";
        }

        // Although the values are bytes, we use int to avoid some casts and conversions caused by the fact that in java, the
        // byte type has a range from -128 to +127 instead of 0 to 255.
        private final int[] fiveBytes;
        private final int[] spacesBefore;
        private final int[] spacesAfter;

        public ImageOffsets(int[] fiveBytes, int[] spacesBefore, int[] spacesAfter) {
            if (fiveBytes == null || spacesBefore == null || spacesAfter == null
                    || fiveBytes.length != HEADER1_BYTES || spacesBefore.length != TOTAL_CHARS || spacesAfter.length != TOTAL_CHARS)
            {
                throw new IllegalArgumentException();
            }
            this.fiveBytes = fiveBytes;
            this.spacesBefore = spacesBefore.clone();
            this.spacesAfter = spacesAfter.clone();
        }

        public static ImageOffsets parseOffsets(String data) throws BadImageDataException {
            if (data == null) throw new IllegalArgumentException();
            var parts = data.replace("\r", "").split("\n");
            if (parts.length != TOTAL_LINES_OFFSETS_FILE) {
                throw new BadImageDataException("The image offsets file does not have " + TOTAL_LINES_OFFSETS_FILE + " lines.");
            }
            var firstBytes = parts[0].split(" ");
            if (firstBytes.length != HEADER1_BYTES) throw new BadImageDataException("The first five bytes data must have 5 values.");
            var fiveBytes = new int[HEADER1_BYTES];
            for (var i = 0; i < HEADER1_BYTES; i++) {
                try {
                    var j = Integer.parseInt(firstBytes[i]);
                    if (j < 0 || j > 255) throw new BadImageDataException("The first five bytes must have values between 0 and 255.");
                    fiveBytes[i] = j;
                } catch (NumberFormatException e) {
                    throw new BadImageDataException("The first five bytes must have values between 0 and 255.");
                }
            }
            var befores = new int[TOTAL_CHARS];
            var afters = new int[TOTAL_CHARS];
            for (var i = 0; i < TOTAL_CHARS_ROOT; i++) {
                var subParts = parts[i + 1].split("\t");
                if (subParts.length != TOTAL_CHARS_ROOT) {
                    throw new BadImageDataException("The line " + (i + 1) + " in the image offsets file does not have " + TOTAL_CHARS_ROOT + " columns.");
                }
                for (var j = 0; j < TOTAL_CHARS_ROOT; j++) {
                    var subPart = subParts[j];
                    var c = i * TOTAL_CHARS_ROOT + j;
                    var idx1 = subPart.indexOf(' ');
                    if (idx1 == -1) throw new BadImageDataException("The image offsets for the character #" + c + " are malformed.");
                    var idx2 = subPart.indexOf(' ', idx1 + 1);
                    if (idx2 == -1) throw new BadImageDataException("The image offsets for the character #" + c + " are malformed.");
                    try {
                        befores[c] = Integer.parseInt(subPart.substring(0, idx1));
                        afters[c] = Integer.parseInt(subPart.substring(idx1 + 1, idx2));
                    } catch (NumberFormatException e) {
                        throw new BadImageDataException("The image offsets for the character #" + c + " features unreadable values.");
                    }
                }
            }
            return new ImageOffsets(fiveBytes, befores, afters);
        }

        public int getHeaderByte(int key) {
            if (key < 0 || key > 4) throw new IllegalArgumentException();
            return fiveBytes[key];
        }

        public int getSpacesAfter(int key) {
            if (key < 0 || key >= TOTAL_CHARS) throw new IllegalArgumentException();
            return spacesAfter[key];
        }

        public int getSpacesBefore(int key) {
            if (key < 0 || key >= TOTAL_CHARS) throw new IllegalArgumentException();
            return spacesBefore[key];
        }

        @Override
        public String toString() {
            var b = new StringBuilder(4000);
            for (var i = 0; i < 5; i++) {
                if (i != 0) b.append(" ");
                b.append(fiveBytes[i]);
            }
            b.append("\n");
            for (var i = 0; i < TOTAL_CHARS_ROOT; i++) {
                if (i != 0) b.append("\n");
                for (var j = 0; j < TOTAL_CHARS_ROOT; j++) {
                    if (j != 0) b.append("\t");
                    var idx = i * TOTAL_CHARS_ROOT + j;
                    b.append(spacesBefore[idx]);
                    b.append(' ');
                    b.append(spacesAfter[idx]);
                    b.append(' ');
                    b.append(charName(idx));
                }
            }
            return b.toString();
        }

        public static String charName(int b) {
            return CHAR_NAMES[b];
        }
    }

    public static final class Glyph {
        private final byte[] pixels;
        private final int width;
        private final int height;
        private final int spaceBefore;
        private final int spaceAfter;

        public Glyph(byte[] pixels, int width, int spaceBefore, int spaceAfter) {
            if (pixels == null || width < 0) throw new IllegalArgumentException();
            if (width == 0 ? pixels.length != 0 : pixels.length == 0 || pixels.length % width != 0) {
                throw new IllegalArgumentException();
            }
            for (var b : pixels) {
                if (b != -1 && b != 0 && b != 1) throw new IllegalArgumentException();
            }
            this.pixels = pixels.clone();
            this.width = width;
            this.height = width == 0 ? 0 : pixels.length / width;
            this.spaceBefore = spaceBefore;
            this.spaceAfter = spaceAfter;
        }

        public static Glyph read(
                ColorSet colors,
                BufferedImage in,
                int sx,
                int sy,
                int maxWidth,
                int height,
                int spaceBefore,
                int spaceAfter)
                throws BadImageDataException
        {
            if (colors == null || in == null || sx < 0 || sy < 0 || maxWidth < 0 || height < 0) throw new IllegalArgumentException();
            if (sx + maxWidth > in.getWidth() || sy + height > in.getHeight()) throw new IllegalArgumentException();
            var background = colors.getBackground().getRGB();
            var foreground = colors.getForeground().getRGB();
            var shadow = colors.getShadow().getRGB();
            var out = colors.getOutOfBounds().getRGB();
            int w;
            for (w = maxWidth - 1; w >= 0; w--) {
                if (in.getRGB(sx + w, sy) != out) break;
                for (var j = 1; j < height; j++) {
                    var px = sx + w;
                    var py = sy + j;
                    if (in.getRGB(px, py) != out) {
                        throw new BadImageDataException(
                                "The image features irregularly-shaped or misaligned glyphs (see [" + px + ", " + py + "]).");
                    }
                }
            }
            w++;
            var pixels = new byte[w * height];
            for (var j = 0; j < height; j++) {
                for (var i = 0; i < w; i++) {
                    var px = sx + i;
                    var py = sy + j;
                    var d = in.getRGB(px, py);
                    if (d == out) {
                        throw new BadImageDataException(
                                "The image features irregularly-shaped or misaligned glyphs (see [" + px + ", " + py + "]).");
                    }
                    byte b;
                    if (d == background) {
                        b = (byte) 0;
                    } else if (d == foreground) {
                        b = (byte) -1;
                    } else if (d == shadow) {
                        b = (byte) 1;
                    } else {
                        var color = Color.fromRGB(d).toString();
                        throw new BadImageDataException(
                                "The image features unrecognized colors (see " + color + " at [" + px + ", " + py + "]).");
                    }
                    pixels[j * w + i] = b;
                }
            }
            return new Glyph(pixels, w, spaceBefore, spaceAfter);
        }

        public byte getPixel(int x, int y) {
            if (x < 0 || y < 0 || x >= width || y >= height) throw new IllegalArgumentException();
            return pixels[y * width + x];
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getSpaceBefore() {
            return spaceBefore;
        }

        public int getSpaceAfter() {
            return spaceAfter;
        }

        public int getSize() {
            return pixels.length;
        }

        public void draw(ColorSet colors, BufferedImage target, int targetX, int targetY) {
            var h = height;
            var w = width;
            if (w == 0) return;
            if (colors == null
                    || target == null
                    || targetX < 0
                    || targetY < 0
                    || targetX > target.getWidth() - w
                    || targetY > target.getHeight() - h)
            {
                throw new IllegalArgumentException();
            }
            for (var y = 0; y < h; y++) {
                for (var x = 0; x < w; x++) {
                    target.setRGB(targetX + x, targetY + y, colors.rgb(getPixel(x, y)));
                }
            }
        }

        public void writePixels(OutputStream out) throws IOException {
            if (out == null) throw new IllegalArgumentException();
            out.write(pixels);
        }
    }

    public static final class ColorSet {
        private final Color background;
        private final Color foreground;
        private final Color shadow;
        private final Color outOfBounds;
        private final int[] rgbs;

        public ColorSet(Color background, Color foreground, Color shadow, Color outOfBounds) {
            if (background == null || foreground == null || shadow == null || outOfBounds == null) throw new IllegalArgumentException();
            this.background = background;
            this.foreground = foreground;
            this.shadow = shadow;
            this.outOfBounds = outOfBounds;
            this.rgbs = new int[] { foreground.getRGB(), background.getRGB(), shadow.getRGB() };
        }

        public Color getBackground() {
            return background;
        }

        public Color getForeground() {
            return foreground;
        }

        public Color getShadow() {
            return shadow;
        }

        public Color getOutOfBounds() {
            return outOfBounds;
        }

        public int rgb(byte value) {
            try {
                return rgbs[value + 1];
            } catch (IndexOutOfBoundsException e) {
                throw new IllegalArgumentException();
            }
        }
    }

    public static final class Color {
        private static final Map<String, Color> COLORS = Map.ofEntries(
            Map.entry("red", new Color(255, 0, 0)),
            Map.entry("green", new Color(0, 255, 0)),
            Map.entry("blue", new Color(0, 0, 255)),
            Map.entry("yellow", new Color(255, 255, 0)),
            Map.entry("magenta", new Color(255, 0, 255)),
            Map.entry("cyan", new Color(0, 255, 255)),
            Map.entry("black", new Color(0, 0, 0)),
            Map.entry("white", new Color(255, 255, 255))
        );

        private final int red;
        private final int green;
        private final int blue;

        public Color(int red, int green, int blue) {
            if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) throw new IllegalArgumentException();
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public static Color forName(String name) throws BadColorException {
            if (name == null) throw new IllegalArgumentException();
            var found = COLORS.get(name.toLowerCase(Locale.ROOT));
            if (found != null) return found;
            if (name.length() == 6) {
                var hex = "0123456789ABCDEF";
                var a = hex.indexOf(name.charAt(0));
                var b = hex.indexOf(name.charAt(1));
                var c = hex.indexOf(name.charAt(2));
                var d = hex.indexOf(name.charAt(3));
                var e = hex.indexOf(name.charAt(4));
                var f = hex.indexOf(name.charAt(5));
                if (a != -1 && b != -1 && c != -1 && d != -1 && e != -1 && f != -1) {
                    return fromRGB((a << 20) | (b << 16) | (c << 12) | (d << 8) | (e << 4) | f);
                }
            }
            throw new BadColorException("No color called " + name + " is understood by this program.");
        }

        public static Color fromRGB(int code) {
            return new Color((code >>> 16) & 0xFF, (code >>> 8) & 0xFF, code & 0xFF);
        }

        public int getRed() {
            return red;
        }

        public int getGreen() {
            return green;
        }

        public int getBlue() {
            return blue;
        }

        public int getRGB() {
            return (0xFF << 24) | (red << 16) | (green << 8) | blue;
        }

        @Override
        public int hashCode() {
            return getRGB();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Color && getRGB() == ((Color) other).getRGB();
        }

        @Override
        public String toString() {
            for (var e : COLORS.entrySet()) {
                if (e.getValue().equals(this)) return e.getKey();
            }
            return "RGB(" + red + ", " + green + ", " + blue + ")";
        }
    }
}