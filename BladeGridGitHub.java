import ij.plugin.*;
import java.util.*;
import ij.*;
import java.awt.geom.*;
import ij.measure.*;
import ij.gui.*;
import java.awt.*;
import java.sql.*;

public class BladeGrid3_ implements PlugIn, DialogListener
{
    private static double crossSize;
    private static String[] colors;
    private static String color;
    private static final int LINES = 0;
    private static final int HLINES = 1;
    private static final int CROSSES = 2;
    private static final int POINTS = 3;
    private static final int NONE = 4;
    private static String[] types;
    private static String type;
    private static double areaPerPoint;
    private static double BladeLenth;
    private static boolean randomOffset;
    private static boolean bold;
    private Random random;
    private ImagePlus imp;
    private double tileWidth;
    private double tileHeight;
    private int xstart;
    private int ystart;
    private int linesV;
    private int linesH;
    private double pixelWidth;
    private double pixelHeight;
    private String units;
    private int gridWidth;
    
    //mySQL setup
    private final static String driver  = "com.mysql.jdbc.Driver";
    private String database  = "database_name";
    private String dbHost  = "database_IP";
    private String dbTable = "Table_name";
    private String dbUser="user_name";
    private String dbPass="user_pass";
    private String dbUrl="jdbc:mysql://"+ dbHost+ "/" + database;
    
    private boolean connected=false;
    
    Statement stmt = null;
	ResultSet rs = null;
    
    // the DataBase Connection
    private Connection con;
    
    public void ConnStart(String dbName) {
        try {
            //Class.forName("org.gjt.mm.mysql.Driver");
            Class.forName(driver);
        } catch(Exception ex) {
            IJ.log("Can't find Database driver class: " + ex);
            return;
        }
        try {
            con = (Connection) DriverManager.getConnection(dbUrl, dbUser, dbPass);
            //IJ.log("connected,,,,,,");
            IJ.log("Connected to " + dbUrl);
            connected=true;
            stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM blade_length");
			// or alternatively, if you don't know ahead of time that
			// the query will be a SELECT...
			if (stmt.execute("SELECT * FROM blade_length")) {
				rs = stmt.getResultSet();
			}
            
            
        } catch(SQLException ex) {
            IJ.log("SQLException: " + ex);
        }
        //IJ.log("connected,,,,,,d");
    }
    
    public void ConnDestroy() {
        if (connected) {
            try {
                con.close();
                IJ.log("Disconnected from database");
            } catch(SQLException ex) {
                IJ.log("SQLException: " + ex);
            }
        }
    }
    
    public BladeGrid3_() {
        this.random = new Random(System.currentTimeMillis());
        this.pixelWidth = 1.0;
        this.pixelHeight = 1.0;
        this.units = "pixels";
    }
    
    public void run(final String s) {
        this.imp = IJ.getImage();
        ConnStart(database);
        IJ.log("connected???");
        
        //sql test
        Statement stmt = null;
		ResultSet rs = null;
		 try {
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM blade_length");
			// or alternatively, if you don't know ahead of time that
			// the query will be a SELECT...
			if (stmt.execute("SELECT * FROM blade_length")) {
				rs = stmt.getResultSet();
			}
			// Now do something with the ResultSet ....
			System.out.println(rs);
			IJ.log("did somethint");
		}
		catch (SQLException ex){
			// handle any errors
			IJ.log("SQLException: " + ex.getMessage());
			IJ.log("SQLState: " + ex.getSQLState());
			IJ.log("VendorError: " + ex.getErrorCode());
		}
        
        
        ConnDestroy();
        this.showDialog();
    }
    
    void drawPoints() {
        final int n = 1;
        final int n2 = 2;
        final GeneralPath generalPath = new GeneralPath();
        for (int i = 0; i < this.linesV; ++i) {
            for (int j = 0; j < this.linesH; ++j) {
                final float n3 = (float)(this.xstart + i * this.tileWidth);
                final float n4 = (float)(this.ystart + j * this.tileHeight);
                generalPath.moveTo(n3 - n2, n4 - n);
                generalPath.lineTo(n3 - n2, n4 + n);
                generalPath.moveTo(n3 + n2, n4 - n);
                generalPath.lineTo(n3 + n2, n4 + n);
                generalPath.moveTo(n3 - n, n4 - n2);
                generalPath.lineTo(n3 + n, n4 - n2);
                generalPath.moveTo(n3 - n, n4 + n2);
                generalPath.lineTo(n3 + n, n4 + n2);
            }
        }
        this.showGrid(generalPath);
    }
    
    void drawCrosses() {
        final GeneralPath generalPath = new GeneralPath();
        float n = (float)(int)Math.round(BladeGrid3_.crossSize * this.tileWidth);
        if (n < 3.0f) {
            n = 3.0f;
        }
        for (int i = 0; i < this.linesV; ++i) {
            for (int j = 0; j < this.linesH; ++j) {
                final float n2 = (float)(this.xstart + i * this.tileWidth);
                final float n3 = (float)(this.ystart + j * this.tileHeight);
                generalPath.moveTo(n2 - n, n3);
                generalPath.lineTo(n2 + n, n3);
                generalPath.moveTo(n2, n3 - n);
                generalPath.lineTo(n2, n3 + n);
            }
        }
        this.showGrid(generalPath);
    }
    
    void showGrid(final Shape shape) {
        if (shape == null) {
            this.imp.setOverlay((Overlay)null);
        }
        else {
            final ShapeRoi shapeRoi = new ShapeRoi(shape);
            ((Roi)shapeRoi).setStrokeColor(this.getColor());
            if (BladeGrid3_.bold && this.linesV * this.linesH < 5000) {
                final ImageCanvas canvas = this.imp.getCanvas();
                final double n = (canvas != null) ? canvas.getMagnification() : 1.0;
                double strokeWidth = 2.0;
                if (n < 1.0) {
                    strokeWidth /= n;
                }
                ((Roi)shapeRoi).setStrokeWidth(strokeWidth);
            }
            this.imp.setOverlay(new Overlay((Roi)shapeRoi));
        }
    }
    
    void drawLines() {
        final GeneralPath generalPath = new GeneralPath();
        this.imp.getWidth();
        this.imp.getHeight();
        final int height = this.imp.getHeight();
        final int width = this.imp.getWidth();
        if (BladeGrid3_.BladeLenth < 1.0) {
            BladeGrid3_.BladeLenth = 1.0;
        }
        if (BladeGrid3_.BladeLenth > 100.0) {
            BladeGrid3_.BladeLenth = 100.0;
        }
        if (BladeGrid3_.areaPerPoint < 5.0) {
            BladeGrid3_.areaPerPoint = 5.0;
        }
        final double n = BladeGrid3_.BladeLenth * 100.0 / BladeGrid3_.areaPerPoint;
        int n2 = 10;
        int n3 = 10;
        if (width == height) {
            n2 = width / (int)n;
            n3 = width / (int)n;
        }
        if (width > height) {
            n2 = width / (int)n;
            n3 = width / (int)n;
        }
        if (width < height) {
            n2 = height / (int)n;
            n3 = height / (int)n;
        }
        if (n2 < 1) {
            n2 = 1;
        }
        if (n3 < 1) {
            n3 = 1;
        }
        int i = 0;
        int j = 0;
        while (i < width) {
            generalPath.moveTo((float)i, 0.0f);
            generalPath.lineTo((float)i, (float)height);
            i += n2;
        }
        while (j < height) {
            generalPath.moveTo(0.0f, (float)j);
            generalPath.lineTo((float)width, (float)j);
            j += n3;
        }
        this.showGrid(generalPath);
    }
    
    void drawHorizontalLines() {
        final GeneralPath generalPath = new GeneralPath();
        final int width = this.imp.getWidth();
        this.imp.getHeight();
        for (int i = 0; i < this.linesH; ++i) {
            final float n = (float)(this.ystart + i * this.tileHeight);
            generalPath.moveTo(0.0f, n);
            generalPath.lineTo((float)width, n);
        }
        this.showGrid(generalPath);
    }
    
    void showDialog() {
        this.imp.getWidth();
        this.imp.getHeight();
        final Calibration calibration = this.imp.getCalibration();
        if (calibration.scaled()) {
            this.pixelWidth = calibration.pixelWidth;
            this.pixelHeight = calibration.pixelHeight;
            this.units = calibration.getUnits();
        }
        else {
            this.pixelWidth = 1.0;
            this.pixelHeight = 1.0;
            this.units = "pixels";
        }
        final NonBlockingGenericDialog nonBlockingGenericDialog = new NonBlockingGenericDialog("Blade Grid");
        ((GenericDialog)nonBlockingGenericDialog).addNumericField("Length of Blade in Meters", BladeGrid3_.BladeLenth, 2, 6, "Max 100");
        ((GenericDialog)nonBlockingGenericDialog).addNumericField("Width of Grid in cm ", BladeGrid3_.areaPerPoint, 2, 6, "Min 5 (under 5 will default to 5");
        ((GenericDialog)nonBlockingGenericDialog).addChoice("Color:", BladeGrid3_.colors, BladeGrid3_.color);
        ((GenericDialog)nonBlockingGenericDialog).addDialogListener((DialogListener)this);
        ((GenericDialog)nonBlockingGenericDialog).showDialog();
        if (((GenericDialog)nonBlockingGenericDialog).wasCanceled()) {
            this.showGrid(null);
        }
    }
    
    public boolean dialogItemChanged(final GenericDialog genericDialog, final AWTEvent awtEvent) {
        this.imp.getWidth();
        this.imp.getHeight();
        BladeGrid3_.BladeLenth = genericDialog.getNextNumber();
        BladeGrid3_.areaPerPoint = genericDialog.getNextNumber();
        BladeGrid3_.color = genericDialog.getNextChoice();
        if (genericDialog.invalidNumber()) {
            return true;
        }
        this.showGrid();
        return true;
    }
    
    private void showGrid() {
        this.drawLines();
    }
    
    Color getColor() {
        Color color = Color.cyan;
        if (BladeGrid3_.color.equals(BladeGrid3_.colors[0])) {
            color = Color.red;
        }
        else if (BladeGrid3_.color.equals(BladeGrid3_.colors[1])) {
            color = Color.green;
        }
        else if (BladeGrid3_.color.equals(BladeGrid3_.colors[2])) {
            color = Color.blue;
        }
        else if (BladeGrid3_.color.equals(BladeGrid3_.colors[3])) {
            color = Color.magenta;
        }
        else if (BladeGrid3_.color.equals(BladeGrid3_.colors[4])) {
            color = Color.cyan;
        }
        else if (BladeGrid3_.color.equals(BladeGrid3_.colors[5])) {
            color = Color.yellow;
        }
        else if (BladeGrid3_.color.equals(BladeGrid3_.colors[6])) {
            color = Color.orange;
        }
        else if (BladeGrid3_.color.equals(BladeGrid3_.colors[7])) {
            color = Color.black;
        }
        else if (BladeGrid3_.color.equals(BladeGrid3_.colors[8])) {
            color = Color.white;
        }
        return color;
    }
    
    static {
        BladeGrid3_.crossSize = 0.1;
        BladeGrid3_.colors = new String[] { "Red", "Green", "Blue", "Magenta", "Cyan", "Yellow", "Orange", "Black", "White" };
        BladeGrid3_.color = "Cyan";
        BladeGrid3_.types = new String[] { "Lines", "Horizontal Lines", "Crosses", "Points", "None" };
        BladeGrid3_.type = BladeGrid3_.types[0];
        BladeGrid3_.areaPerPoint = 30.0;
        BladeGrid3_.BladeLenth = 45.0;
    }
}
