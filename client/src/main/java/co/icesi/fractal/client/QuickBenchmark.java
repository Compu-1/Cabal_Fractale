package co.icesi.fractal.client;

import FractalApp.*;
import co.icesi.fractal.core.FractalRegion;
import com.zeroc.Ice.*;
import java.util.UUID;

public class QuickBenchmark {
    public static void main(String[] args) {
        String host = (args.length >= 1) ? args[0] : "localhost";
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : 10000;
        int width = (args.length >= 3) ? Integer.parseInt(args[2]) : 3840;
        int height = (args.length >= 4) ? Integer.parseInt(args[3]) : 2160;
        int maxIter = (args.length >= 5) ? Integer.parseInt(args[4]) : 5000;
        int strategyType = (args.length >= 6) ? Integer.parseInt(args[5]) : 1;
        int strategyParam = (args.length >= 7) ? Integer.parseInt(args[6]) : 64;
        String preset = (args.length >= 8) ? args[7] : "seahorse";

        FractalRegion region = FractalRegion.fromName(preset);
        double xMin = region.getXMin();
        double xMax = region.getXMax();
        double yMin = region.getYMin();
        double yMax = region.getYMax();

        if (width > 0 && height > 0) {
            double aspect = (double) width / (double) height;
            double realHeight = yMax - yMin;
            xMax = xMin + realHeight * aspect;
        }

        InitializationData initData = new InitializationData();
        initData.properties = Util.createProperties(args);
        initData.properties.setProperty("Ice.MessageSizeMax", "204800");

        try (Communicator communicator = Util.initialize(initData)) {
            String masterEndpoint = String.format("MasterService:tcp -h %s -p %d", host, port);
            MasterServicePrx master = MasterServicePrx.checkedCast(communicator.stringToProxy(masterEndpoint));
            if (master == null) {
                System.err.println("[ERROR] Master no disponible");
                return;
            }

            int p = master.getWorkerCount();
            String jobId = "bench-" + UUID.randomUUID().toString().substring(0, 8);
            RenderConfig config = new RenderConfig(
                jobId, width, height, xMin, xMax, yMin, yMax, maxIter, strategyType, strategyParam
            );

            long t0 = System.currentTimeMillis();
            master.renderFractal(config);
            long tDist = System.currentTimeMillis() - t0;

            System.out.printf("RESULT: Workers=%d, Strategy=%d, Param=%d, T_dist=%d ms%n",
                p, strategyType, strategyParam, tDist);
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }
}
