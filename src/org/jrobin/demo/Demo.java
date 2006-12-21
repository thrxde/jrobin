/* ============================================================
 * JRobin : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.jrobin.org
 * Project Lead:  Sasa Markovic (saxon@jrobin.org);
 *
 * (C) Copyright 2003-2005, by Sasa Markovic.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * Developers:    Sasa Markovic (saxon@jrobin.org)
 *
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package org.jrobin.demo;

import org.jrobin.core.*;
import org.jrobin.graph.RrdGraph;
import org.jrobin.graph.RrdGraphDef;

import java.awt.*;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Simple demo just to check that everything is OK with this library. Creates two files in your
 * $HOME/jrobin-demo directory: demo.rrd and demo.png.
 */
public class Demo {
	static final long SEED = 1909752002L;
	static final Random RANDOM = new Random(SEED);
	static final String FILE = "demo";

	static final long START = Util.getTimestamp(2003, 4, 1);
	static final long END = Util.getTimestamp(2003, 5, 1);
	static final int MAX_STEP = 300;

	static final int IMG_WIDTH = 500;
	static final int IMG_HEIGHT = 300;

	/**
	 * <p>To start the demo, use the following command:</p>
	 * <pre>
	 * java -cp jrobin-{version}.jar org.jrobin.demo.Demo
	 * </pre>
	 *
	 * @param args the name of the backend factory to use (optional)
	 * @throws RrdException Thrown in case of JRobin specific error
	 * @throws IOException  Thrown
	 */
	public static void main(String[] args) throws RrdException, IOException {
		println("== Starting demo");
		long startMillis = System.currentTimeMillis();
		if (args.length > 0) {
			println("Setting default backend factory to " + args[0]);
			RrdDb.setDefaultFactory(args[0]);
		}
		long start = START;
		long end = END;
		String rrdPath = Util.getJRobinDemoPath(FILE + ".rrd");
		String xmlPath = Util.getJRobinDemoPath(FILE + ".xml");
		String rrdRestoredPath = Util.getJRobinDemoPath(FILE + "_restored.rrd");
		String imgPath = Util.getJRobinDemoPath(FILE + ".png");
		String logPath = Util.getJRobinDemoPath(FILE + ".log");
		PrintWriter log = new PrintWriter(new BufferedOutputStream(new FileOutputStream(logPath, false)));
		// creation
		println("== Creating RRD file " + rrdPath);
		RrdDef rrdDef = new RrdDef(rrdPath, start - 1, 300);
		rrdDef.addDatasource("sun", "GAUGE", 600, 0, Double.NaN);
		rrdDef.addDatasource("shade", "GAUGE", 600, 0, Double.NaN);
		rrdDef.addArchive("AVERAGE", 0.5, 1, 600);
		rrdDef.addArchive("AVERAGE", 0.5, 6, 700);
		rrdDef.addArchive("AVERAGE", 0.5, 24, 775);
		rrdDef.addArchive("AVERAGE", 0.5, 288, 797);
		rrdDef.addArchive("MAX", 0.5, 1, 600);
		rrdDef.addArchive("MAX", 0.5, 6, 700);
		rrdDef.addArchive("MAX", 0.5, 24, 775);
		rrdDef.addArchive("MAX", 0.5, 288, 797);
		println(rrdDef.dump());
		log.println(rrdDef.dump());
		println("Estimated file size: " + rrdDef.getEstimatedSize());
		RrdDb rrdDb = new RrdDb(rrdDef);
		println("== RRD file created.");
		if (rrdDb.getRrdDef().equals(rrdDef)) {
			println("Checking RRD file structure... OK");
		}
		else {
			println("Invalid RRD file created. This is a serious bug, bailing out");
			return;
		}
		rrdDb.close();
		println("== RRD file closed.");

		// update database
		GaugeSource sunSource = new GaugeSource(1200, 20);
		GaugeSource shadeSource = new GaugeSource(300, 10);
		println("== Simulating one month of RRD file updates with step not larger than " +
				MAX_STEP + " seconds (* denotes 1000 updates)");
		long t = start;
		int n = 0;
		//rrdDb = new RrdDb(rrdPath);
		//Sample sample = rrdDb.createSample();
		while (t <= end + 86400L) {
			rrdDb = new RrdDb(rrdPath);
			rrdDb.setInfo("T=" + t);
			Sample sample = rrdDb.createSample();
			sample.setTime(t);
			sample.setValue("sun", sunSource.getValue());
			sample.setValue("shade", shadeSource.getValue());
			log.println(sample.dump());
			sample.update();
			t += RANDOM.nextDouble() * MAX_STEP + 1;
			if (((++n) % 1000) == 0) {
				System.out.print("*");
			}
			rrdDb.close();
		}
		println("");
		println("== Finished. RRD file updated " + n + " times");
		//rrdDb.close();

		// test read-only access!
		rrdDb = new RrdDb(rrdPath, true);
		println("File reopen in read-only mode");
		println("== Last update time was: " + rrdDb.getLastUpdateTime());
		println("== Last info was: " + rrdDb.getInfo());

		// fetch data
		println("== Fetching data for the whole month");
		FetchRequest request = rrdDb.createFetchRequest("AVERAGE", start, end);
		println(request.dump());
		log.println(request.dump());
		FetchData fetchData = request.fetchData();
		println("== Data fetched. " + fetchData.getRowCount() + " points obtained");
		println(fetchData.toString());
		println("== Dumping fetched data to XML format");
		println(fetchData.exportXml());
		println("== Fetch completed");

		// dump to XML file
		println("== Dumping RRD file to XML file " + xmlPath + " (can be restored with RRDTool)");
		rrdDb.exportXml(xmlPath);
		println("== Creating RRD file " + rrdRestoredPath + " from XML file " + xmlPath);
		RrdDb rrdRestoredDb = new RrdDb(rrdRestoredPath, xmlPath);

		// close files
		println("== Closing both RRD files");
		rrdDb.close();
		println("== First file closed");
		rrdRestoredDb.close();
		println("== Second file closed");

		// create graph
		println("Creating graph " + Util.getLapTime());
		println("== Creating graph from the second file");
		RrdGraphDef gDef = new RrdGraphDef();
		gDef.setWidth(IMG_WIDTH);
		gDef.setHeight(IMG_HEIGHT);
		gDef.setFilename(imgPath);
		gDef.setStartTime(start);
		gDef.setEndTime(end);
		gDef.setTitle("Temperatures in May 2003");
		gDef.setVerticalLabel("temperature");
		gDef.datasource("sun", rrdRestoredPath, "sun", "AVERAGE");
		gDef.datasource("shade", rrdRestoredPath, "shade", "AVERAGE");
		gDef.datasource("median", "sun,shade,+,2,/");
		gDef.datasource("diff", "sun,shade,-,ABS,-1,*");
		gDef.datasource("sine", "TIME," + start + ",-," + (end - start) +
				",/,2,PI,*,*,SIN,1000,*");
		gDef.line("sun", Color.GREEN, "sun temp");
		gDef.line("shade", Color.BLUE, "shade temp");
		gDef.line("median", Color.MAGENTA, "median value");
		gDef.area("diff", Color.YELLOW, "difference\\r");
		gDef.line("diff", Color.RED, null);
		gDef.line("sine", Color.CYAN, "sine function demo\\r");
		gDef.hrule(2568, Color.GREEN, "hrule");
		gDef.vrule((start + 2 * end) / 3, Color.MAGENTA, "vrule\\r");
		gDef.gprint("sun", "MAX", "maxSun = %.3f%s");
		gDef.gprint("sun", "AVERAGE", "avgSun = %.3f%S\\r");
		gDef.gprint("shade", "MAX", "maxShade = %.3f%S");
		gDef.gprint("shade", "AVERAGE", "avgShade = %.3f%S\\r");
		gDef.print("sun", "MAX", "maxSun = %.3f%s");
		gDef.print("sun", "AVERAGE", "avgSun = %.3f%S\\r");
		gDef.print("shade", "MAX", "maxShade = %.3f%S");
		gDef.print("shade", "AVERAGE", "avgShade = %.3f%S\\r");
		gDef.setImageInfo("<img src='%s' width='%d' height = '%d'>");
		gDef.setPoolUsed(false);
		gDef.setImageFormat("png");
		// create graph finally
		RrdGraph graph = new RrdGraph(gDef);
		println(graph.getRrdGraphInfo().dump());
		println("== Graph created " + Util.getLapTime());
		// locks info
		println("== Locks info ==");
		println(RrdSafeFileBackend.getLockInfo());
		// demo ends
		log.close();
		println("== Demo completed in " +
				((System.currentTimeMillis() - startMillis) / 1000.0) + " sec");
	}

	static void println(String msg) {
		//System.out.println(msg + " " + Util.getLapTime());
		System.out.println(msg);
	}

	static void print(String msg) {
		System.out.print(msg);
	}

	static class GaugeSource {
		private double value;
		private double step;

		GaugeSource(double value, double step) {
			this.value = value;
			this.step = step;
		}

		long getValue() {
			double oldValue = value;
			double increment = RANDOM.nextDouble() * step;
			if (RANDOM.nextDouble() > 0.5) {
				increment *= -1;
			}
			value += increment;
			if (value <= 0) {
				value = 0;
			}
			return Math.round(oldValue);
		}
	}
}




