/*
 * This source file was generated by the Gradle 'init' task
 */
package org.soc.sonices;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.midi.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.time.Instant.ofEpochMilli;

public class SonicESApp {
    private static Logger LOGGER = LogManager.getLogger(SonicESApp.class);

    private static final String CONFIGFILE = "sonices.conf";

    public static void main(String[] args) throws InvalidMidiDataException, InterruptedException, IOException, MidiUnavailableException {
        MidiHandler mh = new MidiHandler("Bus 1");
        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.open();
        sequencer.getTransmitter().setReceiver(mh.receiver);

        // query part
        Properties configuration = new Properties();
        try {
            configuration.load(new FileInputStream(CONFIGFILE));
        } catch (IOException e) {
            LOGGER.error("Cannot read configuration file" + CONFIGFILE + ", aborting...");
            System.exit(1);
        }
        ESClient client = new ESClient(configuration);

        Sequence sequence = null;
        // sequence = experiment1(client);
        // sequence = experiment2(client);
        sequence = experiment3(client);
        sequencer.setSequence(sequence);

        LOGGER.info("start sequencer");
        sequencer.start();
        Thread.sleep(sequencer.getMicrosecondLength() / 1000 + 1000);
        LOGGER.info("stop sequencer");
        sequencer.close();
        mh.close();
        System.exit(0);
    }

    static Sequence experiment1(ESClient client) throws IOException, InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.SMPTE_25, 40);
        long now = System.currentTimeMillis();
        long start = now - TimeUnit.MINUTES.toMillis(1);
        LOGGER.info("Getting log level data from " + ofEpochMilli(start) + " to " + ofEpochMilli(now));

        LogLevelSketches.mapToDensity(LogLevelSketches.queryForLogLevels(client, "WARN", start, now), sequence, new Note(69, 0, 93));
        LogLevelSketches.mapToDensity(LogLevelSketches.queryForLogLevels(client, "DEBUG", start, now), sequence, new Note(52, 1, 70));
        return sequence;
    }

    /**
     * Get Response Latency Percentiles (50p) in 1sec bins and control some filter with it
     * https://overview.qa.cld.elstc.co/app/r/s/uSCKD
     */
    static Sequence experiment2(ESClient client) throws IOException, InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.SMPTE_25, 40);
        long now = 1717016881327L;
        long start = 1717016817963L;
        LOGGER.info("Getting log level data from " + ofEpochMilli(start) + " to " + ofEpochMilli(now));

        SearchResponse<Void> response = ResponseLatencySketch.query(client, start, now);
        ResponseLatencySketch.mapToCC(response, sequence);
        return sequence;
    }

    /**
     * Get GC Overhead log events, parse them and play sound in GC length
     */
    static Sequence experiment3(ESClient client) throws IOException, InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.SMPTE_25, 40);

        // less dense minute
        long start = Instant.parse("2024-05-31T00:08:00.000Z").toEpochMilli();
        long now = Instant.parse("2024-05-31T00:09:00.000Z").toEpochMilli();

        // // denser minute
        // long start = Instant.parse("2024-05-31T00:11:00.000Z").toEpochMilli();
        // long now = Instant.parse("2024-05-31T00:12:00.000Z").toEpochMilli();

        LOGGER.info("Getting log level data from " + ofEpochMilli(start) + " to " + ofEpochMilli(now));

        SearchResponse<Void> response = GcOverheadSketch.query(client, start, now);
        GcOverheadSketch.parseAndPlay(response, start, sequence);

        return sequence;
    }
}
