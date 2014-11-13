package org.deeplearning4j.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;


import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapted from:
 * https://github.com/jpatanooga/Metronome/blob/master/src/main/java/tv/floe/metronome/deeplearning/rbm/visualization/RBMRenderer.java
 * @author Adam Gibson
 *
 */
public class FilterRenderer {



    public  JFrame frame;
    BufferedImage img;
    private int width = 28;
    private int height = 28;
    public String title = "TEST";
    private int heightOffset = 0;
    private int widthOffset = 0;
    private static Logger log = LoggerFactory.getLogger(FilterRenderer.class);



    public FilterRenderer() { }

    public void renderHiddenBiases(int heightOffset, int widthOffset, INDArray render_data, String filename) {

        this.width = render_data.columns();
        this.height = render_data.rows();

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.heightOffset = heightOffset;
        this.widthOffset = widthOffset;
        WritableRaster r = img.getRaster();
        int[] equiv = new int[ render_data.length()];

        for (int i = 0; i < equiv.length; i++) {

            //equiv[i] = (int) Math.round( MatrixUtils.getElement(render_data, i) );
            equiv[i] = (int) Math.round( (double) render_data.getScalar(i).element() * 256 );
            log.debug( "> " + equiv[i] );

        }

        log.debug( "hbias size: Cols: " + render_data.columns() + ", Rows: " + render_data.rows()  );

        r.setDataElements(0, 0, width, height, equiv);

        this.saveToDisk(filename);

    }



    public int computeHistogramBucketIndex(double min, double stepSize, double value, int numberBins) {

        for ( int x = 0; x < numberBins; x++ ) {

            double tmp = (x * stepSize) + min;

            if ( value >= tmp && value <= (tmp + stepSize) ) {
                return x;
            }

        }

        return -10;

    }

    /**
     *
     * This is faster but produces rounding errors
     *
     * @param min
     * @param stepSize
     * @param value
     * @param numberBins
     * @return
     */
    public int computeHistogramBucketIndexAlt(double min, double stepSize, double value, int numberBins) {


        //	log.debug("pre round: val: " + value + ", delta on min: " + (value - min) + ", bin-calc: " + ((value - min) / stepSize));
        //	log.debug("pre round: val: " + value + ", bin-calc: " + ((value - min) / stepSize));



        // int bin = (int) ((value - min) / stepSize);

        int bin = (int) (((value - min)) / (stepSize));

		/*
		for ( int x = 0; x < numberBins; x++ ) {

			double tmp = (x * stepSize) + min;

			if ( value <= tmp ) {
				return x;
			}

		}
		 */
        return bin;

    }

    public static double round(double unrounded, int precision, int roundingMode)
    {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(precision, roundingMode);
        return rounded.doubleValue();
    }

    private String buildBucketLabel(int bucketIndex, double stepSize, double min) {

        //log.debug( "label> min " + min + ", stepSize: " + stepSize );

        //log.debug("bucketIndex > " + bucketIndex);

        double val = min + (bucketIndex * stepSize);
        String ret = "" + round(val, 2, BigDecimal.ROUND_HALF_UP);

        //log.debug("label-ret: " + val);

        return ret;

    }

    /**
     * Take some matrix input data and a bucket count and compute:
     *
     * - a list of N buckets, each with:
     * 1. a bucket label
     * 2. a bucket count
     *
     * over the input dataset
     *
     * @param data
     * @param numberBins
     * @return
     */
    public Map<Integer, Integer> generateHistogramBuckets(INDArray data, int numberBins) {

        Map<Integer, Integer> mapHistory = new TreeMap<>();

        double min = (double) data.min(Integer.MAX_VALUE).element();
        double max = (double) data.max(Integer.MAX_VALUE).element();

        double range = max - min;
        double stepSize = range / numberBins;

		/*
		log.debug( "min: " + min );
		log.debug( "max: " + max );
		log.debug( "range: " + range );
		log.debug( "stepSize: " + stepSize );
		log.debug( "numberBins: " + numberBins );
		 */
        //stepSize = 1;

        for ( int row = 0; row < data.rows(); row++ ) {

            for (int col = 0; col < data.columns(); col++ ) {

                double matrix_value = (double) data.getScalar( row, col ).element();

                // at this point we need round values into bins

                int bucket_key = this.computeHistogramBucketIndex(min, stepSize, matrix_value, numberBins);

                int entry = 0;

                if (mapHistory.containsKey( bucket_key )) {

                    // entry exists, increment

                    entry = mapHistory.get( bucket_key );
                    entry++;

                    mapHistory.put( bucket_key, entry );

                } else {

                    // entry does not exit, createComplex, insert

                    // createComplex new key
                    String bucket_label = buildBucketLabel(bucket_key, stepSize, min);

                    // new entry
                    entry = 1; // new Pair<String, Integer>(bucket_label, 1);

                    // update data structure
                    mapHistory.put( bucket_key, entry );
                }

            }
        }


        return mapHistory;


    }





    /**
     * Groups values into 1 of 10 bins, sums, and renders
     *
     * NOTE: this is "render histogram BS code";
     * - I'm not exactly concerned with how pretty it is.
     *
     * @param data
     * @param numberBins
     */
    public void renderHistogram(INDArray data, String filename, int numberBins) {

        Map<Integer, Integer> mapHistory = this.generateHistogramBuckets( data, numberBins );

        double min = (double) data.min(Integer.MAX_VALUE).element(); //data.getFromOrigin(0, 0);
        double max = (double) data.max(Integer.MAX_VALUE).element(); //data.getFromOrigin(0, 0);

        double range = max - min;
        double stepSize = range / numberBins;


        int xOffset = 50;
        int yOffset = -50;

        int graphWidth = 600;
        int graphHeight = 400;

        BufferedImage img = new BufferedImage( graphWidth, graphHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();


        final int BAR_WIDTH = 40;
        final int X_POSITION = 0;
        final int Y_POSITION = 200;
        int MIN_BAR_WIDTH = 4;

        g2d.setColor(Color.LIGHT_GRAY);
        //g2d.drawRect(xOffset, yOffset, graphWidth, graphHeight);
        g2d.fillRect(0, 0, graphWidth, graphHeight);
        //g2d.fill(new Rectangle(x, y, width, height));

        //     int barWidth = Math.max(MIN_BAR_WIDTH,
        //             (int) Math.floor((double) graphWidth
        //             / (double) mapHistory.size()));
        int barWidth = BAR_WIDTH;

        //       log.debug("width = " + graphWidth + "; size = "
        //             + mapHistory.size() + "; barWidth = " + barWidth);

        int maxValue = 0;
        for (Integer key : mapHistory.keySet()) {
            int value = mapHistory.get(key);
            maxValue = Math.max(maxValue, value);
        }

        // draw Y-scale

        //log.debug( "max-value: " + maxValue );

        double plotAreaHeight = (graphHeight + yOffset);

        double yScaleStepSize = plotAreaHeight / 4;

        double yLabelStepSize = (double)maxValue / 4.0f;

        for ( int yStep = 0; yStep < 5; yStep++ ) {

            double curLabel = yStep * yLabelStepSize ;

            long curY = (int) (graphHeight + yOffset) - Math.round(( (int) (curLabel)
                    / (double) maxValue) * (graphHeight + yOffset - 20));

            //log.debug( "curY: " + curY );

            g2d.setColor(Color.BLACK);
            g2d.drawString("" + curLabel, 10, curY );


        }


        int xPos = xOffset;

        for (Integer key : mapHistory.keySet()) {



            long value = mapHistory.get(key);

            String bucket_label = this.buildBucketLabel(key, stepSize, min);

            long barHeight = Math.round(((double) value
                    / (double) maxValue) * (graphHeight + yOffset - 20));

            //g2d.setColor(new Color(key, key, key));
            g2d.setColor(Color.BLUE);

            long yPos = graphHeight + yOffset - barHeight;

            //            Rectangle2D bar = new Rectangle2D.Float(
            //                  xPos, yPos, barWidth, barHeight);

            //g2d.fill(bar);
            g2d.fillRect((int) xPos, (int) yPos, (int) barWidth, (int) barHeight);
            g2d.setColor(Color.DARK_GRAY);
            g2d.drawRect((int) xPos, (int) yPos, (int) barWidth, (int) barHeight);

            g2d.setColor(Color.BLACK);
            g2d.drawString(bucket_label, xPos + ((barWidth / 2) - 10), barHeight + 20 + yPos);
            //g2d.draw(bar);
            xPos += barWidth + 10;
        }



        try {
            saveImageToDisk( img, filename );
        } catch (IOException e) {
            e.printStackTrace();
        }



        g2d.dispose();

    }

    /**
     *
     * Once the probability image and weight histograms are
     * behaving satisfactorily, we plot the learned filter
     * for each hidden neuron, one per column of W. Each filter
     * is of the same dimension as the input data, and it is
     * most useful to visualize the filters in the same way
     * as the input data is visualized.
     * @throws Exception
     *
     */
    public BufferedImage renderFilters( INDArray data, String filename, int patchWidth, int patchHeight,int patchesPerRow) throws Exception {

        int[] equiv = new int[ data.length()  ];



        int numberCols = data.columns();



        double approx = (double) numberCols / (double) patchesPerRow;
        int numPatchRows = (int) Math.round(approx);
        if(numPatchRows < 1)
            numPatchRows = 1;

        int patchBorder = 2;

        int filterImgWidth = ( patchWidth + patchBorder ) * patchesPerRow;
        int filterImgHeight = numPatchRows * (patchHeight + patchBorder);

        log.debug("Filter Width: " + filterImgWidth);
        log.debug("Filter Height: " + filterImgHeight);

        log.debug("Patch array size: " + equiv.length );


        img = new BufferedImage( filterImgWidth, filterImgHeight, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster r = img.getRaster();


        // for each hidden neuron

        // plot the learned filter (same dim as the input data)


        outer:  for ( int col = 0; col < data.columns(); col++ ) {





            int curX = (col % patchesPerRow ) * (patchWidth + patchBorder );
            int curY = col / patchesPerRow * ( patchHeight + patchBorder );

            INDArray column = data.getColumn(col);

            double col_max =  column.min(Integer.MAX_VALUE).getDouble(0);
            double col_min =  column.max(Integer.MAX_VALUE).getDouble(0);

            // now reshape the column into the shape of the filter patch


            // render the filter patch

            log.debug("rendering " + column.length() + " pixels in column " + col + " for filter patch " + patchWidth + " x " + patchHeight + ", total size: " + (patchWidth * patchHeight) + " at " + curX );

            for (int i = 0; i < column.length(); i++) {

                //double patch_normal = ( column.getFromOrigin(i) - min ) / ( max - min + 0.000001 );
                double patch_normal = ( (double) column.getScalar(i).element() - col_min ) / ( col_max - col_min + 0.000001f );
                equiv[i] = (int) (255 * patch_normal);

            }



            // now draw patch to raster image
            boolean outOfBounds = false;
            if(curX >= filterImgWidth) {
                curX = filterImgWidth - 1;
                outOfBounds = true;
                break outer;

            }
            if(curY >= filterImgHeight) {
                curY = filterImgHeight - 1;
                outOfBounds = true;
                break outer;

            }

            r.setPixels( curX, curY, patchWidth, patchHeight, equiv );
            if(outOfBounds)
                break outer;


        }

        try {
            saveImageToDisk( img, filename );
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            if(!ge.isHeadlessInstance()) {
                log.info("Rendering frame...");
                JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

                FilterPanel panel = new FilterPanel(img);
                frame.add(panel);
                Dimension d = new Dimension(numberCols * patchWidth , numPatchRows * patchHeight);
                frame.setSize(d);
                frame.setMinimumSize(d);
                panel.setMinimumSize(d);
                frame.pack();
                frame.setVisible(true);
                Thread.sleep(10000);
                frame.dispose();

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return img;

    }




    public void renderActivations(int heightOffset, int widthOffset, INDArray activation_data, String filename, int scale ) {

        this.width = activation_data.columns();
        this.height = activation_data.rows();


        log.debug( "----- renderActivations ------" );

        img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        this.heightOffset = heightOffset;
        this.widthOffset = widthOffset;
        WritableRaster r = img.getRaster();
        int[] equiv = new int[ activation_data.length() ];

        double max = 0.1f * scale; //MatrixUtils.max(render_data);
        double min = -0.1f * scale; //MatrixUtils.min(render_data);
        double range = max - min;


        for (int i = 0; i < equiv.length; i++) {

            equiv[i] = (int) Math.round(activation_data.getDouble(i) * 255 );

        }


        log.debug( "activations size: Cols: " + activation_data.columns() + ", Rows: " + activation_data.rows()  );

        r.setPixels(0, 0, width, height, equiv);

        this.saveToDisk(filename);

    }





    public static void saveImageToDisk(BufferedImage img, String imageName) throws IOException {

        File outputfile = new File( imageName );
        if(!outputfile.exists())
            outputfile.createNewFile();

        //FileWriter writer = new FileWriter(file);


        ImageIO.write(img, "png", outputfile);

    }

    public void saveToDisk(String filename) {

        try {
            saveImageToDisk( this.img, filename );
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    public void draw() {
        frame = new JFrame(title);
        frame.setVisible(true);
        start();
        frame.add(new JLabel(new ImageIcon(getImage())));

        frame.pack();
        // Better to DISPOSE than EXIT
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public void close() {
        frame.dispose();
    }

    public Image getImage() {
        return img;
    }

    public void start(){


        int[] pixels = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
        boolean running = true;
        while(running){
            BufferStrategy bs = frame.getBufferStrategy();
            if(bs==null){
                frame.createBufferStrategy(4);
                return;
            }
            for (int i = 0; i < width * height; i++)
                pixels[i] = 0;

            Graphics g= bs.getDrawGraphics();
            g.drawImage(img, heightOffset, widthOffset, width, height, null);
            g.dispose();
            bs.show();

        }
    }


}