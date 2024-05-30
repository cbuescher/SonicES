package org.soc.sonices;

import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.ExtendedBounds;
import co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;

import javax.sound.midi.*;
import java.io.IOException;
import java.util.List;

public class LogLevelSketches {

    public static SearchResponse<Void> queryForLogLevels(ESClient client, String level, long start, long now) throws IOException {
        Query query = BoolQuery.of(
            m -> m.filter(
                MatchPhraseQuery.of(mp -> mp.field("data_stream.dataset").query("elasticsearch.log"))._toQuery(),
                RangeQuery.of(rq -> rq.field("@timestamp").format("epoch_millis").gte(JsonData.of(start)).lte(JsonData.of(now)))._toQuery()
            ).must(MatchPhraseQuery.of(mp -> mp.field("log.level").query(level))._toQuery())
        )._toQuery();
        return client.getClient()
            .search(
                b -> b.index("serverless-logging-*:logs-elasticsearch*")
                    .size(0)
                    .query(query)
                    .aggregations(
                        "0",
                        a2 -> a2.dateHistogram(
                            dh -> dh.field("@timestamp")
                                .fixedInterval(Time.of(t -> t.time("1s")))
                                .timeZone("UTC")
                                .extendedBounds(
                                    ExtendedBounds.of(
                                        eb -> eb.min(FieldDateMath.of(fdm -> fdm.expr(String.valueOf(start))))
                                            .max(FieldDateMath.of(fdm -> fdm.expr(String.valueOf(now))))
                                    )
                                )
                        )
                    ),
                Void.class
            );

    }

    public static void mapToDensity(SearchResponse<Void> response, Sequence sq, Note note) throws InvalidMidiDataException {
        Track track = sq.createTrack();
        long binSizeTicks = 1000;
        List<DateHistogramBucket> bins = response.aggregations().get("0").dateHistogram().buckets().array();
        long offset = bins.get(0).key();
        for (DateHistogramBucket bucket : bins) {
            if (bucket.docCount() > 0) {
                long events = bucket.docCount();
                long eventDistance = binSizeTicks / events;  // i.e for 4 events 1250
                long eventDuration = Math.min(150, eventDistance * 90 / 100);  // at least 10% shorter than event frequency
                for (int e = 0; e < events; e++) {
                    long tick = (bucket.key() - offset) + (e * eventDistance);
                    track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, note.channel(), note.note(), note.velocity()), tick));
                    track.add(
                        new MidiEvent(
                            new ShortMessage(ShortMessage.NOTE_OFF, note.channel(), note.note(), note.velocity()),
                            tick + eventDuration
                        )
                    );
                }
            }
        }
    }

    public static void mapToVelocity(SearchResponse<Void> response, Sequence sq, Note note) throws InvalidMidiDataException {
        Track track = sq.createTrack();
        long binSizeTicks = 1000;
        List<DateHistogramBucket> bins = response.aggregations().get("0").dateHistogram().buckets().array();
        long offset = bins.get(0).key();
        for (DateHistogramBucket b2 : bins) {
            if (b2.docCount() > 0) {
                long events = b2.docCount();
                long eventDistance = binSizeTicks / events;  // i.e for 4 events 1250
                long eventDuration = Math.min(150, eventDistance * 90 / 100);  // at least 10% shorter than event frequency
                for (int e = 0; e < events; e++) {
                    long tick = (b2.key() - offset) + (e * eventDistance);
                    track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, note.channel(), note.note(), note.velocity()), tick));
                    track.add(
                        new MidiEvent(
                            new ShortMessage(ShortMessage.NOTE_OFF, note.channel(), note.note(), note.velocity()),
                            tick + eventDuration
                        )
                    );
                }
            }
        }
    }
}
