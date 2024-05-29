package org.soc.sonices;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.ExtendedBounds;
import co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.NamedValue;

import javax.sound.midi.*;
import java.io.IOException;
import java.util.List;

public class LogLevel5mSketch {

    public static SearchResponse<Void> queryForLofLevels(ESClient client) throws IOException {
        Query query = BoolQuery.of(
                        m -> m.filter(MatchPhraseQuery.of(mp -> mp.field("data_stream.dataset").query("elasticsearch.log"))._toQuery(),
                                        RangeQuery.of(rq -> rq.field("@timestamp").format("strict_date_optional_time")
                                                .gte(JsonData.of("2024-05-28T12:38:00.000Z")).lte(JsonData.of("2024-05-28T12:43:00.000Z")))._toQuery())
                                .mustNot(MatchPhraseQuery.of(mp -> mp.field("log.level").query("INFO"))._toQuery(),
                                        MatchPhraseQuery.of(mp -> mp.field("log.level").query("DEBUG"))._toQuery()))
                ._toQuery();
        return client.getClient()
                .search(b -> b.index("serverless-logging-*:logs-elasticsearch*").size(0).query(query).aggregations("0",
                        a -> a.terms(t -> t.field("log.level").order(NamedValue.of("_count", SortOrder.Desc)).size(5).shardSize(25))
                                .aggregations("1", a2 -> a2.dateHistogram(
                                        dh -> dh.field("@timestamp").fixedInterval(Time.of(t -> t.time("5s"))).timeZone("UTC")
                                                .extendedBounds(ExtendedBounds.of(
                                                        eb -> eb.min(FieldDateMath.of(fdm -> fdm.expr("1716899880000")))
                                                                .max(FieldDateMath.of(fdm -> fdm.expr("1716900180000")))))))), Void.class);

    }

    public static Sequence convertToSequence(SearchResponse<Void> response) throws InvalidMidiDataException {
        // setup sequencer so that 1s has 25*40 = 1000 ticks
        Sequence sq = new Sequence(Sequence.SMPTE_25, 40);
        Track track = sq.createTrack();
        long binSizeTicks = 5 * 1000;
        List<StringTermsBucket> buckets = response.aggregations().get("0").sterms().buckets().array();
        for (StringTermsBucket bucket : buckets) {
            System.out.println(bucket.key()._toJsonString() + " | " + bucket.docCount());
            if (bucket.key()._toJsonString().equals("WARN")) {
                List<DateHistogramBucket> bins = bucket.aggregations().get("1").dateHistogram().buckets().array();
                long offset = bins.get(0).key();
                for (DateHistogramBucket b2 : bins) {
                    if (b2.docCount() > 0) {
                        long events = b2.docCount();
                        long eventDistance = binSizeTicks / events;  // i.e for 4 events 1250
                        long eventDuration = Math.min(250, eventDistance * 90 / 100);  // at least 10% shorter than event frequency
                        for (int e = 0; e < events; e++) {
                            long tick = (b2.key() - offset) + (e * eventDistance);
                            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, 55, 93), tick));
                            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, 55, 93), tick + eventDuration));
                        }
                    }
                    System.out.println("    " + b2.key() + " | " + b2.docCount());
                }
            }
        }
        return sq;
    }
}
