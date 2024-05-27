package org.soc.sonices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MidiHandler {

    private static final Logger LOGGER = LogManager.getLogger(MidiHandler.class);
    private final MidiDevice device;
    private final Receiver receiver;

    private List<ScheduledFuture> notesPending = new ArrayList<>();

    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(4);

    public MidiHandler(String midiInDeviceName) throws InterruptedException {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        // go through devices searching for a MIDI input with the specified name
        MidiDevice deviceCandidate = null;
        for (MidiDevice.Info info : infos) {
            try {
                deviceCandidate = MidiSystem.getMidiDevice(info);
                if (deviceCandidate.getMaxReceivers() == 0 || (deviceCandidate.getDeviceInfo().getName().equals(midiInDeviceName)) == false) {
                    continue;
                }
                break;
            } catch (MidiUnavailableException e) {
                throw new RuntimeException(e);
            }
        }
        this.device = deviceCandidate;
        try {
            this.device.open();
            this.receiver = this.device.getReceiver();
        } catch (MidiUnavailableException e) {
            if (this.device != null) {
                device.close();
            }
            throw new RuntimeException(e);
        }
    }

    public void send(int note, int velocity, long duration) throws InvalidMidiDataException {
        int channel = 0;
        this.receiver.send(new ShortMessage(ShortMessage.NOTE_ON, channel, note, velocity), -1);
        notesPending.add(executor.schedule(
                () -> {
                    try {
                        this.receiver.send(new ShortMessage(ShortMessage.NOTE_OFF, channel, note, velocity), -1);
                    } catch (InvalidMidiDataException e) {
                        LOGGER.warn(e);
                    }
                },
                duration,
                TimeUnit.MILLISECONDS
        ));
    }

    public int messagesPending() {
        int pending = 0;
        Iterator<ScheduledFuture> iterator = notesPending.iterator();
        while (iterator.hasNext()) {
            ScheduledFuture note = iterator.next();
            if (false == note.isDone()) {
                pending++;
            } else {
                iterator.remove();
            }
        }
        return pending;
    }

    public void close() {
        LOGGER.info("finishing");
        if (device != null) {
            device.close();
        }
    }
}