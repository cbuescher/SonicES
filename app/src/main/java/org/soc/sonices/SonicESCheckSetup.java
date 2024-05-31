/*
 * This source file was generated by the Gradle 'init' task
 */
package org.soc.sonices;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import java.io.IOException;
import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static java.time.Instant.ofEpochMilli;

public class SonicESCheckSetup {
    private static Logger LOGGER = LogManager.getLogger(SonicESCheckSetup.class);

    public static void main(String[] args) throws InvalidMidiDataException, InterruptedException, IOException, MidiUnavailableException {
        MidiHandler mh = new MidiHandler("Bus 1");
        Scanner s = new Scanner(System.in);
        while (true) {
            mh.receiver.send(new ShortMessage(ShortMessage.NOTE_ON, 0, 69, 127), -1);
            Thread.sleep(500);
            mh.receiver.send(new ShortMessage(ShortMessage.NOTE_OFF, 0, 69, 127), -1);
            Thread.sleep(500);
        }
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
        long start = Instant.parse("2024-05-30T03:06:02.321Z").toEpochMilli();
        long now = Instant.parse("2024-05-30T03:06:52.516Z").toEpochMilli();
        LOGGER.info("Getting log level data from " + ofEpochMilli(start) + " to " + ofEpochMilli(now));

        SearchResponse<Void> response = GcOverheadSketch.query(client, start, now);
        GcOverheadSketch.parseAndPlay(response, start, sequence);
        return sequence;
    }
}