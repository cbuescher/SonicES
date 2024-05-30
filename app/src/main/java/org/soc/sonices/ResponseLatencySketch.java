package org.soc.sonices;

import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.ArrayPercentilesItem;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.ExtendedBounds;
import co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;

import javax.sound.midi.*;
import java.io.IOException;
import java.util.List;

public class ResponseLatencySketch {

    public static SearchResponse<Void> query(ESClient client, long start, long now) throws IOException {
        Query query = BoolQuery.of(
            m -> m.filter(
                TermQuery.of(tq -> tq.field("data_stream.dataset").value("proxy.log"))._toQuery(),
                BoolQuery.of(
                    bq -> bq.should(
                        MatchPhraseQuery.of(mp -> mp.field("cluster_type").query("es"))._toQuery(),
                        TermQuery.of(tq -> tq.field("application_type").value("es"))._toQuery()
                    )
                )._toQuery(),
                RangeQuery.of(rq -> rq.field("@timestamp").format("epoch_millis").gte(JsonData.of(start)).lte(JsonData.of(now)))._toQuery()
            )
        )._toQuery();
        return client.getClient()
            .search(
                b -> b.index("serverless-logging-*:logs-proxy.log-*")
                    .size(0)
                    .query(query)
                    .aggregations(
                        "0",
                        a1 -> a1.dateHistogram(
                            dh -> dh.field("@timestamp")
                                .fixedInterval(Time.of(t -> t.time("1s")))
                                .timeZone("UTC")
                                .extendedBounds(
                                    ExtendedBounds.of(
                                        eb -> eb.min(FieldDateMath.of(fdm -> fdm.expr(String.valueOf(start))))
                                            .max(FieldDateMath.of(fdm -> fdm.expr(String.valueOf(now))))
                                    )
                                )
                        ).aggregations("1", a2 -> a2.percentiles(pc -> pc.field("response_time").percents(50d).keyed(false)))
                    ),
                Void.class
            );

    }

    public static void mapToCC(SearchResponse<Void> response, Sequence sq) throws InvalidMidiDataException {
        Track track = sq.createTrack();
        Scaler s = new Scaler(0, 25, 60, 90);
        List<DateHistogramBucket> bins = response.aggregations().get("0").dateHistogram().buckets().array();
        long offset = bins.get(0).key();
        for (DateHistogramBucket bucket : bins) {
            if (bucket.docCount() > 0) {
                long tick = (bucket.key() - offset);
                ArrayPercentilesItem arrayPercentilesItem = bucket.aggregations().get("1").tdigestPercentiles().values().array().get(0);
                int ccValue = Math.round(s.scale((float) arrayPercentilesItem.value()));
                track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 2, 1, ccValue), tick));
            }
        }
    }
}
